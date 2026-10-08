package com.gabs.cubo3x3.data.backup

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.ui.theme.ThemeMode
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.domain.training.DailyGoalSettings

data class BackupPackage(
    val appVersion: String,
    val exportedAtEpochMillis: Long,
    val themeMode: ThemeMode,
    val reminderSettings: ReminderSettings,
    val progress: List<BackupProgress>,
    val timerSessions: List<BackupTimerSession>,
    val solveTimes: List<BackupSolveTime>,
    val quizRecords: List<BackupQuizRecord>,
    val customAlgorithms: List<BackupCustomAlgorithm>,
    val cfopAttempts: List<CfopAttempt> = emptyList(),
    val cfopTrainingPlan: CfopTrainingPlan = CfopTrainingPlan(),
    val preferredFormulas: List<PreferredCaseFormula> = emptyList(),
    val dailyGoals: DailyGoalSettings = DailyGoalSettings(),
)

data class BackupTimerSession(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
)

data class BackupProgress(
    val category: String,
    val caseNumber: Int,
    val isFavorite: Boolean,
    val isCompleted: Boolean,
    val updatedAtEpochMillis: Long,
)

data class BackupSolveTime(
    val id: Long,
    val durationMillis: Long,
    val recordedAtEpochMillis: Long,
    val scramble: String,
    val comment: String,
    val penalty: String,
    val sessionId: Long,
    val trainingCategory: String? = null,
    val trainingCaseNumber: Int? = null,
)

data class BackupQuizRecord(
    val level: String,
    val mode: String,
    val bestScore: Int,
    val bestStreak: Int,
    val bestAccuracyPercent: Int,
    val gamesPlayed: Int,
    val updatedAtEpochMillis: Long,
)

data class BackupCustomAlgorithm(
    val id: Long,
    val name: String,
    val notation: String,
    val tags: List<String>,
    val colorScheme: CubeColorScheme,
    val viewpoint: CubeViewpoint,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class BackupSummary(
    val appVersion: String,
    val exportedAtEpochMillis: Long,
    val progressCount: Int,
    val solveTimeCount: Int,
    val timerSessionCount: Int,
    val quizRecordCount: Int,
    val customAlgorithmCount: Int,
    val reminderEnabled: Boolean,
    val cfopAttemptCount: Int = 0,
    val trainingPlanMode: String = "FREE",
    val preferredFormulaCount: Int = 0,
    val dailyGoalsEnabled: Boolean = false,
    val dailyRecognitionTarget: Int = 5,
    val dailyExecutionTarget: Int = 5,
)

class PreparedBackup internal constructor(
    internal val packageData: BackupPackage,
    val summary: BackupSummary,
)

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Invalid(message: String = "O arquivo não é um backup 3x3 válido.", cause: Throwable? = null) :
        BackupException(message, cause)

    class Incompatible(val schemaVersion: Int?) : BackupException(
        if (schemaVersion == null) {
            "O arquivo não informa uma versão de backup compatível."
        } else {
            "A versão $schemaVersion deste backup não é compatível com o aplicativo."
        },
    )
}

internal fun BackupPackage.summary(): BackupSummary = BackupSummary(
    appVersion = appVersion,
    exportedAtEpochMillis = exportedAtEpochMillis,
    progressCount = progress.size,
    solveTimeCount = solveTimes.size,
    timerSessionCount = timerSessions.size,
    quizRecordCount = quizRecords.size,
    customAlgorithmCount = customAlgorithms.size,
    reminderEnabled = reminderSettings.enabled,
    cfopAttemptCount = cfopAttempts.size,
    trainingPlanMode = cfopTrainingPlan.mode.name,
    preferredFormulaCount = preferredFormulas.size,
    dailyGoalsEnabled = dailyGoals.enabled,
    dailyRecognitionTarget = dailyGoals.recognitionTarget,
    dailyExecutionTarget = dailyGoals.executionTarget,
)
