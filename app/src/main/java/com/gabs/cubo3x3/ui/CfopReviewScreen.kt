package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.quiz.CfopCasePerformance
import com.gabs.cubo3x3.domain.quiz.CfopReview
import com.gabs.cubo3x3.domain.quiz.CfopReviewSchedule
import com.gabs.cubo3x3.domain.quiz.CfopSpacedReview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CfopReviewScreen(
    attempts: List<CfopAttempt>,
    onSaveAttempt: (CfopAttempt) -> Unit,
    onOpenCase: (AlgorithmEntry) -> Unit,
    clock: () -> Long = { System.currentTimeMillis() },
) {
    var categoryName by rememberSaveable { mutableStateOf(AlgorithmCategory.F2L.name) }
    var reviewMode by rememberSaveable { mutableStateOf("DUE") }
    var showUpcoming by rememberSaveable { mutableStateOf(false) }
    var trainingIds by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    var comparing by rememberSaveable { mutableStateOf(false) }
    val latestClock by rememberUpdatedState(clock)
    var currentTime by remember { mutableLongStateOf(clock().coerceAtLeast(0L)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(attempts, active, lifecycleOwner) {
        if (!active) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                currentTime = latestClock().coerceAtLeast(0L)
                delay(60_000L)
            }
        }
    }
    val listState = rememberLazyListState()
    val category = AlgorithmCategory.valueOf(categoryName)
    val schedules = remember(attempts) { CfopSpacedReview.schedule(attempts) }
    val byId = remember(schedules) { schedules.associateBy { it.performance.entry.id } }
    val due = remember(schedules, category, currentTime) {
        CfopSpacedReview.due(schedules, category, currentTime)
    }
    val upcoming = remember(schedules, category, currentTime) {
        CfopSpacedReview.upcoming(schedules, category, currentTime)
    }
    val errors = remember(attempts, category) { CfopReview.queue(attempts, category) }
    val queue = if (reviewMode == "DUE") due.map { it.performance } else errors
    val practicedCount = schedules.count { it.performance.entry.category == category }
    if (comparing) {
        CfopComparisonScreen(attempts, category, onOpenCase, onBack = { comparing = false })
        return
    }
    if (active) {
        val fixed = remember(trainingIds, categoryName) {
            trainingIds.split(",").map { AlgorithmCatalog.entry(category, it.toInt()) }
        }
        CfopRecognitionScreen(
            attempts, onSaveAttempt, onOpenCase,
            fixedRound = fixed,
            onReturnToReview = { active = false },
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("cfop-review"),
        state = listState,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(if (reviewMode == "DUE") "Revisão espaçada CFOP" else "Revisão de casos difíceis",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (reviewMode == "DUE")
                "Erros pedem revisão agora. Acertos em revisões vencidas ampliam o intervalo: 1, 3, 7, 14 e 30 dias. " +
                    "Acertos antecipados não adiam a revisão."
                else "Prioridade pela proporção de erros nas últimas 10 respostas de cada caso. " +
                    "Empates: mais erros, depois o caso respondido há mais tempo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DUE" to "Por prazo", "ERRORS" to "Por erros").forEach { (mode, label) ->
                    FilterChip(selected = reviewMode == mode, onClick = { reviewMode = mode },
                        label = { Text(label) }, modifier = Modifier.weight(1f)
                            .testTag(if (mode == "DUE") "review-mode-due" else "review-mode-errors"))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlgorithmCategory.entries.forEach {
                    FilterChip(selected = category == it, onClick = { categoryName = it.name },
                        label = { Text(it.label) }, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Text("${practicedCount} ${if (practicedCount == 1) "caso praticado" else "casos praticados"} · ${queue.size} para revisar",
                fontWeight = FontWeight.SemiBold)
            Text("Acertos e erros vêm do reconhecimento CFOP, não dos recordes de notação " +
                "ou da velocidade do cronômetro. Favoritos e concluídos não mudam.",
                style = MaterialTheme.typography.bodySmall)
            if (reviewMode == "DUE" && upcoming.isNotEmpty()) {
                Text("${upcoming.size} agendados · próxima revisão: ${reviewDate(upcoming.first().dueAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("review-next-date"))
            }
            TextButton(onClick = { comparing = true }, modifier = Modifier.testTag("review-compare")) {
                Text("Comparar casos confundidos")
            }
        }
        if (queue.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(when {
                        practicedCount == 0 -> "Ainda não há respostas ${category.label}. " +
                            "Comece na aba Reconhecer CFOP para criar seu histórico."
                        reviewMode == "DUE" -> "Nenhuma revisão vencida em ${category.label}. " +
                            "Você pode consultar a agenda ou praticar livremente na aba Reconhecer CFOP."
                        else -> "Nenhum erro recente em ${category.label}. " +
                            "Continue praticando para manter o reconhecimento."
                    }, modifier = Modifier.padding(18.dp))
                }
            }
        } else {
            item {
                Button(onClick = {
                    // Recheck the clock, then freeze IDs. Neither time nor new answers reorder a round.
                    val now = latestClock().coerceAtLeast(0L)
                    currentTime = now
                    val freshQueue = if (reviewMode == "DUE")
                        CfopSpacedReview.due(schedules, category, now).map { it.performance }
                        else errors
                    if (freshQueue.isNotEmpty()) {
                        trainingIds = freshQueue.take(10).joinToString(",") { it.entry.number.toString() }
                        active = true
                    }
                }, modifier = Modifier.fillMaxWidth().testTag("review-start")) {
                    val count = minOf(10, queue.size)
                    Text("Treinar revisão ($count ${if (count == 1) "caso" else "casos"})")
                }
            }
        }
        items(queue, key = { it.entry.id }) { result ->
            ReviewCaseCard(result, if (reviewMode == "DUE") byId[result.entry.id] else null,
                currentTime, onOpenCase, "review-${result.entry.id}")
        }
        if (reviewMode == "DUE" && upcoming.isNotEmpty()) {
            item {
                TextButton(onClick = { showUpcoming = !showUpcoming },
                    modifier = Modifier.fillMaxWidth().testTag("review-show-upcoming")) {
                    Text(if (showUpcoming) "Ocultar próximas revisões" else "Ver próximas revisões")
                }
            }
            if (showUpcoming) {
                items(upcoming, key = { "upcoming-${it.performance.entry.id}" }) { schedule ->
                    ReviewCaseCard(schedule.performance, schedule, currentTime, onOpenCase,
                        "upcoming-${schedule.performance.entry.id}")
                }
            }
        }
    }
}

private fun reviewDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

@Composable
private fun ReviewCaseCard(
    result: CfopCasePerformance,
    schedule: CfopReviewSchedule?,
    nowEpochMillis: Long,
    onOpenCase: (AlgorithmEntry) -> Unit,
    tag: String,
) {
    Card(Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${result.entry.category.label} · Caso ${result.entry.number}",
                style = MaterialTheme.typography.titleMedium)
            if (schedule != null) {
                Text(when {
                    schedule.isDue(nowEpochMillis) && !schedule.lastAnswerCorrect ->
                        "Revisar agora: erro na última resposta."
                    schedule.isDue(nowEpochMillis) -> "Revisão vencida: ${reviewDate(schedule.dueAtEpochMillis)}"
                    else -> "Agendado para: ${reviewDate(schedule.dueAtEpochMillis)}"
                }, fontWeight = FontWeight.SemiBold)
                if (schedule.intervalDays > 0) Text(
                    "Intervalo atual: ${schedule.intervalDays} ${if (schedule.intervalDays == 1) "dia" else "dias"}",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("${result.recentErrors} ${if (result.recentErrors == 1) "erro" else "erros"} " +
                if (result.recentAttempts == 1) "na última resposta"
                else "nas últimas ${result.recentAttempts} respostas")
            Text("${result.correct}/${result.attempts} acertos acumulados · ${result.accuracyPercent}%")
            Text("Última resposta: ${reviewDate(result.lastAnsweredAtEpochMillis)}",
                style = MaterialTheme.typography.bodySmall)
            AlgorithmCaseDiagram(result.entry,
                remember(result.entry.id) { AlgorithmCatalog.initialState(result.entry) },
                null, 0f, false, Modifier.fillMaxWidth().height(150.dp))
            TextButton(onClick = { onOpenCase(result.entry) }) { Text("Estudar ${result.entry.id}") }
        }
    }
}
