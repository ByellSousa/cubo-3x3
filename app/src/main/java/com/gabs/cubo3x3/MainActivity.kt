package com.gabs.cubo3x3

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.gabs.cubo3x3.data.progress.AlgorithmProgressRepository
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.backup.BackupException
import com.gabs.cubo3x3.data.backup.BackupRepository
import com.gabs.cubo3x3.data.backup.PreparedBackup
import com.gabs.cubo3x3.data.quiz.QuizRecordRepository
import com.gabs.cubo3x3.data.quiz.CfopAttemptRepository
import com.gabs.cubo3x3.data.formula.PreferredFormulaRepository
import com.gabs.cubo3x3.data.custom.CustomAlgorithmRepository
import com.gabs.cubo3x3.data.timer.SolveTimeRepository
import com.gabs.cubo3x3.data.timer.DEFAULT_TIMER_SESSION_ID
import com.gabs.cubo3x3.data.timer.TimerSessionRepository
import com.gabs.cubo3x3.data.transfer.PreparedTimerExport
import com.gabs.cubo3x3.data.transfer.TimerTransferException
import com.gabs.cubo3x3.data.transfer.TimerTransferPreview
import com.gabs.cubo3x3.data.transfer.TimerTransferRepository
import com.gabs.cubo3x3.preferences.AppPreferences
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.reminder.ReminderNotifier
import com.gabs.cubo3x3.reminder.ReminderScheduler
import com.gabs.cubo3x3.ui.Cubo3x3App
import com.gabs.cubo3x3.ui.TimerTransferActions
import com.gabs.cubo3x3.ui.TimerTransferUiState
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import com.gabs.cubo3x3.ui.theme.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences(applicationContext)
        val database = CuboDatabase.getInstance(applicationContext)
        val progressRepository = AlgorithmProgressRepository(database.algorithmProgressDao())
        val solveTimeRepository = SolveTimeRepository(database.solveTimeDao())
        val timerSessionRepository = TimerSessionRepository(database.timerSessionDao())
        val timerTransferRepository = TimerTransferRepository(database)
        val quizRecordRepository = QuizRecordRepository(database.quizRecordDao())
        val cfopRepository = CfopAttemptRepository(database.cfopAttemptDao())
        val formulaRepository = PreferredFormulaRepository(database.preferredFormulaDao())
        val cfopMutationMutex = Mutex()
        val customAlgorithmRepository = CustomAlgorithmRepository(database.customAlgorithmDao())
        val backupRepository = BackupRepository(
            database = database,
            preferences = preferences,
            appVersion = BuildConfig.VERSION_NAME,
        )
        val reminderScheduler = ReminderScheduler(applicationContext)
        setContent {
            val scope = rememberCoroutineScope()
            var backupStatus by remember { mutableStateOf<String?>(null) }
            var quizHistoryRevision by rememberSaveable { mutableLongStateOf(0L) }
            // Immutable revision captured by editor callbacks, even if validation finishes after restore.
            val formulaWriteRevision = quizHistoryRevision
            val goalWriteRevision = quizHistoryRevision
            var pendingBackup by remember { mutableStateOf<PreparedBackup?>(null) }
            var transferBusy by remember { mutableStateOf(false) }
            var transferStatus by remember { mutableStateOf<String?>(null) }
            var pendingTimerImport by remember { mutableStateOf<TimerTransferPreview?>(null) }
            var pendingTimerExport by remember { mutableStateOf<PreparedTimerExport?>(null) }
            var showTimerExportPreview by remember { mutableStateOf(false) }
            var reminderStatus by remember { mutableStateOf<String?>(null) }
            var pendingReminderSettings by remember { mutableStateOf<ReminderSettings?>(null) }
            var reminderPermissionGranted by remember {
                mutableStateOf(ReminderNotifier.canPostNotifications(this@MainActivity))
            }
            val reminderPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                reminderPermissionGranted = granted ||
                    ReminderNotifier.canPostNotifications(this@MainActivity)
                val requestedSettings = pendingReminderSettings
                pendingReminderSettings = null
                if (reminderPermissionGranted && requestedSettings != null) {
                    scope.launch { preferences.setReminderSettings(requestedSettings) }
                    reminderStatus = "Lembrete ativado."
                } else {
                    reminderStatus = "Permissão não concedida; o lembrete continua desativado."
                }
            }
            val createBackupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        backupStatus = "Exportando backup…"
                        try {
                            val output = requireNotNull(contentResolver.openOutputStream(uri, "w"))
                            val summary = backupRepository.exportTo(output)
                            backupStatus =
                                "Backup exportado: ${summary.progressCount} marcações, " +
                                "${summary.solveTimeCount} tempos em ${summary.timerSessionCount} sessões, " +
                                "${summary.quizRecordCount} recordes e " +
                                "${summary.customAlgorithmCount} algoritmos personalizados e " +
                                "${summary.cfopAttemptCount} respostas CFOP e " +
                                "${summary.preferredFormulaCount} fórmulas preferidas."
                        } catch (error: Exception) {
                            backupStatus = error.toBackupMessage("Não foi possível exportar o backup")
                        }
                    }
                }
            }
            val openBackupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        backupStatus = "Validando backup…"
                        pendingBackup = null
                        try {
                            val input = requireNotNull(contentResolver.openInputStream(uri))
                            pendingBackup = backupRepository.inspect(input)
                            backupStatus = "Arquivo validado. Confirme para substituir os dados atuais."
                        } catch (error: Exception) {
                            backupStatus = error.toBackupMessage("Não foi possível importar o backup")
                        }
                    }
                }
            }
            val createTimerExportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/plain"),
            ) { uri ->
                val prepared = pendingTimerExport
                if (uri == null || prepared == null) {
                    pendingTimerExport = null
                    transferBusy = false
                    transferStatus = "Exportação cancelada."
                } else {
                    scope.launch {
                        try {
                            val output = requireNotNull(contentResolver.openOutputStream(uri, "w"))
                            timerTransferRepository.exportTo(prepared, output)
                            transferStatus = "JSON exportado: ${prepared.preview.solveCount} tempos em ${prepared.preview.sessions.size} sessões."
                        } catch (error: Exception) {
                            transferStatus = error.toBackupMessage("Não foi possível exportar os tempos")
                        } finally {
                            pendingTimerExport = null
                            transferBusy = false
                        }
                    }
                }
            }
            val openTimerImportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) {
                    transferBusy = false
                    transferStatus = "Importação cancelada."
                } else {
                    scope.launch {
                        try {
                            val input = requireNotNull(contentResolver.openInputStream(uri))
                            pendingTimerImport = timerTransferRepository.inspect(input)
                            transferStatus = "Arquivo validado. Revise a prévia antes de importar."
                        } catch (error: Exception) {
                            transferStatus = error.toBackupMessage("Não foi possível ler os tempos")
                        } finally {
                            transferBusy = false
                        }
                    }
                }
            }
            val savedTheme by preferences.themeMode.collectAsState(initial = null)
            val savedReminderSettings by preferences.reminderSettings.collectAsState(initial = null)
            val timerInspectionEnabled by preferences.timerInspectionEnabled.collectAsState(initial = false)
            val timerInspectionSoundsEnabled by
                preferences.timerInspectionSoundsEnabled.collectAsState(initial = false)
            val selectedTimerSessionId by preferences.selectedTimerSessionId
                .collectAsState(initial = DEFAULT_TIMER_SESSION_ID)
            val algorithmProgress by progressRepository.progress.collectAsState(initial = null)
            val cfopTrainingPlan by preferences.cfopTrainingPlan.collectAsState(initial = null)
            val dailyGoals by preferences.dailyGoals.collectAsState(initial = null)
            val dailyGoalTimes by solveTimeRepository.directedTimes.collectAsState(initial = null)
            val timerSessions by timerSessionRepository.sessions.collectAsState(initial = emptyList())
            val selectedSolveTimes = remember(selectedTimerSessionId) {
                solveTimeRepository.observeSession(selectedTimerSessionId)
            }
            val solveTimes by selectedSolveTimes.collectAsState(initial = emptyList())
            val quizRecords by quizRecordRepository.records.collectAsState(initial = emptyMap())
            val cfopAttempts by cfopRepository.attempts.collectAsState(initial = null)
            val preferredFormulas by formulaRepository.formulas.collectAsState(initial = null)
            val customAlgorithms by customAlgorithmRepository.algorithms.collectAsState(initial = emptyList())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (savedTheme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> systemDark
            }

            LaunchedEffect(savedReminderSettings, reminderPermissionGranted) {
                savedReminderSettings?.let(reminderScheduler::apply)
            }

            LaunchedEffect(timerSessions, selectedTimerSessionId) {
                if (timerSessions.isEmpty()) {
                    timerSessionRepository.ensureDefaultSession()
                } else if (timerSessions.none { it.id == selectedTimerSessionId }) {
                    val fallback = timerSessions.firstOrNull { it.id == DEFAULT_TIMER_SESSION_ID }
                        ?: timerSessions.first()
                    preferences.setSelectedTimerSessionId(fallback.id)
                }
            }

            Cubo3x3Theme(darkTheme = darkTheme) {
                if (savedTheme == null || savedReminderSettings == null ||
                    algorithmProgress == null || cfopTrainingPlan == null || preferredFormulas == null ||
                    dailyGoals == null || dailyGoalTimes == null || cfopAttempts == null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Carregando 3x3",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                } else {
                    Cubo3x3App(
                        themeMode = savedTheme ?: ThemeMode.SYSTEM,
                        algorithmProgress = algorithmProgress.orEmpty(),
                        cfopTrainingPlan = cfopTrainingPlan ?: CfopTrainingPlan(),
                        dailyGoals = dailyGoals ?: com.gabs.cubo3x3.domain.training.DailyGoalSettings(),
                        dailyGoalTimes = dailyGoalTimes.orEmpty(),
                        onDailyGoalsChange = { goals ->
                            cfopMutationMutex.withLock {
                                check(goalWriteRevision == quizHistoryRevision) {
                                    "O backup substituiu os dados. Reabra as metas para editar."
                                }
                                preferences.setDailyGoals(goals)
                            }
                        },
                        preferredFormulas = preferredFormulas.orEmpty(),
                        onPreferredFormulaChange = { entry, notation ->
                            cfopMutationMutex.withLock {
                                check(formulaWriteRevision == quizHistoryRevision) {
                                    "O backup substituiu os dados. Reabra o caso para editar."
                                }
                                formulaRepository.change(entry.category, entry.number, notation)
                            }
                        },
                        onCfopTrainingPlanChange = { plan ->
                            val revision = quizHistoryRevision
                            scope.launch {
                                cfopMutationMutex.withLock {
                                    if (revision == quizHistoryRevision) preferences.setCfopTrainingPlan(plan)
                                }
                            }
                        },
                        solveTimes = solveTimes,
                        timerSessions = timerSessions,
                        selectedTimerSessionId = selectedTimerSessionId,
                        quizRecords = quizRecords,
                        cfopAttempts = cfopAttempts.orEmpty(),
                        quizHistoryRevision = quizHistoryRevision,
                        onSaveCfopAttempt = { attempt ->
                            val revision = quizHistoryRevision
                            scope.launch {
                                cfopMutationMutex.withLock {
                                    if (revision == quizHistoryRevision) cfopRepository.save(attempt)
                                }
                            }
                        },
                        customAlgorithms = customAlgorithms,
                        reminderSettings = savedReminderSettings ?: ReminderSettings(),
                        reminderPermissionGranted = reminderPermissionGranted,
                        reminderStatusMessage = reminderStatus,
                        timerInspectionEnabled = timerInspectionEnabled,
                        timerInspectionSoundsEnabled = timerInspectionSoundsEnabled,
                        onThemeModeChange = { mode ->
                            scope.launch { preferences.setThemeMode(mode) }
                        },
                        onTimerInspectionEnabledChange = { enabled ->
                            scope.launch { preferences.setTimerInspectionEnabled(enabled) }
                        },
                        onTimerInspectionSoundsEnabledChange = { enabled ->
                            scope.launch { preferences.setTimerInspectionSoundsEnabled(enabled) }
                        },
                        onFavoriteChange = { entry, isFavorite ->
                            scope.launch {
                                progressRepository.setFavorite(
                                    category = entry.category.name,
                                    caseNumber = entry.number,
                                    isFavorite = isFavorite,
                                )
                            }
                        },
                        onCompletedChange = { entry, isCompleted ->
                            scope.launch {
                                progressRepository.setCompleted(
                                    category = entry.category.name,
                                    caseNumber = entry.number,
                                    isCompleted = isCompleted,
                                )
                            }
                        },
                        onClearAlgorithmProgress = {
                            scope.launch { progressRepository.clear() }
                        },
                        onSaveSolveTime = {
                                durationMillis,
                                scramble,
                                penalty,
                                trainingCategory,
                                trainingCaseNumber,
                            ->
                            scope.launch {
                                solveTimeRepository.add(
                                    durationMillis,
                                    scramble,
                                    penalty,
                                    selectedTimerSessionId,
                                    trainingCategory,
                                    trainingCaseNumber,
                                )
                            }
                        },
                        onUpdateSolveTime = { id, comment, penalty ->
                            scope.launch { solveTimeRepository.updateDetails(id, comment, penalty) }
                        },
                        onDeleteSolveTime = { id ->
                            scope.launch { solveTimeRepository.delete(id) }
                        },
                        onClearSolveTimes = {
                            scope.launch { solveTimeRepository.clear() }
                        },
                        onClearCurrentTimerSession = {
                            scope.launch { solveTimeRepository.clearSession(selectedTimerSessionId) }
                        },
                        onSelectTimerSession = { id ->
                            scope.launch { preferences.setSelectedTimerSessionId(id) }
                        },
                        onCreateTimerSession = { name ->
                            scope.launch {
                                val id = timerSessionRepository.create(name)
                                preferences.setSelectedTimerSessionId(id)
                            }
                        },
                        onRenameTimerSession = { id, name ->
                            scope.launch { timerSessionRepository.rename(id, name) }
                        },
                        onDeleteTimerSession = { id ->
                            scope.launch {
                                preferences.setSelectedTimerSessionId(DEFAULT_TIMER_SESSION_ID)
                                timerSessionRepository.deleteAndKeepSolves(id)
                            }
                        },
                        onSaveQuizResult = { level, mode, score ->
                            scope.launch { quizRecordRepository.saveResult(level, mode, score) }
                        },
                        onClearQuizRecords = {
                            scope.launch { quizRecordRepository.clear() }
                        },
                        onCreateCustomAlgorithm = { name, notation, tags, scheme, viewpoint ->
                            scope.launch {
                                customAlgorithmRepository.create(name, notation, tags, scheme, viewpoint)
                            }
                        },
                        onUpdateCustomAlgorithm = { id, name, notation, tags, scheme, viewpoint ->
                            scope.launch {
                                customAlgorithmRepository.update(
                                    id, name, notation, tags, scheme, viewpoint,
                                )
                            }
                        },
                        onDuplicateCustomAlgorithm = { id ->
                            scope.launch { customAlgorithmRepository.duplicate(id) }
                        },
                        onDeleteCustomAlgorithm = { id ->
                            scope.launch { customAlgorithmRepository.delete(id) }
                        },
                        backupStatusMessage = backupStatus,
                        timerTransferState = TimerTransferUiState(
                            busy = transferBusy,
                            message = transferStatus,
                            importPreview = pendingTimerImport,
                            exportPreview = if (showTimerExportPreview) pendingTimerExport?.preview else null,
                        ),
                        timerTransferActions = TimerTransferActions(
                            chooseImport = {
                                if (!transferBusy) {
                                    transferBusy = true
                                    transferStatus = "Escolha o JSON exportado pelo csTimer."
                                    pendingTimerImport = null
                                    pendingTimerExport = null
                                    showTimerExportPreview = false
                                    openTimerImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                                }
                            },
                            prepareExport = {
                                if (!transferBusy) {
                                    transferBusy = true
                                    transferStatus = "Preparando exportação…"
                                    pendingTimerImport = null
                                    scope.launch {
                                        try {
                                            pendingTimerExport = timerTransferRepository.prepareExport()
                                            showTimerExportPreview = true
                                            transferStatus = "Revise a prévia antes de escolher o arquivo."
                                        } catch (error: Exception) {
                                            transferStatus = error.toBackupMessage("Não foi possível preparar os tempos")
                                        } finally {
                                            transferBusy = false
                                        }
                                    }
                                }
                            },
                            confirmImport = {
                                val prepared = pendingTimerImport
                                if (!transferBusy && prepared != null) {
                                    transferBusy = true
                                    transferStatus = "Importando em novas sessões…"
                                    scope.launch {
                                        try {
                                            timerTransferRepository.importAsNewSessions(prepared)
                                            pendingTimerImport = null
                                            transferStatus = "Importados ${prepared.solveCount} tempos em ${prepared.sessions.size} novas sessões."
                                        } catch (error: Exception) {
                                            transferStatus = error.toBackupMessage("Não foi possível importar os tempos")
                                        } finally {
                                            transferBusy = false
                                        }
                                    }
                                }
                            },
                            confirmExport = {
                                if (!transferBusy && pendingTimerExport != null) {
                                    transferBusy = true
                                    showTimerExportPreview = false
                                    val timestamp = backupFileFormatter.format(Instant.now().atZone(ZoneId.systemDefault()))
                                    createTimerExportLauncher.launch("3x3-cstimer-$timestamp.txt")
                                }
                            },
                            cancelPreview = {
                                if (!transferBusy) {
                                    pendingTimerImport = null
                                    pendingTimerExport = null
                                    showTimerExportPreview = false
                                    transferStatus = null
                                }
                            },
                        ),
                        pendingBackupRestore = pendingBackup?.summary,
                        onExportBackup = {
                            backupStatus = null
                            val timestamp = backupFileFormatter.format(
                                Instant.now().atZone(ZoneId.systemDefault()),
                            )
                            createBackupLauncher.launch("3x3-backup-$timestamp.3x3backup")
                        },
                        onChooseBackupImport = {
                            backupStatus = null
                            pendingBackup = null
                            openBackupLauncher.launch(
                                arrayOf("application/json", "application/octet-stream", "*/*"),
                            )
                        },
                        onConfirmBackupRestore = {
                            val backup = pendingBackup ?: return@Cubo3x3App
                            scope.launch {
                                backupStatus = "Restaurando backup…"
                                try {
                                    cfopMutationMutex.withLock {
                                        // A confirmed replacement invalidates old round state and queued writes.
                                        quizHistoryRevision += 1
                                        backupRepository.restore(backup)
                                        // Rebuild the timer with fully restored progress AND preferences.
                                        quizHistoryRevision += 1
                                    }
                                    pendingBackup = null
                                    backupStatus = "Backup restaurado com sucesso."
                                } catch (error: Exception) {
                                    backupStatus = error.toBackupMessage(
                                        "Não foi possível restaurar o backup",
                                    )
                                }
                            }
                        },
                        onCancelBackupRestore = {
                            pendingBackup = null
                            backupStatus = "Restauração cancelada; os dados atuais foram mantidos."
                        },
                        onSaveReminderSettings = { settings ->
                            reminderStatus = null
                            if (
                                settings.enabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !ReminderNotifier.canPostNotifications(this@MainActivity)
                            ) {
                                pendingReminderSettings = settings
                                reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scope.launch { preferences.setReminderSettings(settings) }
                                reminderStatus = if (settings.enabled) {
                                    "Lembrete salvo."
                                } else {
                                    "Lembrete desativado."
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private val backupFileFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private fun Throwable.toBackupMessage(prefix: String): String = when (this) {
    is BackupException -> message ?: prefix
    is TimerTransferException -> message ?: prefix
    else -> "$prefix. Tente novamente."
}
