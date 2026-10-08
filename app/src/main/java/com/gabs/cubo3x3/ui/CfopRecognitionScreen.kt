package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.data.quiz.QuizRecord
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.quiz.CfopQuestionFactory
import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.QuizScore
import java.util.UUID

@Composable
fun QuizHubScreen(
    records: Map<String, QuizRecord>,
    onSaveResult: (QuizLevel, QuizMode, QuizScore) -> Unit,
    attempts: List<CfopAttempt>,
    onSaveAttempt: (CfopAttempt) -> Unit,
    onOpenCase: (AlgorithmEntry) -> Unit,
    historyRevision: Long = 0,
) {
    key(historyRevision) {
        QuizHubContent(records, onSaveResult, attempts, onSaveAttempt, onOpenCase)
    }
}

@Composable
private fun QuizHubContent(
    records: Map<String, QuizRecord>,
    onSaveResult: (QuizLevel, QuizMode, QuizScore) -> Unit,
    attempts: List<CfopAttempt>,
    onSaveAttempt: (CfopAttempt) -> Unit,
    onOpenCase: (AlgorithmEntry) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf("NOTATION") }
    val stateHolder = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = section == "NOTATION", onClick = { section = "NOTATION" },
                label = { Text("Notação") })
            FilterChip(selected = section == "CFOP", onClick = { section = "CFOP" },
                label = { Text("Reconhecer CFOP") })
            FilterChip(selected = section == "REVIEW", onClick = { section = "REVIEW" },
                label = { Text("Revisão") })
        }
        stateHolder.SaveableStateProvider(section) {
            when (section) {
                "NOTATION" -> QuizScreen(records, onSaveResult)
                "REVIEW" -> CfopReviewScreen(attempts, onSaveAttempt, onOpenCase)
                else -> CfopRecognitionScreen(attempts, onSaveAttempt, onOpenCase)
            }
        }
    }
}

