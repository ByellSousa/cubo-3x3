package com.gabs.cubo3x3.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.quiz.QuizRecord
import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.QuizQuestionFactory
import com.gabs.cubo3x3.domain.quiz.QuizScore
import com.gabs.cubo3x3.domain.quiz.QuizTimer
import kotlinx.coroutines.delay

private enum class QuizPhase { CONFIGURATION, PLAYING, RESULT }

@Composable
fun QuizScreen(
    records: Map<String, QuizRecord>,
    onSaveResult: (QuizLevel, QuizMode, QuizScore) -> Unit,
) {
    var levelName by rememberSaveable { mutableStateOf(QuizLevel.ESSENTIAL.name) }
    var modeName by rememberSaveable { mutableStateOf(QuizMode.PRACTICE.name) }
    var phaseName by rememberSaveable { mutableStateOf(QuizPhase.CONFIGURATION.name) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedToken by rememberSaveable { mutableStateOf<String?>(null) }
    var answered by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var currentStreak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by rememberSaveable { mutableIntStateOf(0) }
    var points by rememberSaveable { mutableIntStateOf(0) }
    var startedAtRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var remainingMillis by rememberSaveable { mutableLongStateOf(60_000L) }
    var previousBestScore by rememberSaveable { mutableIntStateOf(0) }
    var isNewRecord by rememberSaveable { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }

    val level = QuizLevel.valueOf(levelName)
    val mode = QuizMode.valueOf(modeName)
    val phase = QuizPhase.valueOf(phaseName)
    val score = QuizScore(
        answered = answered,
        correct = correct,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        points = points,
    )
    val timer = remember { QuizTimer(SystemClock::elapsedRealtime) }

    fun applyScore(updated: QuizScore) {
        answered = updated.answered
        correct = updated.correct
        currentStreak = updated.currentStreak
        bestStreak = updated.bestStreak
        points = updated.points
    }

    fun resetScore() {
        applyScore(QuizScore())
        questionIndex = 0
        selectedToken = null
        remainingMillis = 60_000L
        isNewRecord = false
    }

    fun startGame() {
        resetScore()
        previousBestScore = records[QuizRecord.key(level, mode)]?.bestScore ?: 0
        startedAtRealtime = timer.start()
        phaseName = QuizPhase.PLAYING.name
    }

    fun finishGame() {
        val finalScore = QuizScore(
            answered = answered,
            correct = correct,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            points = points,
        )
        isNewRecord = finalScore.points > previousBestScore
        onSaveResult(level, mode, finalScore)
        phaseName = QuizPhase.RESULT.name
    }

    BackHandler(enabled = phase != QuizPhase.CONFIGURATION) {
        if (phase == QuizPhase.PLAYING) {
            confirmAbandon = true
        } else {
            phaseName = QuizPhase.CONFIGURATION.name
        }
    }

    LaunchedEffect(phaseName, modeName, startedAtRealtime) {
        if (phase != QuizPhase.PLAYING || mode != QuizMode.TIMED) return@LaunchedEffect
        while (true) {
            remainingMillis = timer.remainingMillis(startedAtRealtime)
            if (remainingMillis == 0L) {
                finishGame()
                break
            }
            delay(50L)
        }
    }

    when (phase) {
        QuizPhase.CONFIGURATION -> QuizConfiguration(
            level = level,
            mode = mode,
            records = records,
            onLevelChange = { levelName = it.name },
            onModeChange = { modeName = it.name },
            onStart = ::startGame,
        )
        QuizPhase.PLAYING -> QuizGame(
            level = level,
            mode = mode,
            questionIndex = questionIndex,
            selectedToken = selectedToken,
            score = score,
            remainingMillis = remainingMillis,
            onAnswer = { token ->
                if (selectedToken == null) {
                    selectedToken = token
                    applyScore(score.answer(token == QuizQuestionFactory
                        .create(level, questionIndex).correctToken))
                }
            },
            onNext = {
                questionIndex += 1
                selectedToken = null
            },
            onFinish = { confirmFinish = true },
        )
        QuizPhase.RESULT -> QuizResult(
            level = level,
            mode = mode,
            score = score,
            previousBestScore = previousBestScore,
            isNewRecord = isNewRecord,
            onPlayAgain = ::startGame,
            onBack = { phaseName = QuizPhase.CONFIGURATION.name },
        )
    }

    if (confirmAbandon) {
        AlertDialog(
            onDismissRequest = { confirmAbandon = false },
            title = { Text("Abandonar a partida?") },
            text = { Text("O resultado atual não será salvo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAbandon = false
                    phaseName = QuizPhase.CONFIGURATION.name
                    resetScore()
                }) { Text("Abandonar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAbandon = false }) { Text("Continuar") }
            },
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finalizar a prática?") },
            text = { Text("O resultado atual será salvo nos recordes locais.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    finishGame()
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text("Continuar") }
            },
        )
    }
}

@Composable
private fun QuizConfiguration(
    level: QuizLevel,
    mode: QuizMode,
    records: Map<String, QuizRecord>,
    onLevelChange: (QuizLevel) -> Unit,
    onModeChange: (QuizMode) -> Unit,
    onStart: () -> Unit,
) {
    val record = records[QuizRecord.key(level, mode)]
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            QuizInfoCard(
                title = "Quiz visual de notação",
                body = "Observe o movimento que o cubo repete e escolha a notação correspondente.",
            )
        }
        item { QuizSection("Nível", "A quantidade de movimentos aumenta a cada nível") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizLevel.entries.forEach { option ->
                    FilterChip(
                        selected = level == option,
                        onClick = { onLevelChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item { QuizSection("Modo", "Pratique livremente ou marque pontos em 60 segundos") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuizMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Melhor pontuação", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "${record?.bestScore ?: 0} pontos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (record == null) {
                            "Nenhuma partida concluída neste nível e modo."
                        } else {
                            "${record.gamesPlayed} partidas · ${record.bestAccuracyPercent}% de melhor precisão"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Começar")
            }
        }
    }
}

@Composable
private fun QuizGame(
    level: QuizLevel,
    mode: QuizMode,
    questionIndex: Int,
    selectedToken: String?,
    score: QuizScore,
    remainingMillis: Long,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val question = remember(level, questionIndex) {
        QuizQuestionFactory.create(level, questionIndex)
    }
    val move = remember(question.correctToken) {
        MoveNotation.parseToken(question.correctToken)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${score.points} pontos", fontWeight = FontWeight.Bold)
                Text("Sequência ${score.currentStreak}")
                if (mode == QuizMode.TIMED) {
                    Text(
                        text = "${(remainingMillis + 999L) / 1_000L}s",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                QuizMovePreview(
                    move = move,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(245.dp)
                        .padding(10.dp),
                )
            }
        }
        item {
            Text(
                "Qual notação representa este movimento?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        items(question.options, key = { it }) { option ->
            val answered = selectedToken != null
            val isCorrectOption = option == question.correctToken
            val isSelected = option == selectedToken
            val containerColor = when {
                answered && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                answered && isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            OutlinedButton(
                onClick = { onAnswer(option) },
                enabled = !answered,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = containerColor,
                    disabledContainerColor = containerColor,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(option, fontFamily = FontFamily.Monospace)
            }
        }
        if (selectedToken != null) {
            item {
                Text(
                    text = if (selectedToken == question.correctToken) {
                        "Correto!"
                    } else {
                        "A resposta correta é ${question.correctToken}."
                    },
                    color = if (selectedToken == question.correctToken) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text("Próxima")
                }
            }
        }
        if (mode == QuizMode.PRACTICE && score.answered > 0) {
            item {
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar prática")
                }
            }
        }
    }
}

@Composable
private fun QuizResult(
    level: QuizLevel,
    mode: QuizMode,
    score: QuizScore,
    previousBestScore: Int,
    isNewRecord: Boolean,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            QuizInfoCard(
                title = if (isNewRecord) "Novo recorde!" else "Partida concluída",
                body = "${level.label} · ${mode.label}",
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${score.points}", style = MaterialTheme.typography.displaySmall)
                    Text("pontos", style = MaterialTheme.typography.labelLarge)
                    QuizMetric("Acertos", "${score.correct} de ${score.answered}")
                    QuizMetric("Precisão", "${score.accuracyPercent}%")
                    QuizMetric("Melhor sequência", score.bestStreak.toString())
                    if (!isNewRecord && previousBestScore > 0) {
                        Text(
                            "Recorde anterior: $previousBestScore pontos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Jogar novamente")
            }
        }
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar à configuração")
            }
        }
    }
}

@Composable
private fun QuizMetric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuizSection(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuizInfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
