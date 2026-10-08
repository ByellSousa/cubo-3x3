package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.training.DailyGoalSettings
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.data.backup.BackupSummary
import com.gabs.cubo3x3.data.quiz.QuizRecord
import com.gabs.cubo3x3.data.custom.CustomAlgorithm
import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.data.timer.TimerSession
import com.gabs.cubo3x3.BuildConfig
import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.QuizScore
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.prototype.CubePrototypeScreen
import com.gabs.cubo3x3.ui.theme.F2LDark
import com.gabs.cubo3x3.ui.theme.F2LLight
import com.gabs.cubo3x3.ui.theme.OLLDark
import com.gabs.cubo3x3.ui.theme.OLLLight
import com.gabs.cubo3x3.ui.theme.PLLDark
import com.gabs.cubo3x3.ui.theme.PLLLight
import com.gabs.cubo3x3.ui.theme.ThemeMode
import kotlinx.coroutines.delay

private enum class AppDestination(val label: String, val subtitle: String) {
    HOME("Início", "Seu treino de cubo 3x3"),
    ALGORITHMS("Algoritmos", "Estude F2L, OLL e PLL"),
    TIMER("Cronômetro", "Tempos salvos no aparelho"),
    QUIZ("Quiz", "Notação e reconhecimento CFOP"),
    MORE("Mais", "Ferramentas e preferências"),
}

