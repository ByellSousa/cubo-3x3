package com.gabs.cubo3x3.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeNet
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.toComposeColor
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.data.timer.DEFAULT_TIMER_SESSION_ID
import com.gabs.cubo3x3.data.timer.TimerSession
import com.gabs.cubo3x3.data.timer.TimerSessionRepository
import com.gabs.cubo3x3.data.timer.effectiveDurationMillis
import com.gabs.cubo3x3.domain.timer.AverageResult
import com.gabs.cubo3x3.domain.timer.AnalysisPeriod
import com.gabs.cubo3x3.domain.timer.AnalysisPoint
import com.gabs.cubo3x3.domain.timer.DistributionBucket
import com.gabs.cubo3x3.domain.timer.InspectionPenalty
import com.gabs.cubo3x3.domain.timer.InspectionRules
import com.gabs.cubo3x3.domain.timer.SolveStatisticsCalculator
import com.gabs.cubo3x3.domain.timer.Stopwatch
import com.gabs.cubo3x3.domain.timer.StopwatchPhase
import com.gabs.cubo3x3.domain.timer.StopwatchSession
import com.gabs.cubo3x3.domain.timer.TimerAnalysisCalculator
import com.gabs.cubo3x3.domain.timer.TnoodleScrambleGenerator
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlanCodec
import com.gabs.cubo3x3.domain.timer.CfopTrainingRound
import com.gabs.cubo3x3.domain.timer.CfopExecutionAnalysis
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun TimerScreen(
    solveTimes: List<SolveTime>,
    sessions: List<TimerSession>,
    selectedSessionId: Long,
    inspectionEnabled: Boolean,
    inspectionSoundsEnabled: Boolean,
    onInspectionEnabledChange: (Boolean) -> Unit,
    onInspectionSoundsEnabledChange: (Boolean) -> Unit,
    onSaveSolveTime: (Long, String, SolvePenalty, String?, Int?) -> Unit,
    onUpdateSolveTime: (Long, String, SolvePenalty) -> Unit,
    onDeleteSolveTime: (Long) -> Unit,
    onClearSolveTimes: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onCreateSession: (String) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onActiveChange: (Boolean) -> Unit = {},
    algorithmProgress: Map<String, AlgorithmProgress> = emptyMap(),
    initialTrainingPlan: CfopTrainingPlan = CfopTrainingPlan(),
    onTrainingPlanChange: (CfopTrainingPlan) -> Unit = {},
) {
    var timerStateName by rememberSaveable { mutableStateOf(StopwatchPhase.IDLE.name) }
    var startedAtRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var displayedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var inspectionStartedAtRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var inspectionElapsedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var eightSecondAlertPlayed by rememberSaveable { mutableStateOf(false) }
    var twelveSecondAlertPlayed by rememberSaveable { mutableStateOf(false) }
    var automaticPenaltyName by rememberSaveable { mutableStateOf(SolvePenalty.NONE.name) }
    var scrambleHistory by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var scrambleTrainingHistory by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var currentScrambleIndex by rememberSaveable { mutableIntStateOf(-1) }
    var scrambleRequest by rememberSaveable { mutableIntStateOf(0) }
    var scrambleLoading by remember { mutableStateOf(true) }
    var scrambleError by remember { mutableStateOf<String?>(null) }
    var showScrambleNet by rememberSaveable { mutableStateOf(false) }
    var selectedSolveId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var showAnalysis by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var confirmAbort by remember { mutableStateOf(false) }
    var showTrainingSettings by rememberSaveable { mutableStateOf(false) }
    var storedTrainingPlan by rememberSaveable {
        mutableStateOf(CfopTrainingPlanCodec.encode(initialTrainingPlan))
    }
    val trainingPlan = CfopTrainingPlanCodec.decode(storedTrainingPlan)
    var roundCaseIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var roundPracticedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var roundInitialized by rememberSaveable { mutableStateOf(false) }
    var lastHandledScrambleRequest by rememberSaveable { mutableIntStateOf(-1) }
    val trainingRound = CfopTrainingRound(roundCaseIds, roundPracticedIds.toSet())
    val completedTraining = trainingPlan.mode == CfopTrainingMode.COMPLETED
    var showSessionManager by rememberSaveable { mutableStateOf(false) }
    var creatingSession by rememberSaveable { mutableStateOf(false) }
    var editingSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val timerState = StopwatchPhase.valueOf(timerStateName)
    val stopwatch = remember { Stopwatch(SystemClock::elapsedRealtime) }
    val scrambleGenerator = remember { TnoodleScrambleGenerator() }
    val statistics = remember(solveTimes) { SolveStatisticsCalculator.calculate(solveTimes) }
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()
    }
    val isInspecting = inspectionStartedAtRealtime > 0L
    val active = timerState == StopwatchPhase.RUNNING || isInspecting
    val view = LocalView.current
    LaunchedEffect(active) { onActiveChange(active) }
    DisposableEffect(Unit) { onDispose { onActiveChange(false) } }
    DisposableEffect(view, active) {
        val previous = view.keepScreenOn
        if (active) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    BackHandler(enabled = active) { confirmAbort = true }
    val inspectionPenalty = InspectionRules.penaltyAt(inspectionElapsedMillis)
    val currentScramble = scrambleHistory.getOrNull(currentScrambleIndex).orEmpty()
    val currentTrainingId = scrambleTrainingHistory.getOrNull(currentScrambleIndex).orEmpty()
    val currentTrainingEntry = currentTrainingId
        .takeIf(String::isNotBlank)
        ?.let { id -> AlgorithmCatalog.allEntries().firstOrNull { it.id == id } }
    val displayedScramble = scrambleError ?: currentScramble
    val trainingLabel = when (trainingPlan.mode) {
        CfopTrainingMode.FREE -> "Treino livre"
        CfopTrainingMode.CATEGORY -> "${trainingPlan.category?.label} · Todos os casos"
        CfopTrainingMode.CASE -> "${trainingPlan.category?.label} · Caso ${trainingPlan.caseNumber}"
        CfopTrainingMode.COMPLETED ->
            "Concluídos · ${roundPracticedIds.size}/${roundCaseIds.size} praticados"
    }
    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }
        ?: sessions.firstOrNull()
    val defaultSessionName = sessions.firstOrNull { it.id == DEFAULT_TIMER_SESSION_ID }?.name
        ?: "Principal"
    val timerSession = StopwatchSession(
        phase = timerState,
        startedAtRealtimeMillis = startedAtRealtime,
    )

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }

    fun beginTraining(plan: CfopTrainingPlan) {
        storedTrainingPlan = CfopTrainingPlanCodec.encode(plan)
        roundCaseIds = ArrayList(
            if (plan.mode == CfopTrainingMode.COMPLETED) {
                CfopTrainingRound.start(plan, algorithmProgress).caseIds
            } else emptyList(),
        )
        roundPracticedIds = arrayListOf()
        roundInitialized = true
        scrambleHistory = arrayListOf()
        scrambleTrainingHistory = arrayListOf()
        currentScrambleIndex = -1
        scrambleError = null
        scrambleLoading = true
        scrambleRequest += 1
    }

    LaunchedEffect(scrambleRequest) {
        if (timerState == StopwatchPhase.RUNNING) return@LaunchedEffect
        // Opening options, switching tabs and recreation must not silently skip a case.
        if (lastHandledScrambleRequest == scrambleRequest && currentScramble.isNotBlank()) {
            scrambleLoading = false
            return@LaunchedEffect
        }
        scrambleLoading = true
        if (completedTraining && !roundInitialized) {
            roundCaseIds = ArrayList(CfopTrainingRound.start(trainingPlan, algorithmProgress).caseIds)
            roundInitialized = true
        }
        val activeRound = CfopTrainingRound(roundCaseIds, roundPracticedIds.toSet())
        if (completedTraining && activeRound.nextCase() == null) {
            scrambleLoading = false
            lastHandledScrambleRequest = scrambleRequest
            scrambleError = if (activeRound.isComplete) null
                else "Nenhum caso no treino. Selecione concluídos ou acrescente casos novos em Opções."
            return@LaunchedEffect
        }
        runCatching {
            withContext(Dispatchers.Default) {
                val category = trainingPlan.category
                if (completedTraining) {
                    val id = requireNotNull(activeRound.nextCase(currentTrainingId))
                    val entry = AlgorithmCatalog.allEntries().single { it.id == id }
                    AlgorithmCatalog.setupNotation(entry) to entry.id
                } else if (category == null) {
                    scrambleGenerator.nextScramble() to ""
                } else {
                    val entries = AlgorithmCatalog.entries(category)
                    val entry = if (trainingPlan.caseNumber != null) {
                        entries.single { it.number == trainingPlan.caseNumber }
                    } else {
                        entries.random()
                    }
                    AlgorithmCatalog.setupNotation(entry) to entry.id
                }
            }
        }.onSuccess { (generated, trainingId) ->
            val retainedHistory = if (currentScrambleIndex >= 0) {
                scrambleHistory.take(currentScrambleIndex + 1)
            } else {
                emptyList()
            }
            val retainedTrainingHistory = if (currentScrambleIndex >= 0) {
                val expectedSize = retainedHistory.size
                scrambleTrainingHistory.take(expectedSize) +
                    List((expectedSize - scrambleTrainingHistory.size).coerceAtLeast(0)) { "" }
            } else {
                emptyList()
            }
            scrambleHistory = ArrayList(retainedHistory + generated)
            scrambleTrainingHistory = ArrayList(retainedTrainingHistory + trainingId)
            currentScrambleIndex = scrambleHistory.lastIndex
            scrambleError = null
            lastHandledScrambleRequest = scrambleRequest
        }.onFailure {
            if (it is CancellationException) throw it
            scrambleError = "Não foi possível gerar o embaralhamento"
        }
        scrambleLoading = false
    }

    LaunchedEffect(timerStateName, startedAtRealtime) {
        if (timerState != StopwatchPhase.RUNNING) return@LaunchedEffect
        while (true) {
            displayedMillis = stopwatch.elapsed(timerSession)
            delay(32L)
        }
    }

    LaunchedEffect(inspectionStartedAtRealtime, inspectionSoundsEnabled) {
        if (inspectionStartedAtRealtime <= 0L) return@LaunchedEffect
        while (true) {
            val elapsed = (SystemClock.elapsedRealtime() - inspectionStartedAtRealtime)
                .coerceAtLeast(0L)
            inspectionElapsedMillis = elapsed
            if (
                inspectionSoundsEnabled &&
                elapsed >= InspectionRules.EIGHT_SECOND_CALLOUT_MILLIS &&
                !eightSecondAlertPlayed
            ) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                eightSecondAlertPlayed = true
            }
            if (
                inspectionSoundsEnabled &&
                elapsed >= InspectionRules.TWELVE_SECOND_CALLOUT_MILLIS &&
                !twelveSecondAlertPlayed
            ) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
                twelveSecondAlertPlayed = true
            }
            delay(50L)
        }
    }

    fun startSolve(penalty: SolvePenalty) {
        displayedMillis = 0L
        automaticPenaltyName = penalty.name
        inspectionStartedAtRealtime = 0L
        inspectionElapsedMillis = 0L
        val started = stopwatch.start()
        startedAtRealtime = started.startedAtRealtimeMillis
        timerStateName = started.phase.name
    }

    fun handleTimerTap() {
        // Read live state: a second fast tap must not replay a stale RUNNING closure.
        when (StopwatchPhase.valueOf(timerStateName)) {
            StopwatchPhase.IDLE -> {
                if (scrambleLoading || scrambleError != null || currentScramble.isBlank()) return
                if (completedTraining &&
                    CfopTrainingRound(roundCaseIds, roundPracticedIds.toSet()).isComplete) return
                when {
                    inspectionStartedAtRealtime > 0L -> startSolve(
                        when (InspectionRules.penaltyAt(
                            (SystemClock.elapsedRealtime() - inspectionStartedAtRealtime).coerceAtLeast(0L),
                        )) {
                            InspectionPenalty.NONE -> SolvePenalty.NONE
                            InspectionPenalty.PLUS_TWO -> SolvePenalty.PLUS_TWO
                            InspectionPenalty.DNF -> SolvePenalty.DNF
                        },
                    )
                    inspectionEnabled -> {
                        inspectionElapsedMillis = 0L
                        eightSecondAlertPlayed = false
                        twelveSecondAlertPlayed = false
                        automaticPenaltyName = SolvePenalty.NONE.name
                        inspectionStartedAtRealtime = SystemClock.elapsedRealtime()
                    }
                    else -> startSolve(SolvePenalty.NONE)
                }
            }
            StopwatchPhase.RUNNING -> {
                val finished = stopwatch.finish(StopwatchSession(
                    phase = StopwatchPhase.RUNNING, startedAtRealtimeMillis = startedAtRealtime,
                ))
                displayedMillis = finished.durationMillis
                timerStateName = StopwatchPhase.IDLE.name
                startedAtRealtime = 0L
                if (finished.durationMillis > 0L) {
                    onSaveSolveTime(
                        finished.durationMillis,
                        currentScramble,
                        SolvePenalty.valueOf(automaticPenaltyName),
                        currentTrainingEntry?.category?.name,
                        currentTrainingEntry?.number,
                    )
                    automaticPenaltyName = SolvePenalty.NONE.name
                    val updatedRound = if (completedTraining) {
                        trainingRound.recordAttempt(currentTrainingId).also {
                            roundPracticedIds = ArrayList(it.practicedIds)
                        }
                    } else null
                    if (updatedRound?.isComplete != true) {
                        scrambleLoading = true
                        scrambleRequest += 1
                    }
                }
            }
            StopwatchPhase.PAUSED -> Unit
        }
    }

Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!active) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedSession?.name ?: "Principal",
                        style = MaterialTheme.typography.labelLarge, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(
                        trainingLabel + if (completedTraining && currentTrainingEntry != null)
                            " · ${currentTrainingEntry.category.label} ${currentTrainingEntry.number}"
                        else if (inspectionEnabled) " · Inspeção 15s ativa" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("timer-training-label"))
                }
                TextButton(onClick = { showOptions = true }) { Text("Opções") }
            }
            CompactScramblePanel(
                scramble = displayedScramble,
                loading = scrambleLoading,
                canShowNet = scrambleError == null && currentScramble.isNotBlank(),
                position = currentScrambleIndex + 1,
                historySize = scrambleHistory.size,
                onPrevious = {
                    if (currentScrambleIndex > 0) {
                        currentScrambleIndex -= 1
                        scrambleError = null
                    }
                },
                onNext = {
                    scrambleError = null
                    if (currentScrambleIndex < scrambleHistory.lastIndex) currentScrambleIndex += 1
                    else if (!completedTraining || !trainingRound.isComplete) {
                        scrambleLoading = true
                        scrambleRequest += 1
                    }
                },
                onShowNet = { showScrambleNet = true },
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("timer-touch-area")
                .semantics {
                    contentDescription = when {
                        timerState == StopwatchPhase.RUNNING ->
                            "Cronômetro em andamento. Toque para concluir"
                        isInspecting -> "Inspeção em andamento. Toque para iniciar a resolução"
                        scrambleLoading -> "Preparando próximo embaralhamento"
                        completedTraining && trainingRound.isComplete -> "Rodada concluída"
                        else -> "Cronômetro parado. Toque para iniciar"
                    }
                }
                .clickable(onClick = ::handleTimerTap),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val timerText = when {
                    !isInspecting -> formatDuration(displayedMillis)
                    inspectionPenalty == InspectionPenalty.DNF -> "DNF"
                    inspectionPenalty == InspectionPenalty.PLUS_TWO -> "+2"
                    else -> formatDuration(InspectionRules.remainingMillis(inspectionElapsedMillis))
                }
                // Fit minutes and enlarged system fonts without clipping the time.
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                val availableWidth = LocalConfiguration.current.screenWidthDp - 56f
                val numberSize = (availableWidth /
                    (timerText.length.coerceAtLeast(4) * 0.62f * fontScale)).coerceIn(24f, 100f).sp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timerText, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium, fontSize = numberSize, maxLines = 1)
                    Text(
                        when {
                            timerState == StopwatchPhase.RUNNING -> "Toque para concluir"
                            completedTraining && trainingRound.isComplete -> "Rodada concluída"
                            isInspecting -> inspectionInstruction(inspectionElapsedMillis)
                            scrambleLoading -> "Preparando próximo embaralhamento…"
                            inspectionEnabled -> "Toque para iniciar a inspeção"
                            else -> "Toque para iniciar"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    if (!active && completedTraining && trainingRound.isComplete) {
                        TextButton(onClick = { beginTraining(trainingPlan) }) { Text("Repetir treino") }
                    }
                }
            }
        }
        if (!active) {
            CompactStatistics(statistics)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showHistory = true }) { Text("Histórico") }
                TextButton(onClick = { showAnalysis = true }) { Text("Análise") }
            }
        }
    }
    if (showOptions && !active) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Opções do cronômetro") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionSelector(selectedSession, solveTimes.size) {
                        showOptions = false
                        showSessionManager = true
                    }
                    TrainingSelector(
                        trainingLabel, currentTrainingEntry,
                    ) {
                        showOptions = false
                        showTrainingSettings = true
                    }
                    FilterChip(
                        selected = inspectionEnabled,
                        onClick = {
                            onInspectionEnabledChange(!inspectionEnabled)
                            if (inspectionEnabled) onInspectionSoundsEnabledChange(false)
                        }, label = { Text("Inspeção 15s") })
                    FilterChip(
                        selected = inspectionSoundsEnabled,
                        onClick = { onInspectionSoundsEnabledChange(!inspectionSoundsEnabled) },
                        enabled = inspectionEnabled, label = { Text("Som 8s/12s") })
                    Text(
                        "Embaralhamentos por estado aleatório TNoodle para treino pessoal; " +
                            "não substitui o programa oficial em competições.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showOptions = false }) { Text("Fechar") } },
        )
    }
    if (showHistory && !active) {
        TimerHistoryDialog(solveTimes, selectedSession?.name ?: "Principal", statistics,
            onDismiss = { showHistory = false }, onSelect = { selectedSolveId = it.id },
            onClear = { confirmClear = true })
    }
    if (confirmAbort) {
        AlertDialog(
            onDismissRequest = { confirmAbort = false },
            title = { Text("Cancelar tentativa?") },
            text = { Text("O tempo continua contando. Cancelar não salva esta tentativa.") },
            confirmButton = {
                TextButton(onClick = {
                    timerStateName = StopwatchPhase.IDLE.name
                    startedAtRealtime = 0L
                    inspectionStartedAtRealtime = 0L
                    inspectionElapsedMillis = 0L
                    displayedMillis = 0L
                    automaticPenaltyName = SolvePenalty.NONE.name
                    confirmAbort = false
                }) { Text("Cancelar tentativa") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAbort = false }) { Text("Continuar") }
            },
        )
    }

    solveTimes.firstOrNull { it.id == selectedSolveId }?.let { solve ->
        SolveDetailDialog(
            solve = solve,
            solveNumber = solveTimes.size - solveTimes.indexOf(solve),
            onDismiss = { selectedSolveId = null },
            onSave = { comment, penalty ->
                onUpdateSolveTime(solve.id, comment, penalty)
                selectedSolveId = null
            },
            onDelete = {
                onDeleteSolveTime(solve.id)
                selectedSolveId = null
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar todo o histórico?") },
            text = {
                Text(
                    "Todos os tempos, scrambles, comentários e penalidades da sessão " +
                        "${selectedSession?.name ?: "atual"} serão excluídos.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearSolveTimes()
                    confirmClear = false
                }) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar") }
            },
        )
    }

    if (showScrambleNet && currentScramble.isNotBlank()) {
        ScrambleNetDialog(
            scramble = currentScramble,
            cfop = currentTrainingId.isNotBlank(),
            onDismiss = { showScrambleNet = false },
        )
    }

    if (showAnalysis) {
        TimerAnalysisDialog(
            solveTimes = solveTimes,
            sessionName = selectedSession?.name ?: "Sessão atual",
            onDismiss = { showAnalysis = false },
        )
    }

    if (showTrainingSettings) {
        CfopTrainingDialog(
            initialPlan = trainingPlan,
            progress = algorithmProgress,
            onDismiss = { showTrainingSettings = false },
            onApply = { plan ->
                beginTraining(plan)
                onTrainingPlanChange(plan)
                showTrainingSettings = false
            },
        )
    }

    if (showSessionManager) {
        SessionManagerDialog(
            sessions = sessions,
            selectedSessionId = selectedSessionId,
            onDismiss = { showSessionManager = false },
            onSelect = { id ->
                onSelectSession(id)
                showSessionManager = false
            },
            onCreate = {
                showSessionManager = false
                creatingSession = true
            },
            onRename = { id ->
                showSessionManager = false
                editingSessionId = id
            },
            onDelete = { id ->
                showSessionManager = false
                deletingSessionId = id
            },
        )
    }

    if (creatingSession) {
        SessionNameDialog(
            title = "Nova sessão",
            initialName = "",
            sessions = sessions,
            editingId = null,
            onDismiss = { creatingSession = false },
            onSave = { name ->
                onCreateSession(name)
                creatingSession = false
            },
        )
    }

    sessions.firstOrNull { it.id == editingSessionId }?.let { session ->
        SessionNameDialog(
            title = "Renomear sessão",
            initialName = session.name,
            sessions = sessions,
            editingId = session.id,
            onDismiss = { editingSessionId = null },
            onSave = { name ->
                onRenameSession(session.id, name)
                editingSessionId = null
            },
        )
    }

    sessions.firstOrNull { it.id == deletingSessionId }?.let { session ->
        AlertDialog(
            onDismissRequest = { deletingSessionId = null },
            title = { Text("Excluir ${session.name}?") },
            text = {
                Text(
                    "A sessão será removida, mas seus tempos não serão perdidos: " +
                        "eles serão movidos para $defaultSessionName.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session.id)
                    deletingSessionId = null
                }) { Text("Excluir sessão") }
            },
            dismissButton = {
                TextButton(onClick = { deletingSessionId = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TrainingSelector(
    label: String,
    currentEntry: AlgorithmEntry?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Configurar treino CFOP" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Modo do cronômetro", style = MaterialTheme.typography.labelMedium)
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                if (currentEntry == null) {
                    "Configurar"
                } else {
                    "Atual: ${currentEntry.category.label} ${currentEntry.number}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}


@Composable
private fun TimerAnalysisDialog(
    solveTimes: List<SolveTime>,
    sessionName: String,
    onDismiss: () -> Unit,
) {
    var periodName by rememberSaveable { mutableStateOf(AnalysisPeriod.ALL.name) }
    val period = AnalysisPeriod.valueOf(periodName)
    val analysis = remember(solveTimes, period) {
        TimerAnalysisCalculator.calculate(solveTimes, period)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Recordes e análise")
                Text(
                    sessionName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .height(560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Período", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnalysisPeriod.entries.chunked(2).forEach { rowPeriods ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPeriods.forEach { option ->
                                FilterChip(
                                    selected = period == option,
                                    onClick = { periodName = option.name },
                                    label = { Text(option.label) },
                                )
                            }
                        }
                    }
                }
                Text(
                    "${analysis.filteredSolves.size} de ${solveTimes.size} tempos neste período. " +
                        "O histórico completo permanece disponível na tela anterior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Recordes pessoais", style = MaterialTheme.typography.titleSmall)
                RecordsPanel(analysis.statistics)
                CfopPerformancePanel(analysis.filteredSolves)
                AnalysisChartCard(
                    title = "Evolução",
                    subtitle = "Tempos válidos em ordem cronológica",
                    emptyMessage = "Registre um tempo válido para visualizar a evolução.",
                    hasData = analysis.evolution.isNotEmpty(),
                ) {
                    PerformanceLineChart(
                        points = analysis.evolution.takeLast(100),
                        contentDescription = "Gráfico de evolução dos tempos",
                    )
                }
                AnalysisChartCard(
                    title = "Distribuição",
                    subtitle = "Quantidade de resultados por faixa",
                    emptyMessage = "Ainda não há resultados válidos para distribuir.",
                    hasData = analysis.distribution.isNotEmpty(),
                ) {
                    DistributionChart(analysis.distribution)
                }
                AnalysisChartCard(
                    title = "Consistência",
                    subtitle = "Evolução da média móvel ao12",
                    emptyMessage = "São necessários 12 resultados na janela para calcular o ao12.",
                    hasData = analysis.consistency.isNotEmpty(),
                ) {
                    PerformanceLineChart(
                        points = analysis.consistency.takeLast(100),
                        contentDescription = "Gráfico de consistência por ao12",
                    )
                }
                Text(
                    "No ao100 são descartados os 5 melhores e os 5 piores resultados. " +
                        "DNFs ocupam as piores posições.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}

@Composable
internal fun CfopPerformancePanel(solves: List<SolveTime>) {
    var showCases by rememberSaveable { mutableStateOf(false) }
    var categoryFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    var evolutionCaseId by rememberSaveable { mutableStateOf<String?>(null) }
    val cases = remember(solves) { CfopExecutionAnalysis.calculate(solves) }
    val directed = solves.filter {
        it.trainingCategory != null && it.trainingCaseNumber != null
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Desempenho CFOP", style = MaterialTheme.typography.titleSmall)
            if (directed.isEmpty()) {
                Text(
                    "As tentativas do treino direcionado aparecerão aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AlgorithmCategory.entries.forEach { category ->
                    val categorySolves = directed.filter {
                        it.trainingCategory == category.name
                    }
                    if (categorySolves.isNotEmpty()) {
                        val valid = categorySolves.mapNotNull(SolveTime::effectiveDurationMillis)
                        val average = valid.takeIf { it.isNotEmpty() }?.average()?.toLong()
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${category.label} · ${categorySolves.size} tentativas · " +
                                    "${categorySolves.mapNotNull(SolveTime::trainingCaseNumber).distinct().size} casos",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Melhor: ${valid.minOrNull()?.let(::formatDuration) ?: "DNF"} · " +
                                    "Média: ${average?.let(::formatDuration) ?: "DNF"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                TextButton(onClick = { showCases = !showCases },
                    modifier = Modifier.testTag("execution-show-cases")) {
                    Text(if (showCases) "Ocultar casos" else "Ver desempenho por caso (${cases.size})")
                }
                if (showCases) {
                    Text("Execução na sessão e período selecionados. Média válida inclui +2 e " +
                        "exclui DNF; não é ao5 nem mede os acertos do Quiz.",
                        style = MaterialTheme.typography.bodySmall)
                    FilterChip(selected = categoryFilterName == null,
                        onClick = { categoryFilterName = null }, label = { Text("Todas as etapas") },
                        modifier = Modifier.testTag("execution-category-ALL"))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlgorithmCategory.entries.forEach { category ->
                            FilterChip(selected = categoryFilterName == category.name,
                                onClick = { categoryFilterName = category.name },
                                label = { Text(category.label) },
                                modifier = Modifier.testTag("execution-category-${category.name}"))
                        }
                    }
                    val visible = cases.filter {
                        categoryFilterName == null || it.entry.category.name == categoryFilterName
                    }
                    if (visible.isEmpty()) Text("Nenhum tempo de execução nesta etapa.")
                    visible.forEach { result ->
                        Column(modifier = Modifier.testTag("execution-case-${result.entry.id}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${result.entry.category.label} · Caso ${result.entry.number}",
                                fontWeight = FontWeight.SemiBold)
                            Text("${result.attemptCount} tentativas · ${result.dnfCount} DNF",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Melhor: ${result.bestMillis?.let(::formatDuration) ?: "DNF"} · " +
                                "Média válida: ${result.averageValidMillis?.let(::formatDuration) ?: "DNF"}",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Último: ${formatAverage(result.latestResult)}",
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(
                                onClick = {
                                    evolutionCaseId = result.entry.id.takeUnless { evolutionCaseId == it }
                                },
                                modifier = Modifier.testTag("execution-evolution-${result.entry.id}"),
                            ) {
                                Text(if (evolutionCaseId == result.entry.id) "Ocultar evolução"
                                    else "Ver evolução")
                            }
                            if (evolutionCaseId == result.entry.id) {
                                if (result.evolution.isEmpty()) Text("Ainda não há tempos válidos neste caso.")
                                else PerformanceLineChart(result.evolution.takeLast(100),
                                    "Evolução da execução ${result.entry.category.label} caso ${result.entry.number}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsPanel(statistics: com.gabs.cubo3x3.domain.timer.SolveStatistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordRow(
                listOf(
                    "Single" to statistics.bestTime,
                    "mo3" to statistics.bestMo3,
                    "ao5" to statistics.bestAo5,
                ),
            )
            RecordRow(
                listOf(
                    "ao12" to statistics.bestAo12,
                    "ao100" to statistics.bestAo100,
                ),
            )
        }
    }
}

@Composable
private fun RecordRow(records: List<Pair<String, AverageResult>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        records.forEach { (label, result) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatAverage(result),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
        if (records.size == 2) Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnalysisChartCard(
    title: String,
    subtitle: String,
    emptyMessage: String,
    hasData: Boolean,
    chart: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                chart()
                if (!hasData) {
                    Text(
                        emptyMessage,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { contentDescription = emptyMessage },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceLineChart(
    points: List<AnalysisPoint>,
    contentDescription: String,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            if (points.isEmpty()) return@Canvas
            repeat(3) { guide ->
                val y = size.height * guide / 2f
                drawLine(guideColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val minimum = points.minOf(AnalysisPoint::durationMillis)
            val maximum = points.maxOf(AnalysisPoint::durationMillis)
            val span = (maximum - minimum).coerceAtLeast(1L)
            val coordinates = points.mapIndexed { index, point ->
                val x = if (points.size == 1) {
                    size.width / 2f
                } else {
                    size.width * index / (points.size - 1).toFloat()
                }
                val normalized = (point.durationMillis - minimum).toFloat() / span.toFloat()
                Offset(x, size.height - normalized * size.height)
            }
            coordinates.zipWithNext().forEach { (start, end) ->
                drawLine(lineColor, start, end, strokeWidth = 4f)
            }
            coordinates.forEach { point -> drawCircle(lineColor, radius = 5f, center = point) }
        }
        if (points.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Melhor ${formatDuration(points.minOf(AnalysisPoint::durationMillis))}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Pior ${formatDuration(points.maxOf(AnalysisPoint::durationMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DistributionChart(buckets: List<DistributionBucket>) {
    val barColor = MaterialTheme.colorScheme.tertiary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .semantics { contentDescription = "Gráfico de distribuição dos tempos" },
        ) {
            if (buckets.isEmpty()) return@Canvas
            drawLine(
                guideColor,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                strokeWidth = 2f,
            )
            val maximumCount = buckets.maxOf(DistributionBucket::count).coerceAtLeast(1)
            val slotWidth = size.width / buckets.size
            buckets.forEachIndexed { index, bucket ->
                val height = size.height * bucket.count / maximumCount.toFloat()
                drawRect(
                    color = barColor,
                    topLeft = Offset(index * slotWidth + slotWidth * 0.12f, size.height - height),
                    size = Size(slotWidth * 0.76f, height),
                )
            }
        }
        buckets.forEach { bucket ->
            Text(
                "${formatDuration(bucket.minimumMillis)}–" +
                    "${formatDuration(bucket.maximumMillis)}: ${bucket.count}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionSelector(
    session: TimerSession?,
    solveCount: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Selecionar sessão do cronômetro" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sessão", style = MaterialTheme.typography.labelMedium)
                Text(
                    session?.name ?: "Carregando…",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
            }
            Text(
                "$solveCount tempos · Gerenciar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SessionManagerDialog(
    sessions: List<TimerSession>,
    selectedSessionId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onRename: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sessões do cronômetro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sessions.forEach { session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            FilterChip(
                                selected = session.id == selectedSessionId,
                                onClick = { onSelect(session.id) },
                                label = { Text(session.name) },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onRename(session.id) }) {
                                    Text("Renomear")
                                }
                                if (session.id != DEFAULT_TIMER_SESSION_ID) {
                                    TextButton(onClick = { onDelete(session.id) }) {
                                        Text("Excluir")
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onCreate) { Text("Criar nova sessão") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun SessionNameDialog(
    title: String,
    initialName: String,
    sessions: List<TimerSession>,
    editingId: Long?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(initialName, editingId) { mutableStateOf(initialName) }
    val normalized = name.trim()
    val duplicate = sessions.any {
        it.id != editingId && it.name.equals(normalized, ignoreCase = true)
    }
    val error = when {
        normalized.isEmpty() -> "Informe um nome."
        normalized.length > TimerSessionRepository.MAX_NAME_LENGTH ->
            "Use no máximo ${TimerSessionRepository.MAX_NAME_LENGTH} caracteres."
        duplicate -> "Já existe uma sessão com esse nome."
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                supportingText = error?.let { message -> ({ Text(message) }) },
                isError = error != null,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = error == null,
                onClick = { onSave(normalized) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CompactScramblePanel(
    scramble: String, loading: Boolean, canShowNet: Boolean, position: Int, historySize: Int,
    onPrevious: () -> Unit, onNext: () -> Unit, onShowNet: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(
                if (loading) "Gerando estado aleatório…" else scramble,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onShowNet, enabled = !loading && canShowNet) { Text("Cubo 2D") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPrevious, enabled = !loading && position > 1) { Text("Anterior") }
                TextButton(onClick = onNext, enabled = !loading) {
                    Text(if (position in 1 until historySize) "Próximo" else "Novo")
                }
            }
        }
    }
}

@Composable
private fun CompactStatistics(statistics: com.gabs.cubo3x3.domain.timer.SolveStatistics) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Melhor" to statistics.bestTime, "mo3" to statistics.currentMo3,
            "ao5" to statistics.currentAo5, "ao12" to statistics.currentAo12).forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatAverage(value), style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TimerHistoryDialog(
    solveTimes: List<SolveTime>, sessionName: String,
    statistics: com.gabs.cubo3x3.domain.timer.SolveStatistics,
    onDismiss: () -> Unit, onSelect: (SolveTime) -> Unit, onClear: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Histórico de tempos", style = MaterialTheme.typography.titleLarge)
                        Text("$sessionName · ${solveTimes.size} tentativas",
                            style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
                StatisticsPanel(statistics)
                HistoryTable(solveTimes, Modifier.fillMaxWidth().weight(1f), onSelect)
                if (solveTimes.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Limpar sessão") }
                }
            }
        }
    }
}

@Composable
private fun ScrambleNetDialog(
    scramble: String,
    cfop: Boolean,
    onDismiss: () -> Unit,
) {
    val net = remember(scramble, cfop) {
        runCatching {
            CubeNet.from(
                (if (cfop) CubeState.solved() else CubeState.solvedWcaScrambleOrientation())
                    .apply(MoveNotation.parseAlgorithm(scramble)),
            )
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cubo após o embaralhamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (net == null) {
                    Text("Não foi possível desenhar este embaralhamento.")
                } else {
                    ScrambleCubeNet(net)
                    Text(
                        "Planificação: U acima; L, F, R e B ao centro; D abaixo. " +
                            if (cfop) "Orientação CFOP: amarelo em cima e vermelho à frente."
                            else "Orientação WCA: branco em cima e verde à frente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}

@Composable
private fun ScrambleCubeNet(net: CubeNet) {
    val outline = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .semantics { contentDescription = "Planificação 2D do cubo embaralhado" },
    ) {
        val cell = minOf(size.width / 12f, size.height / 9f)
        val netWidth = cell * 12f
        val netHeight = cell * 9f
        val origin = Offset((size.width - netWidth) / 2f, (size.height - netHeight) / 2f)
        val gap = cell * 0.05f

        fun drawFace(colors: List<StickerColor>, column: Int, row: Int) {
            colors.forEachIndexed { index, color ->
                val cellColumn = index % 3
                val cellRow = index / 3
                val topLeft = Offset(
                    x = origin.x + (column * 3 + cellColumn) * cell + gap,
                    y = origin.y + (row * 3 + cellRow) * cell + gap,
                )
                val stickerSize = Size(cell - gap * 2f, cell - gap * 2f)
                drawRect(
                    color = color.toComposeColor(CubeColorScheme.STANDARD),
                    topLeft = topLeft,
                    size = stickerSize,
                )
                drawRect(
                    color = outline,
                    topLeft = topLeft,
                    size = stickerSize,
                    style = Stroke(width = maxOf(1f, cell * 0.035f)),
                )
            }
        }

        drawFace(net.up, column = 1, row = 0)
        drawFace(net.left, column = 0, row = 1)
        drawFace(net.front, column = 1, row = 1)
        drawFace(net.right, column = 2, row = 1)
        drawFace(net.back, column = 3, row = 1)
        drawFace(net.down, column = 1, row = 2)
    }
}

@Composable
private fun StatisticsPanel(statistics: com.gabs.cubo3x3.domain.timer.SolveStatistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            StatisticsRow("", "Tempo", "mo3", "ao5", "ao12", header = true)
            StatisticsRow(
                "Atual",
                formatAverage(statistics.currentTime),
                formatAverage(statistics.currentMo3),
                formatAverage(statistics.currentAo5),
                formatAverage(statistics.currentAo12),
            )
            StatisticsRow(
                "Melhor",
                formatAverage(statistics.bestTime),
                formatAverage(statistics.bestMo3),
                formatAverage(statistics.bestAo5),
                formatAverage(statistics.bestAo12),
            )
        }
    }
}

@Composable
private fun StatisticsRow(label: String, time: String, mo3: String, ao5: String, ao12: String, header: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(label, time, mo3, ao5, ao12).forEachIndexed { index, value ->
            Text(
                value,
                modifier = Modifier.weight(if (index == 0) 0.9f else 1f),
                style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = if (index == 0 || header) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                fontFamily = if (!header && index > 0) FontFamily.Monospace else null,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HistoryTable(
    solveTimes: List<SolveTime>,
    modifier: Modifier,
    onSelect: (SolveTime) -> Unit,
) {
    Card(modifier = modifier) {
        if (solveTimes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Os tempos concluídos aparecerão aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    HistoryRow("#", "Tempo", "mo3", "ao5", "ao12", header = true)
                }
                itemsIndexed(solveTimes, key = { _, solve -> solve.id }) { index, solve ->
                    Box(modifier = Modifier.clickable { onSelect(solve) }) {
                        HistoryRow(
                            number = (solveTimes.size - index).toString(),
                            time = formatSolve(solve),
                            mo3 = formatAverage(
                                SolveStatisticsCalculator.windowAt(solveTimes, index, 3, trimmed = false),
                            ),
                            ao5 = formatAverage(
                                SolveStatisticsCalculator.windowAt(solveTimes, index, 5, trimmed = true),
                            ),
                            ao12 = formatAverage(
                                SolveStatisticsCalculator.windowAt(solveTimes, index, 12, trimmed = true),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    number: String,
    time: String,
    mo3: String,
    ao5: String,
    ao12: String,
    header: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (header) 7.dp else 9.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(number, time, mo3, ao5, ao12).forEachIndexed { index, value ->
            Text(
                value,
                modifier = Modifier.weight(if (index == 0) 0.55f else 1f),
                style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                fontFamily = if (!header && index > 0) FontFamily.Monospace else null,
                textAlign = TextAlign.Center,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SolveDetailDialog(
    solve: SolveTime,
    solveNumber: Int,
    onDismiss: () -> Unit,
    onSave: (String, SolvePenalty) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var comment by rememberSaveable(solve.id) { mutableStateOf(solve.comment) }
    var penaltyName by rememberSaveable(solve.id) { mutableStateOf(solve.penalty.name) }
    val penalty = SolvePenalty.valueOf(penaltyName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solve nº $solveNumber · ${formatSolve(solve.copy(penalty = penalty))}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Resultado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SolvePenalty.entries.forEach { option ->
                        FilterChip(
                            selected = penalty == option,
                            onClick = { penaltyName = option.name },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 2_000) comment = it },
                    label = { Text("Comentário") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                DetailField("Embaralhamento", solve.scramble.ifBlank { "Não registrado" })
                if (solve.trainingCategory != null && solve.trainingCaseNumber != null) {
                    DetailField(
                        "Treino CFOP",
                        "${solve.trainingCategory} · Caso ${solve.trainingCaseNumber}",
                    )
                }
                DetailField("Data", formatRecordedAt(solve.recordedAtEpochMillis))
                TextButton(onClick = { copySolve(context, solve, solveNumber, comment, penalty) }) {
                    Text("Copiar detalhes")
                }
                TextButton(onClick = onDelete) { Text("Excluir este tempo") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(comment, penalty) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private val SolvePenalty.label: String
    get() = when (this) {
        SolvePenalty.NONE -> "OK"
        SolvePenalty.PLUS_TWO -> "+2"
        SolvePenalty.DNF -> "DNF"
    }

private fun copySolve(
    context: Context,
    solve: SolveTime,
    solveNumber: Int,
    comment: String,
    penalty: SolvePenalty,
) {
    val text = buildString {
        appendLine("Solve nº $solveNumber: ${formatSolve(solve.copy(penalty = penalty))}")
        appendLine("Data: ${formatRecordedAt(solve.recordedAtEpochMillis)}")
        appendLine("Embaralhamento: ${solve.scramble.ifBlank { "Não registrado" }}")
        if (solve.trainingCategory != null && solve.trainingCaseNumber != null) {
            appendLine("Treino CFOP: ${solve.trainingCategory} · Caso ${solve.trainingCaseNumber}")
        }
        if (comment.isNotBlank()) append("Comentário: ${comment.trim()}")
    }.trim()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Solve 3x3", text))
}

private fun formatSolve(solve: SolveTime): String = when (solve.penalty) {
    SolvePenalty.NONE -> formatDuration(solve.durationMillis)
    SolvePenalty.PLUS_TWO -> "${formatDuration(solve.effectiveDurationMillis ?: solve.durationMillis)}+"
    SolvePenalty.DNF -> "DNF"
}

private fun formatAverage(result: AverageResult): String = when {
    result.isDnf -> "DNF"
    result.durationMillis == null -> "—"
    else -> formatDuration(result.durationMillis)
}

private fun formatDuration(durationMillis: Long): String {
    val safeDuration = durationMillis.coerceAtLeast(0L)
    val minutes = safeDuration / 60_000L
    val seconds = (safeDuration / 1_000L) % 60L
    val centiseconds = (safeDuration / 10L) % 100L
    return if (minutes == 0L) {
        String.format(Locale.ROOT, "%d.%02d", seconds, centiseconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, centiseconds)
    }
}

private fun inspectionInstruction(elapsedMillis: Long): String = when {
    elapsedMillis >= InspectionRules.DNF_THRESHOLD_MILLIS ->
        "DNF de inspeção. Toque para cronometrar a resolução"
    elapsedMillis >= InspectionRules.PLUS_TWO_THRESHOLD_MILLIS ->
        "+2 de inspeção. Toque para iniciar a resolução"
    elapsedMillis >= InspectionRules.TWELVE_SECOND_CALLOUT_MILLIS ->
        "12 segundos — toque para iniciar a resolução"
    elapsedMillis >= InspectionRules.EIGHT_SECOND_CALLOUT_MILLIS ->
        "8 segundos — toque para iniciar a resolução"
    else -> "Inspeção em andamento — toque para iniciar a resolução"
}

private val recordedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatRecordedAt(epochMillis: Long): String = if (epochMillis == 0L) "Data não registrada" else Instant
    .ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(recordedAtFormatter)
