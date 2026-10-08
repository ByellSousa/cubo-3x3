package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.quiz.CfopComparisonKey
import com.gabs.cubo3x3.domain.quiz.CfopConfusionPair
import com.gabs.cubo3x3.domain.quiz.CfopConfusions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CfopComparisonScreen(
    attempts: List<CfopAttempt>,
    initialCategory: AlgorithmCategory,
    onOpenCase: (AlgorithmEntry) -> Unit,
    onBack: () -> Unit,
) {
    var categoryName by rememberSaveable { mutableStateOf(initialCategory.name) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    val category = AlgorithmCategory.valueOf(categoryName)
    val selected = remember(selectedId) { CfopComparisonKey.fromId(selectedId) }
    val listState = rememberLazyListState()
    val pairs by produceState<List<CfopConfusionPair>?>(null, attempts) {
        value = null
        value = withContext(Dispatchers.Default) { CfopConfusions.pairs(attempts) }
    }
    val currentPairs = pairs
    val visible = currentPairs.orEmpty().filter { it.key.category == category }
    if (selected != null) {
        BackHandler { selectedId = "" }
        ComparisonDetail(selected, currentPairs?.firstOrNull { it.key == selected }, currentPairs != null,
            onOpenCase, onBack = { selectedId = "" })
        return
    }
    BackHandler(onBack = onBack)
    if (currentPairs == null) {
        // A short placeholder LazyColumn would clamp the restored index before the rows arrive.
        // Keep the remembered list state detached until the complete history is available.
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("comparison-back-review")) {
                Text("Voltar à revisão")
            }
            Text("Casos confundidos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Calculando pares…", Modifier.testTag("comparison-loading"))
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().testTag("cfop-comparison-list"),
        state = listState, contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.testTag("comparison-back-review")) {
                Text("Voltar à revisão")
            }
            Text("Casos confundidos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Trocas nas últimas 10 respostas de cada caso. " +
                "Contamos as duas direções do par; acertos recentes podem retirar erros antigos da janela.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlgorithmCategory.entries.forEach {
                    FilterChip(selected = category == it, onClick = { categoryName = it.name },
                        label = { Text(it.label) }, modifier = Modifier.weight(1f)
                            .testTag("comparison-filter-${it.name}"))
                }
            }
        }
        item {
            Text("${visible.size} ${if (visible.size == 1) "par" else "pares"} em ${category.label}",
                Modifier.testTag("comparison-count"), fontWeight = FontWeight.SemiBold)
            Text("Comparar e consultar não contam como uma resposta ou um tempo de treino.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (visible.isEmpty()) item {
            Card(Modifier.fillMaxWidth()) {
                Text("Nenhuma troca recente em ${category.label}. " +
                    "O reconhecimento CFOP cria este histórico; não sugerimos pares sem respostas erradas.",
                    Modifier.padding(16.dp).testTag("comparison-empty"))
            }
        }
        items(visible, key = { it.key.id }) { pair ->
            Card(Modifier.fillMaxWidth().testTag("confusion-${pair.key.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${category.label} · Casos ${pair.key.firstCaseNumber} e ${pair.key.secondCaseNumber}",
                        style = MaterialTheme.typography.titleMedium)
                    ConfusionCounts(pair)
                    TextButton(onClick = { selectedId = pair.key.id },
                        modifier = Modifier.testTag("compare-${pair.key.id}")) {
                        Text("Comparar ${pair.key.first.id} e ${pair.key.second.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfusionCounts(pair: CfopConfusionPair) {
    Text("${pair.total} ${if (pair.total == 1) "troca recente" else "trocas recentes"}",
        fontWeight = FontWeight.SemiBold)
    Text("Vi o caso ${pair.key.firstCaseNumber} e escolhi o ${pair.key.secondCaseNumber}: ${pair.firstChosenAsSecond}")
    Text("Vi o caso ${pair.key.secondCaseNumber} e escolhi o ${pair.key.firstCaseNumber}: ${pair.secondChosenAsFirst}")
    Text("Última troca: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .format(Instant.ofEpochMilli(pair.lastConfusedAtEpochMillis).atZone(ZoneId.systemDefault())),
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ComparisonDetail(
    key: CfopComparisonKey,
    pair: CfopConfusionPair?,
    historyReady: Boolean,
    onOpenCase: (AlgorithmEntry) -> Unit,
    onBack: () -> Unit,
) {
    var showFormulas by rememberSaveable(key.id) { mutableStateOf(false) }
    var enlarged by rememberSaveable(key.id) { mutableStateOf(false) }
    val cues = remember(key) { comparisonCues(key) }
    val changed = cues.filter { it.differs }
    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize().testTag("cfop-comparison-detail"), state = listState,
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.testTag("comparison-back-list")) { Text("Voltar aos pares") }
            Text("${key.category.label} · Casos ${key.firstCaseNumber} e ${key.secondCaseNumber}",
                Modifier.testTag("comparison-title"), style = MaterialTheme.typography.titleLarge)
            Text("Amarelo acima, branco abaixo, vermelho à frente e verde à direita. " +
                "Mesma orientação do catálogo; não giramos os padrões para forçar uma semelhança.",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            when {
                !historyReady -> Text("Atualizando histórico…")
                pair != null -> ConfusionCounts(pair)
                else -> Text("Este par não tem mais trocas na janela recente. " +
                    "Os dois casos continuam abertos para estudo.", Modifier.testTag("comparison-retired"))
            }
        }
        item {
            TextButton(onClick = { enlarged = !enlarged }, Modifier.testTag("comparison-enlarge")) {
                Text(if (enlarged) "Comparar lado a lado" else "Ampliar diagramas")
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sideBySide = !enlarged && maxWidth >= 340.dp && LocalDensity.current.fontScale <= 1.3f
                if (sideBySide) Row(Modifier.testTag("comparison-layout-side"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComparisonCaseCard(key.first, showFormulas, onOpenCase, Modifier.weight(1f))
                    ComparisonCaseCard(key.second, showFormulas, onOpenCase, Modifier.weight(1f))
                } else Column(Modifier.testTag("comparison-layout-stacked"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComparisonCaseCard(key.first, showFormulas, onOpenCase, Modifier.fillMaxWidth())
                    ComparisonCaseCard(key.second, showFormulas, onOpenCase, Modifier.fillMaxWidth())
                }
            }
            TextButton(onClick = { showFormulas = !showFormulas }, Modifier.testTag("comparison-formulas-toggle")) {
                Text(if (showFormulas) "Ocultar fórmulas originais" else "Mostrar fórmulas originais")
            }
        }
        item {
            Text("O que observar", style = MaterialTheme.typography.titleMedium)
            if (key.category != AlgorithmCategory.F2L) Text(
                "L = linha, C = coluna. Linhas 1→3: atrás→frente. Colunas 1→3: esquerda→direita. " +
                    if (key.category == AlgorithmCategory.PLL) "Cada destino corresponde à seta; fixo = peça sem troca."
                    else "As laterais indicam onde há um adesivo amarelo.",
                style = MaterialTheme.typography.bodySmall)
            if (changed.isEmpty()) Text("Estas pistas são iguais; compare os diagramas na orientação indicada.")
        }
        items(changed, key = { it.label }) { cue ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cue.label, fontWeight = FontWeight.SemiBold)
                    Text("${key.first.id}: ${cue.first}")
                    Text("${key.second.id}: ${cue.second}")
                }
            }
        }
    }
}

@Composable
private fun ComparisonCaseCard(
    entry: AlgorithmEntry,
    showFormula: Boolean,
    onOpenCase: (AlgorithmEntry) -> Unit,
    modifier: Modifier,
) {
    Card(modifier.testTag("comparison-case-${entry.id}")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.id, style = MaterialTheme.typography.titleMedium)
            AlgorithmCaseDiagram(entry, remember(entry.id) { AlgorithmCatalog.initialState(entry) },
                null, 0f, false, Modifier.fillMaxWidth().height(190.dp).testTag("comparison-diagram-${entry.id}"))
            if (showFormula) {
                Text("Fórmula original", style = MaterialTheme.typography.labelMedium)
                Text(entry.notation, Modifier.testTag("comparison-formula-${entry.id}"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
            TextButton(onClick = { onOpenCase(entry) }, Modifier.testTag("comparison-study-${entry.id}")) {
                Text("Estudar ${entry.id}")
            }
        }
    }
}