@Composable
fun Cubo3x3App(
    themeMode: ThemeMode,
    algorithmProgress: Map<String, AlgorithmProgress>,
    solveTimes: List<SolveTime>,
    timerSessions: List<TimerSession>,
    selectedTimerSessionId: Long,
    quizRecords: Map<String, QuizRecord>,
    customAlgorithms: List<CustomAlgorithm>,
    reminderSettings: ReminderSettings,
    reminderPermissionGranted: Boolean,
    reminderStatusMessage: String?,
    timerInspectionEnabled: Boolean,
    timerInspectionSoundsEnabled: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTimerInspectionEnabledChange: (Boolean) -> Unit,
    onTimerInspectionSoundsEnabledChange: (Boolean) -> Unit,
    onFavoriteChange: (AlgorithmEntry, Boolean) -> Unit,
    onCompletedChange: (AlgorithmEntry, Boolean) -> Unit,
    onClearAlgorithmProgress: () -> Unit,
    onSaveSolveTime: (
        Long,
        String,
        com.gabs.cubo3x3.data.timer.SolvePenalty,
        String?,
        Int?,
    ) -> Unit,
    onUpdateSolveTime: (Long, String, com.gabs.cubo3x3.data.timer.SolvePenalty) -> Unit,
    onDeleteSolveTime: (Long) -> Unit,
    onClearSolveTimes: () -> Unit,
    onClearCurrentTimerSession: () -> Unit,
    onSelectTimerSession: (Long) -> Unit,
    onCreateTimerSession: (String) -> Unit,
    onRenameTimerSession: (Long, String) -> Unit,
    onDeleteTimerSession: (Long) -> Unit,
    onSaveQuizResult: (QuizLevel, QuizMode, QuizScore) -> Unit,
    onClearQuizRecords: () -> Unit,
    onCreateCustomAlgorithm: (String, String, List<String>, CubeColorScheme, CubeViewpoint) -> Unit,
    onUpdateCustomAlgorithm: (Long, String, String, List<String>, CubeColorScheme, CubeViewpoint) -> Unit,
    onDuplicateCustomAlgorithm: (Long) -> Unit,
    onDeleteCustomAlgorithm: (Long) -> Unit,
    backupStatusMessage: String?,
    pendingBackupRestore: BackupSummary?,
    onExportBackup: () -> Unit,
    onChooseBackupImport: () -> Unit,
    onConfirmBackupRestore: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    onSaveReminderSettings: (ReminderSettings) -> Unit,
    timerTransferState: TimerTransferUiState = TimerTransferUiState(),
    timerTransferActions: TimerTransferActions = TimerTransferActions(),
    cfopAttempts: List<CfopAttempt> = emptyList(),
    onSaveCfopAttempt: (CfopAttempt) -> Unit = {},
    quizHistoryRevision: Long = 0,
    cfopTrainingPlan: CfopTrainingPlan = CfopTrainingPlan(),
    onCfopTrainingPlanChange: (CfopTrainingPlan) -> Unit = {},
    preferredFormulas: Map<String, PreferredCaseFormula> = emptyMap(),
    onPreferredFormulaChange: (suspend (AlgorithmEntry, String?) -> Unit)? = null,
    dailyGoals: DailyGoalSettings = DailyGoalSettings(),
    dailyGoalTimes: List<SolveTime> = emptyList(),
    onDailyGoalsChange: suspend (DailyGoalSettings) -> Unit = {},
) {
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var prototypeOpen by rememberSaveable { mutableStateOf(false) }
    var algorithmCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var savedAlgorithmFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlgorithmCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlgorithmNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var customAlgorithmsOpen by rememberSaveable { mutableStateOf(false) }
    var backupOpen by rememberSaveable { mutableStateOf(false) }
    var timerTransferOpen by rememberSaveable { mutableStateOf(false) }
    var reminderOpen by rememberSaveable { mutableStateOf(false) }
    var dailyGoalsOpen by rememberSaveable { mutableStateOf(false) }
    val destination = AppDestination.valueOf(destinationName)
    val algorithmCategory = algorithmCategoryName?.let(AlgorithmCategory::valueOf)
    val savedAlgorithmFilter = savedAlgorithmFilterName?.let(SavedAlgorithmFilter::valueOf)
    val selectedAlgorithmCategory = selectedAlgorithmCategoryName?.let(AlgorithmCategory::valueOf)
    val f2lListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val ollListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pllListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val favoritesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val completedListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val catalogStateHolder = rememberSaveableStateHolder()
    val appStateHolder = rememberSaveableStateHolder()

    BackHandler(
        enabled = prototypeOpen || customAlgorithmsOpen || backupOpen || timerTransferOpen || reminderOpen || dailyGoalsOpen ||
            algorithmCategory != null || savedAlgorithmFilter != null || selectedAlgorithmNumber != null,
    ) {
        when {
            prototypeOpen -> prototypeOpen = false
            customAlgorithmsOpen -> customAlgorithmsOpen = false
            timerTransferOpen -> {
                if (!timerTransferState.busy) timerTransferActions.cancelPreview()
                timerTransferOpen = false
            }
            backupOpen -> backupOpen = false
            reminderOpen -> reminderOpen = false
            dailyGoalsOpen -> dailyGoalsOpen = false
            selectedAlgorithmNumber != null -> {
                selectedAlgorithmNumber = null
                selectedAlgorithmCategoryName = null
            }
            savedAlgorithmFilter != null -> savedAlgorithmFilterName = null
            else -> algorithmCategoryName = null
        }
    }

    if (dailyGoalsOpen) {
        androidx.compose.runtime.key(quizHistoryRevision) {
            DailyGoalsScreen(dailyGoals, cfopAttempts, dailyGoalTimes, onDailyGoalsChange,
                onBack = { dailyGoalsOpen = false })
        }
        return
    }

    if (prototypeOpen) {
        CubePrototypeScreen()
        return
    }

    if (customAlgorithmsOpen) {
        CustomAlgorithmsScreen(
            algorithms = customAlgorithms,
            onCreate = onCreateCustomAlgorithm,
            onUpdate = onUpdateCustomAlgorithm,
            onDuplicate = onDuplicateCustomAlgorithm,
            onDelete = onDeleteCustomAlgorithm,
            onBack = { customAlgorithmsOpen = false },
        )
        return
    }

    if (timerTransferOpen) {
        TimerTransferScreen(
            state = timerTransferState,
            actions = timerTransferActions,
            onBack = {
                if (!timerTransferState.busy) timerTransferActions.cancelPreview()
                timerTransferOpen = false
            },
        )
        return
    }

    if (backupOpen) {
        BackupScreen(
            statusMessage = backupStatusMessage,
            pendingRestore = pendingBackupRestore,
            onExport = onExportBackup,
            onChooseImport = onChooseBackupImport,
            onConfirmRestore = onConfirmBackupRestore,
            onCancelRestore = onCancelBackupRestore,
            onBack = { backupOpen = false },
            onOpenTimerTransfer = { timerTransferOpen = true },
        )
        return
    }

    if (reminderOpen) {
        ReminderScreen(
            settings = reminderSettings,
            permissionGranted = reminderPermissionGranted,
            statusMessage = reminderStatusMessage,
            onSave = onSaveReminderSettings,
            onBack = { reminderOpen = false },
        )
        return
    }

    val selectedNumber = selectedAlgorithmNumber
    if (selectedAlgorithmCategory != null && selectedNumber != null) {
        androidx.compose.runtime.key(quizHistoryRevision) {
        AlgorithmDetailScreen(
            category = selectedAlgorithmCategory,
            number = selectedNumber,
            progress = algorithmProgress["${selectedAlgorithmCategory.name}-$selectedNumber"]
                ?: AlgorithmProgress(),
            onFavoriteChange = onFavoriteChange,
            onCompletedChange = onCompletedChange,
            preferredFormula = preferredFormulas["${selectedAlgorithmCategory.name}-$selectedNumber"],
            onPreferredFormulaChange = onPreferredFormulaChange,
            onBack = {
                selectedAlgorithmNumber = null
                selectedAlgorithmCategoryName = null
            },
        )
        }
        return
    }

    if (algorithmCategory != null) {
        val listState = when (algorithmCategory) {
            AlgorithmCategory.F2L -> f2lListState
            AlgorithmCategory.OLL -> ollListState
            AlgorithmCategory.PLL -> pllListState
        }
        catalogStateHolder.SaveableStateProvider("category-${algorithmCategory.name}") {
            AlgorithmListScreen(
                category = algorithmCategory,
                progress = algorithmProgress,
                listState = listState,
                onBack = { algorithmCategoryName = null },
                onOpenAlgorithm = { entry ->
                    selectedAlgorithmCategoryName = entry.category.name
                    selectedAlgorithmNumber = entry.number
                },
            )
        }
        return
    }

    if (savedAlgorithmFilter != null) {
        catalogStateHolder.SaveableStateProvider("saved-${savedAlgorithmFilter.name}") {
            SavedAlgorithmListScreen(
                filter = savedAlgorithmFilter,
                progress = algorithmProgress,
                listState = when (savedAlgorithmFilter) {
                    SavedAlgorithmFilter.FAVORITES -> favoritesListState
                    SavedAlgorithmFilter.COMPLETED -> completedListState
                },
                onBack = { savedAlgorithmFilterName = null },
                onOpenAlgorithm = { entry ->
                    selectedAlgorithmCategoryName = entry.category.name
                    selectedAlgorithmNumber = entry.number
                },
            )
        }
        return
    }

    AppShell(
        stateHolder = appStateHolder,
        cfopAttempts = cfopAttempts,
        dailyGoals = dailyGoals,
        dailyGoalTimes = dailyGoalTimes,
        onOpenDailyGoals = { dailyGoalsOpen = true },
        onSaveCfopAttempt = onSaveCfopAttempt,
        quizHistoryRevision = quizHistoryRevision,
        onOpenStudyCase = {
            selectedAlgorithmCategoryName = it.category.name
            selectedAlgorithmNumber = it.number
        },
        destination = destination,
        algorithmProgress = algorithmProgress,
        solveTimes = solveTimes,
        timerSessions = timerSessions,
        selectedTimerSessionId = selectedTimerSessionId,
        cfopTrainingPlan = cfopTrainingPlan,
        onCfopTrainingPlanChange = onCfopTrainingPlanChange,
        quizRecords = quizRecords,
        onDestinationChange = { destinationName = it.name },
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        onOpenPrototype = { prototypeOpen = true },
        onOpenAlgorithmCategory = { category ->
            algorithmCategoryName = category.name
            savedAlgorithmFilterName = null
            selectedAlgorithmNumber = null
        },
        onOpenSavedAlgorithms = { filter ->
            savedAlgorithmFilterName = filter.name
            algorithmCategoryName = null
            selectedAlgorithmNumber = null
        },
        onSaveSolveTime = onSaveSolveTime,
        timerInspectionEnabled = timerInspectionEnabled,
        timerInspectionSoundsEnabled = timerInspectionSoundsEnabled,
        onTimerInspectionEnabledChange = onTimerInspectionEnabledChange,
        onTimerInspectionSoundsEnabledChange = onTimerInspectionSoundsEnabledChange,
        onUpdateSolveTime = onUpdateSolveTime,
        onDeleteSolveTime = onDeleteSolveTime,
        onClearSolveTimes = onClearSolveTimes,
        onClearCurrentTimerSession = onClearCurrentTimerSession,
        onSelectTimerSession = onSelectTimerSession,
        onCreateTimerSession = onCreateTimerSession,
        onRenameTimerSession = onRenameTimerSession,
        onDeleteTimerSession = onDeleteTimerSession,
        onClearAlgorithmProgress = onClearAlgorithmProgress,
        onSaveQuizResult = onSaveQuizResult,
        onClearQuizRecords = onClearQuizRecords,
        onOpenCustomAlgorithms = { customAlgorithmsOpen = true },
        onOpenBackup = { backupOpen = true },
        onOpenReminders = { reminderOpen = true },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(
    stateHolder: SaveableStateHolder,
    dailyGoals: DailyGoalSettings,
    dailyGoalTimes: List<SolveTime>,
    onOpenDailyGoals: () -> Unit,
    cfopAttempts: List<CfopAttempt>,
    onSaveCfopAttempt: (CfopAttempt) -> Unit,
    quizHistoryRevision: Long,
    onOpenStudyCase: (AlgorithmEntry) -> Unit,
    destination: AppDestination,
    algorithmProgress: Map<String, AlgorithmProgress>,
    solveTimes: List<SolveTime>,
    timerSessions: List<TimerSession>,
    selectedTimerSessionId: Long,
    cfopTrainingPlan: CfopTrainingPlan,
    onCfopTrainingPlanChange: (CfopTrainingPlan) -> Unit,
    quizRecords: Map<String, QuizRecord>,
    onDestinationChange: (AppDestination) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenPrototype: () -> Unit,
    onOpenAlgorithmCategory: (AlgorithmCategory) -> Unit,
    onOpenSavedAlgorithms: (SavedAlgorithmFilter) -> Unit,
    onSaveSolveTime: (
        Long,
        String,
        com.gabs.cubo3x3.data.timer.SolvePenalty,
        String?,
        Int?,
    ) -> Unit,
    timerInspectionEnabled: Boolean,
    timerInspectionSoundsEnabled: Boolean,
    onTimerInspectionEnabledChange: (Boolean) -> Unit,
    onTimerInspectionSoundsEnabledChange: (Boolean) -> Unit,
    onUpdateSolveTime: (Long, String, com.gabs.cubo3x3.data.timer.SolvePenalty) -> Unit,
    onDeleteSolveTime: (Long) -> Unit,
    onClearSolveTimes: () -> Unit,
    onClearCurrentTimerSession: () -> Unit,
    onSelectTimerSession: (Long) -> Unit,
    onCreateTimerSession: (String) -> Unit,
    onRenameTimerSession: (Long, String) -> Unit,
    onDeleteTimerSession: (Long) -> Unit,
    onClearAlgorithmProgress: () -> Unit,
    onSaveQuizResult: (QuizLevel, QuizMode, QuizScore) -> Unit,
    onClearQuizRecords: () -> Unit,
    onOpenCustomAlgorithms: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    var timerActive by remember { mutableStateOf(false) }
    val timerFocused = destination == AppDestination.TIMER && timerActive

    Scaffold(
        topBar = {
            if (destination != AppDestination.TIMER) {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CubeMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(destination.label, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = destination.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
            }
        },
        bottomBar = {
            if (!timerFocused) {
            NavigationBar {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { onDestinationChange(item) },
                        icon = {
                            NavigationGlyph(
                                destination = item,
                                selected = item == destination,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            stateHolder.SaveableStateProvider(
                if (destination == AppDestination.TIMER) "TIMER-$quizHistoryRevision" else destination.name,
            ) {
                when (destination) {
                    AppDestination.HOME -> HomeScreen(
                        algorithmProgress = algorithmProgress,
                        dailyGoals = dailyGoals,
                        attempts = cfopAttempts,
                        times = dailyGoalTimes,
                        onOpenDailyGoals = onOpenDailyGoals,
                        onNavigate = onDestinationChange,
                        onOpenPrototype = onOpenPrototype,
                        onOpenCategory = onOpenAlgorithmCategory,
                    )
                    AppDestination.ALGORITHMS -> AlgorithmsScreen(
                        algorithmProgress = algorithmProgress,
                        onOpenCategory = onOpenAlgorithmCategory,
                        onOpenSaved = onOpenSavedAlgorithms,
                    )
                    AppDestination.TIMER -> TimerScreen(
                        algorithmProgress = algorithmProgress,
                        initialTrainingPlan = cfopTrainingPlan,
                        onTrainingPlanChange = onCfopTrainingPlanChange,
                        solveTimes = solveTimes,
                        sessions = timerSessions,
                        selectedSessionId = selectedTimerSessionId,
                        inspectionEnabled = timerInspectionEnabled,
                        inspectionSoundsEnabled = timerInspectionSoundsEnabled,
                        onInspectionEnabledChange = onTimerInspectionEnabledChange,
                        onInspectionSoundsEnabledChange = onTimerInspectionSoundsEnabledChange,
                        onSaveSolveTime = onSaveSolveTime,
                        onUpdateSolveTime = onUpdateSolveTime,
                        onDeleteSolveTime = onDeleteSolveTime,
                        onClearSolveTimes = onClearCurrentTimerSession,
                        onSelectSession = onSelectTimerSession,
                        onCreateSession = onCreateTimerSession,
                        onRenameSession = onRenameTimerSession,
                        onDeleteSession = onDeleteTimerSession,
                        onActiveChange = { timerActive = it },
                    )
                    AppDestination.QUIZ -> QuizHubScreen(
                        records = quizRecords,
                        onSaveResult = onSaveQuizResult,
                        attempts = cfopAttempts,
                        onSaveAttempt = onSaveCfopAttempt,
                        onOpenCase = onOpenStudyCase,
                        historyRevision = quizHistoryRevision,
                    )
                    AppDestination.MORE -> MoreScreen(
                        onOpenDailyGoals = onOpenDailyGoals,
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onOpenPrototype = onOpenPrototype,
                        onClearAlgorithmProgress = onClearAlgorithmProgress,
                        onClearSolveTimes = onClearSolveTimes,
                        onClearQuizRecords = onClearQuizRecords,
                        onOpenCustomAlgorithms = onOpenCustomAlgorithms,
                        onOpenBackup = onOpenBackup,
                        onOpenReminders = onOpenReminders,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    dailyGoals: DailyGoalSettings,
    attempts: List<CfopAttempt>,
    times: List<SolveTime>,
    onOpenDailyGoals: () -> Unit,
    algorithmProgress: Map<String, AlgorithmProgress>,
    onNavigate: (AppDestination) -> Unit,
    onOpenPrototype: () -> Unit,
    onOpenCategory: (AlgorithmCategory) -> Unit,
) {
    val goalProgress = if (dailyGoals.enabled) rememberDailyGoalProgress(attempts, times) else null
    ScreenList {
        item {
            HeroCard(onOpenPrototype)
        }
        item {
            SectionTitle("Método CFOP", "Progresso local por etapa")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProgressCard(
                    "F2L",
                    "${completedCount(AlgorithmCategory.F2L, algorithmProgress)} de 41",
                    categoryColor(Category.F2L),
                    onClick = { onOpenCategory(AlgorithmCategory.F2L) },
                    modifier = Modifier.weight(1f),
                )
                ProgressCard(
                    "OLL",
                    "${completedCount(AlgorithmCategory.OLL, algorithmProgress)} de 57",
                    categoryColor(Category.OLL),
                    onClick = { onOpenCategory(AlgorithmCategory.OLL) },
                    modifier = Modifier.weight(1f),
                )
                ProgressCard(
                    "PLL",
                    "${completedCount(AlgorithmCategory.PLL, algorithmProgress)} de 21",
                    categoryColor(Category.PLL),
                    onClick = { onOpenCategory(AlgorithmCategory.PLL) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionTitle("Acessos rápidos", "Tudo funciona localmente")
        }
        item {
            ActionCard(
                title = "Explorar algoritmos",
                body = "Listas F2L, OLL e PLL sem bloqueios.",
                badge = "CFOP",
                onClick = { onNavigate(AppDestination.ALGORITHMS) },
            )
        }
        item {
            ActionCard(
                title = "Abrir cronômetro",
                body = "Prepare o treino e consulte seus tempos.",
                badge = "00:00",
                onClick = { onNavigate(AppDestination.TIMER) },
            )
        }
        item {
            ActionCard(
                title = "Quiz de notação",
                body = "Treine reconhecimento de movimentos.",
                badge = "R U R'",
                onClick = { onNavigate(AppDestination.QUIZ) },
            )
        }
        if (dailyGoals.enabled) item {
            DailyGoalProgressCard(dailyGoals, goalProgress, onConfigure = onOpenDailyGoals)
        }
    }
}

@Composable
private fun HeroCard(onOpenPrototype: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPrototype),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusPill("PROTÓTIPO APROVADO")
            Text("Continue praticando", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Abra o visualizador do cubo, reproduza sequências e ajuste a velocidade.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Abrir protótipo do cubo  ›",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AlgorithmsScreen(
    algorithmProgress: Map<String, AlgorithmProgress>,
    onOpenCategory: (AlgorithmCategory) -> Unit,
    onOpenSaved: (SavedAlgorithmFilter) -> Unit,
) {
    ScreenList {
        item {
            SectionTitle("Sua biblioteca", "Itens salvos neste aparelho")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionCard(
                    title = "Favoritos",
                    body = "Algoritmos marcados para revisar.",
                    badge = algorithmProgress.count { it.value.isFavorite }.toString(),
                    onClick = { onOpenSaved(SavedAlgorithmFilter.FAVORITES) },
                    modifier = Modifier.weight(1f),
                )
                ActionCard(
                    title = "Concluídos",
                    body = "Casos que você já estudou.",
                    badge = algorithmProgress.count { it.value.isCompleted }.toString(),
                    onClick = { onOpenSaved(SavedAlgorithmFilter.COMPLETED) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            CategoryCard(
                category = Category.F2L,
                title = "F2L",
                description = "Pares da primeira e segunda camadas",
                count = "${completedCount(AlgorithmCategory.F2L, algorithmProgress)}/41",
                onClick = { onOpenCategory(AlgorithmCategory.F2L) },
            )
        }
        item {
            CategoryCard(
                category = Category.OLL,
                title = "OLL",
                description = "Orientação da última camada",
                count = "${completedCount(AlgorithmCategory.OLL, algorithmProgress)}/57",
                onClick = { onOpenCategory(AlgorithmCategory.OLL) },
            )
        }
        item {
            CategoryCard(
                category = Category.PLL,
                title = "PLL",
                description = "Permutação da última camada",
                count = "${completedCount(AlgorithmCategory.PLL, algorithmProgress)}/21",
                onClick = { onOpenCategory(AlgorithmCategory.PLL) },
            )
        }
        item {
            Text(
                "119 casos únicos fornecidos pelo usuário e verificados pelo estado do cubo. O estado resolvido completa as 42 configurações F2L.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoreScreen(
    onOpenDailyGoals: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenPrototype: () -> Unit,
    onClearAlgorithmProgress: () -> Unit,
    onClearSolveTimes: () -> Unit,
    onClearQuizRecords: () -> Unit,
    onOpenCustomAlgorithms: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    var confirmClearProgress by remember { mutableStateOf(false) }
    var confirmClearTimes by remember { mutableStateOf(false) }
    var confirmClearQuiz by remember { mutableStateOf(false) }

    ScreenList {
        item { SectionTitle("Aparência", "Escolha como o aplicativo deve ser exibido") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.label()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("Ferramentas", "Recursos locais do aplicativo") }
        item {
            ActionCard(title = "Metas diárias", body = "Objetivos opcionais de reconhecimento e treino CFOP.",
                badge = "PRÁTICA", onClick = onOpenDailyGoals)
        }
        item {
            ActionCard(
                title = "Lembretes locais",
                body = "Escolha horário e dias para lembrar de praticar.",
                badge = "OPCIONAL",
                onClick = onOpenReminders,
            )
        }
        item {
            ActionCard(
                title = "Visualizador do cubo",
                body = "Prototipo aprovado com sequencias e controle de velocidade.",
                badge = "3x3",
                onClick = onOpenPrototype,
            )
        }
        item { SectionTitle("Dados", "Gerencie somente os dados salvos neste aparelho") }
        item {
            ActionCard(
                title = "Limpar favoritos e concluídos",
                body = "Remove todas as marcações do catálogo CFOP.",
                badge = "LIMPAR",
                onClick = { confirmClearProgress = true },
            )
        }
        item {
            ActionCard(
                title = "Limpar tempos",
                body = "Remove todo o histórico do cronômetro.",
                badge = "LIMPAR",
                onClick = { confirmClearTimes = true },
            )
        }
        item {
            ActionCard(
                title = "Limpar recordes do Quiz",
                body = "Remove pontuações, sequências e precisão salvas.",
                badge = "LIMPAR",
                onClick = { confirmClearQuiz = true },
            )
        }
        item {
            ActionCard(
                title = "Algoritmos personalizados",
                body = "Construtor liberado, sem recursos Premium.",
                badge = "CRIAR",
                onClick = onOpenCustomAlgorithms,
            )
        }
        item {
            ActionCard(
                title = "Backup e restauração",
                body = "Arquivo local .3x3backup pelo seletor do Android.",
                badge = "LOCAL",
                onClick = onOpenBackup,
            )
        }
        item {
            Text(
                "3x3 · versão ${BuildConfig.VERSION_NAME} · gratuito, offline e sem anúncios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (confirmClearProgress) {
        AlertDialog(
            onDismissRequest = { confirmClearProgress = false },
            title = { Text("Limpar favoritos e concluídos?") },
            text = { Text("Todas as marcações de estudo serão removidas deste aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAlgorithmProgress()
                        confirmClearProgress = false
                    },
                ) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearProgress = false }) { Text("Cancelar") }
            },
        )
    }

    if (confirmClearTimes) {
        AlertDialog(
            onDismissRequest = { confirmClearTimes = false },
            title = { Text("Limpar todos os tempos?") },
            text = { Text("Todo o histórico do cronômetro será removido deste aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearSolveTimes()
                        confirmClearTimes = false
                    },
                ) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearTimes = false }) { Text("Cancelar") }
            },
        )
    }

    if (confirmClearQuiz) {
        AlertDialog(
            onDismissRequest = { confirmClearQuiz = false },
            title = { Text("Limpar recordes do Quiz?") },
            text = { Text("Todos os recordes locais do Quiz serão removidos deste aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearQuizRecords()
                        confirmClearQuiz = false
                    },
                ) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearQuiz = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ScreenList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
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
private fun InfoCard(title: String, body: String) {
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
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            message,
            modifier = Modifier.padding(22.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class Category { F2L, OLL, PLL }

@Composable
private fun categoryColor(category: Category): Color {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    return when (category) {
        Category.F2L -> if (dark) F2LDark else F2LLight
        Category.OLL -> if (dark) OLLDark else OLLLight
        Category.PLL -> if (dark) PLLDark else PLLLight
    }
}

@Composable
private fun ProgressCard(
    title: String,
    progress: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Abrir $title" },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(color = accent, shape = RoundedCornerShape(999.dp)) {
                Spacer(modifier = Modifier.size(width = 28.dp, height = 5.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                progress,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    title: String,
    description: String,
    count: String,
    onClick: () -> Unit,
) {
    val accent = categoryColor(category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Abrir $title" },
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                contentColor = accent,
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Text(title, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(count, style = MaterialTheme.typography.labelMedium, color = accent)
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    body: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(contentColor = contentColor),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(width = 62.dp, height = 46.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge, style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                Text("›", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun completedCount(
    category: AlgorithmCategory,
    progress: Map<String, AlgorithmProgress>,
): Int = AlgorithmCatalog.entries(category).count { entry ->
    progress[entry.id]?.isCompleted == true
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}

@Composable
private fun CubeMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier.semantics { contentDescription = "Marca 3x3" },
    ) {
        val cx = size.width / 2f
        val top = size.height * 0.08f
        val middle = size.height * 0.38f
        val bottom = size.height * 0.88f
        val left = size.width * 0.08f
        val right = size.width * 0.92f

        drawPath(Path().apply {
            moveTo(cx, top)
            lineTo(right, middle)
            lineTo(cx, size.height * 0.66f)
            lineTo(left, middle)
            close()
        }, primary)
        drawPath(Path().apply {
            moveTo(left, middle)
            lineTo(cx, size.height * 0.66f)
            lineTo(cx, bottom)
            lineTo(left, size.height * 0.62f)
            close()
        }, secondary)
        drawPath(Path().apply {
            moveTo(cx, size.height * 0.66f)
            lineTo(right, middle)
            lineTo(right, size.height * 0.62f)
            lineTo(cx, bottom)
            close()
        }, tertiary)
    }
}

@Composable
private fun NavigationGlyph(
    destination: AppDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier.semantics { contentDescription = destination.label },
    ) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        val pad = size.minDimension * 0.16f
        when (destination) {
            AppDestination.HOME -> {
                val roof = Path().apply {
                    moveTo(pad, size.height * 0.48f)
                    lineTo(size.width / 2f, pad)
                    lineTo(size.width - pad, size.height * 0.48f)
                }
                drawPath(roof, color, style = stroke)
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.26f, size.height * 0.46f),
                    size = Size(size.width * 0.48f, size.height * 0.38f),
                    style = stroke,
                )
            }
            AppDestination.ALGORITHMS -> {
                repeat(3) { index ->
                    val y = size.height * (0.25f + index * 0.25f)
                    drawCircle(color, size.minDimension * 0.045f, Offset(pad, y))
                    drawLine(color, Offset(size.width * 0.34f, y), Offset(size.width - pad, y), stroke.width, StrokeCap.Round)
                }
            }
            AppDestination.TIMER -> {
                drawCircle(color, size.minDimension * 0.32f, center, style = stroke)
                drawLine(color, center, Offset(center.x, size.height * 0.31f), stroke.width, StrokeCap.Round)
                drawLine(color, center, Offset(size.width * 0.66f, size.height * 0.60f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.42f, pad), Offset(size.width * 0.58f, pad), stroke.width, StrokeCap.Round)
            }
            AppDestination.QUIZ -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2, size.height - pad * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.12f),
                    style = stroke,
                )
                drawCircle(color, size.minDimension * 0.045f, Offset(center.x, size.height * 0.72f))
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 225f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.34f, size.height * 0.25f),
                    size = Size(size.width * 0.32f, size.height * 0.32f),
                    style = stroke,
                )
            }
            AppDestination.MORE -> {
                repeat(3) { index ->
                    drawCircle(
                        color,
                        size.minDimension * 0.075f,
                        Offset(size.width * (0.25f + index * 0.25f), center.y),
                    )
                }
            }
        }
    }
}