@Composable
fun CfopRecognitionScreen(
    attempts: List<CfopAttempt>,
    onSaveAttempt: (CfopAttempt) -> Unit,
    onOpenCase: (AlgorithmEntry) -> Unit,
    fixedRound: List<AlgorithmEntry>? = null,
    onReturnToReview: () -> Unit = {},
) {
    require(fixedRound == null || (fixedRound.isNotEmpty() && fixedRound.size <= 10 &&
        fixedRound.map { it.id }.distinct().size == fixedRound.size &&
        fixedRound.map { it.category }.distinct().size == 1))
    var categoryName by rememberSaveable {
        mutableStateOf((fixedRound?.first()?.category ?: AlgorithmCategory.F2L).name)
    }
    var phase by rememberSaveable {
        mutableStateOf(if (fixedRound == null) "CONFIGURATION" else "PLAYING")
    }
    var session by rememberSaveable {
        mutableStateOf(if (fixedRound == null) "" else UUID.randomUUID().toString())
    }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedNumber by rememberSaveable { mutableIntStateOf(0) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var answeredAt by rememberSaveable { mutableLongStateOf(0L) }
    var confirmExit by remember { mutableStateOf(false) }
    val category = AlgorithmCategory.valueOf(categoryName)
    val round = remember(categoryName, session) {
        fixedRound ?: CfopQuestionFactory.round(category, session.hashCode())
    }
    val target = round[questionIndex]
    val question = remember(target.id, session) {
        CfopQuestionFactory.create(target, session.hashCode() xor target.number)
    }
    fun start() {
        session = UUID.randomUUID().toString()
        questionIndex = 0
        selectedNumber = 0
        correctCount = 0
        phase = "PLAYING"
    }
    fun attempt() = CfopAttempt("$session-$questionIndex", category.name,
        target.number, selectedNumber, answeredAt)
    // Replay after restoration is safe: the database ignores a repeated answer ID.
    LaunchedEffect(session, questionIndex, selectedNumber) {
        if (session.isNotEmpty() && selectedNumber != 0) onSaveAttempt(attempt())
    }
    BackHandler(enabled = phase != "CONFIGURATION") {
        if (phase == "PLAYING") confirmExit = true
        else if (fixedRound != null) onReturnToReview() else phase = "CONFIGURATION"
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("cfop-recognition"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (phase) {
            "CONFIGURATION" -> {
                item {
                    Text("Reconhecimento de casos CFOP",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Observe o padrão e escolha a fórmula que o resolve na orientação mostrada. " +
                        "Rodadas de 10 casos, sem limite de tempo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlgorithmCategory.entries.forEach {
                            FilterChip(selected = category == it,
                                onClick = { categoryName = it.name },
                                label = { Text(it.label) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    val history = attempts.filter { it.category == category.name }
                    Text(if (history.isEmpty()) "Você ainda não respondeu casos ${category.label}."
                        else "${history.count { it.isCorrect }} acertos em ${history.size} respostas ${category.label}.")
                }
                item {
                    Button(onClick = ::start, modifier = Modifier.fillMaxWidth()) {
                        Text("Começar reconhecimento")
                    }
                }
            }
            "RESULT" -> {
                item { Text("Rodada concluída", style = MaterialTheme.typography.headlineSmall) }
                item { Text("$correctCount de ${round.size} acertos · ${category.label}") }
                item { Text("As respostas ficam salvas por caso, inclusive os erros. " +
                    "Não mudam seus favoritos nem marcam casos como concluídos.") }
                item {
                    Button(onClick = { if (fixedRound != null) onReturnToReview() else start() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (fixedRound != null) "Voltar à revisão" else "Nova rodada")
                    }
                }
                if (fixedRound == null) {
                    item {
                        OutlinedButton(onClick = { phase = "CONFIGURATION" },
                            modifier = Modifier.fillMaxWidth()) { Text("Escolher etapa") }
                    }
                }
            }
            else -> {
                item {
                    Text("${category.label} · Pergunta ${questionIndex + 1} de ${round.size} · $correctCount acertos",
                        fontWeight = FontWeight.SemiBold)
                    Text("Amarelo acima · branco abaixo · vermelho à frente · verde à direita",
                        style = MaterialTheme.typography.bodySmall)
                }
                item {
                    val state = remember(target.id) { AlgorithmCatalog.initialState(target) }
                    Card(Modifier.fillMaxWidth()) {
                        AlgorithmCaseDiagram(target, state, null, 0f, false,
                            Modifier.fillMaxWidth().height(220.dp).padding(12.dp))
                    }
                }
                item { Text("Qual fórmula resolve este padrão?",
                    style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(question.options, key = { _, entry -> entry.id }) { index, option ->
                    OutlinedButton(
                        onClick = {
                            if (selectedNumber == 0) {
                                selectedNumber = option.number
                                answeredAt = System.currentTimeMillis()
                                if (option.id == target.id) correctCount += 1
                                onSaveAttempt(attempt())
                            }
                        },
                        enabled = selectedNumber == 0,
                        modifier = Modifier.fillMaxWidth().testTag("cfop-option-$index"),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("${('A'.code + index).toChar()}", fontWeight = FontWeight.Bold)
                            Text(option.notation, fontFamily = FontFamily.Monospace)
                            if (selectedNumber != 0) {
                                if (option.id == target.id) Text("Resposta correta")
                                else if (option.number == selectedNumber) Text("Sua resposta")
                            }
                        }
                    }
                }
                if (selectedNumber != 0) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (selectedNumber == target.number) "Acertou!" else "Vamos revisar este caso.",
                                    fontWeight = FontWeight.Bold)
                                Text("Caso ${category.label} ${target.number}")
                                TextButton(onClick = { onOpenCase(target) }) { Text("Estudar este caso") }
                            }
                        }
                    }
                    item {
                        Button(onClick = {
                            if (questionIndex == round.lastIndex) phase = "RESULT"
                            else { questionIndex += 1; selectedNumber = 0 }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (questionIndex == round.lastIndex) "Ver resultado" else "Próximo padrão")
                        }
                    }
                }
                item {
                    TextButton(onClick = { confirmExit = true }) { Text("Encerrar rodada") }
                }
            }
        }
    }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("Encerrar a rodada?") },
        text = { Text("As respostas já registradas serão mantidas no seu histórico CFOP.") },
        confirmButton = {
            TextButton(onClick = {
                confirmExit = false
                if (fixedRound != null) onReturnToReview() else phase = "CONFIGURATION"
            }) { Text("Encerrar") }
        },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Continuar") } },
    )
}
