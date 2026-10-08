package com.gabs.cubo3x3.data.backup

import androidx.room.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabs.cubo3x3.data.progress.AlgorithmProgressEntity
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.formula.PreferredFormulaEntity
import com.gabs.cubo3x3.data.quiz.CfopAttemptEntity
import com.gabs.cubo3x3.data.quiz.CfopAttemptRepository
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import com.gabs.cubo3x3.domain.reminder.ReminderDay
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.preferences.AppPreferences
import com.gabs.cubo3x3.ui.theme.ThemeMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.ui.AlgorithmCategory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryInstrumentedTest {
    private lateinit var database: CuboDatabase
    private lateinit var preferences: AppPreferences
    private lateinit var repository: BackupRepository
    private lateinit var preferenceFile: File
    private lateinit var preferenceJob: Job

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CuboDatabase::class.java).build()
        // Both Room AND preferences are isolated; never touch the user's real DataStore.
        preferenceFile = File(context.cacheDir, "backup-test-${UUID.randomUUID()}.preferences_pb")
        preferenceJob = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(preferenceJob + Dispatchers.IO), produceFile = { preferenceFile },
        )
        preferences = AppPreferences(context, store)
        repository = BackupRepository(database, preferences, "test", now = { 900L })
    }

    @After
    fun closeDatabase() {
        runBlocking {
            preferenceJob.cancelAndJoin()
            database.close()
            preferenceFile.delete()
        }
    }

    @Test
    fun goalsInspectDoesNotMutatePreferencesAndConfirmedRestorePreservesOtherSettings() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val goals = com.gabs.cubo3x3.domain.training.DailyGoalSettings(true, 2, 3)
        preferences.setDailyGoals(goals)
        preferences.setTimerInspectionEnabled(true)
        val output = ByteArrayOutputStream()
        assertEquals(2, repository.exportTo(output).dailyRecognitionTarget)
        preferences.setDailyGoals(goals.copy(recognitionTarget = 9))
        val prepared = repository.inspect(ByteArrayInputStream(output.toByteArray()))
        assertEquals(9, preferences.dailyGoals.first().recognitionTarget)
        repository.restore(prepared)
        assertEquals(goals, preferences.dailyGoals.first())
        assertEquals(true, preferences.timerInspectionEnabled.first())
        val document = com.google.gson.JsonParser.parseString(output.toString("UTF-8")).asJsonObject
        document.getAsJsonObject("settings").addProperty("dailyGoals", "1|1|0|0")
        try {
            repository.inspect(ByteArrayInputStream(document.toString().toByteArray()))
            fail("Invalid daily goals accepted")
        } catch (_: BackupException.Invalid) {
            assertEquals(goals, preferences.dailyGoals.first())
            assertEquals(1, database.backupDao().readTimerSessions().size)
        }
    }

    @Test
    fun legacyEightRestoreDisablesGoalsOnlyAfterConfirmationAndKeepsHistory() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val attempt = CfopAttemptEntity("goal-answer", "PLL", 21, 1, 500)
        database.backupDao().insertCfopAttempts(listOf(attempt))
        preferences.setDailyGoals(com.gabs.cubo3x3.domain.training.DailyGoalSettings(true, 4, 0))
        val output = ByteArrayOutputStream()
        repository.exportTo(output)
        val document = com.google.gson.JsonParser.parseString(output.toString("UTF-8")).asJsonObject
        document.addProperty("schemaVersion", 8)
        document.getAsJsonObject("settings").remove("dailyGoals")
        val prepared = repository.inspect(ByteArrayInputStream(document.toString().toByteArray()))
        assertEquals(false, prepared.summary.dailyGoalsEnabled)
        assertEquals(true, preferences.dailyGoals.first().enabled)
        repository.restore(prepared)
        assertEquals(com.gabs.cubo3x3.domain.training.DailyGoalSettings(), preferences.dailyGoals.first())
        assertEquals(listOf(attempt), database.backupDao().readCfopAttempts())
    }

    @Test
    fun directedGoalsQueryIncludesAllSessionsButNeverOrdinaryImportedTimes() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0),
            TimerSessionEntity(2, "Outra", 0)))
        database.backupDao().insertSolveTimes(listOf(
            SolveTimeEntity(1, 1_000, 500, trainingCategory = "F2L", trainingCaseNumber = 1),
            SolveTimeEntity(2, 1_000, 600, sessionId = 2, trainingCategory = "OLL", trainingCaseNumber = 57),
            SolveTimeEntity(3, 1_000, 700, sessionId = 2)))
        val times = com.gabs.cubo3x3.data.timer.SolveTimeRepository(database.solveTimeDao()).directedTimes.first()
        assertEquals(listOf(2L, 1L), times.map { it.id })
        assertEquals(3, database.backupDao().readSolveTimes().size)
    }

    @Test
    fun preferredFormulaRoundTripAndInvalidImportNeverMutateCurrentData() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val personal = PreferredFormulaEntity("F2L", 1, "R U R' U2'", 700)
        database.backupDao().insertPreferredFormulas(listOf(personal))
        val output = ByteArrayOutputStream()
        assertEquals(1, repository.exportTo(output).preferredFormulaCount)
        database.backupDao().clearPreferredFormulas()
        val prepared = repository.inspect(ByteArrayInputStream(output.toByteArray()))
        assertEquals(1, prepared.summary.preferredFormulaCount)
        repository.restore(prepared)
        assertEquals(listOf(personal), database.backupDao().readPreferredFormulas())

        val document = com.google.gson.JsonParser.parseString(output.toString("UTF-8")).asJsonObject
        document.getAsJsonArray("preferredFormulas")[0].asJsonObject.addProperty("notation", "U")
        try {
            repository.inspect(ByteArrayInputStream(document.toString().toByteArray()))
            fail("Formula que nao resolve o caso foi aceita")
        } catch (_: BackupException.Invalid) {
            assertEquals(listOf(personal), database.backupDao().readPreferredFormulas())
            assertEquals(1, database.backupDao().readTimerSessions().size)
        }
    }

    @Test
    fun legacyPreviewKeepsCurrentPreferencesAndConfirmedRestoreClearsOnlyAbsentChoices() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val marks = AlgorithmProgressEntity("F2L", 1, true, true, 100)
        val attempt = CfopAttemptEntity("legacy-answer", "F2L", 1, 1, 500)
        database.backupDao().insertProgress(listOf(marks))
        database.backupDao().insertCfopAttempts(listOf(attempt))
        val plan = CfopTrainingPlan(CfopTrainingMode.CASE, AlgorithmCategory.F2L, 1)
        preferences.setCfopTrainingPlan(plan)
        val output = ByteArrayOutputStream()
        repository.exportTo(output)
        val document = com.google.gson.JsonParser.parseString(output.toString("UTF-8")).asJsonObject
        document.addProperty("schemaVersion", 7)
        document.remove("preferredFormulas")
        database.backupDao().insertPreferredFormulas(
            listOf(PreferredFormulaEntity("F2L", 1, "R U R' U2'", 700)),
        )
        val prepared = repository.inspect(ByteArrayInputStream(document.toString().toByteArray()))
        assertEquals(0, prepared.summary.preferredFormulaCount)
        assertEquals(1, database.backupDao().readPreferredFormulas().size)
        repository.restore(prepared)
        assertEquals(0, database.backupDao().readPreferredFormulas().size)
        assertEquals(listOf(marks), database.backupDao().readProgress())
        assertEquals(listOf(attempt), database.backupDao().readCfopAttempts())
        assertEquals(plan, preferences.cfopTrainingPlan.first())
    }

    @Test
    fun cfopAnswersSurviveBackupRestoreAndReplayIsIdempotent() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val cfop = CfopAttemptRepository(database.cfopAttemptDao())
        val answer = CfopAttempt("round-0", "OLL", 1, 2, 800)
        cfop.save(answer)
        cfop.save(answer)
        assertEquals(1, database.backupDao().readCfopAttempts().size)
        val output = ByteArrayOutputStream()
        assertEquals(1, repository.exportTo(output).cfopAttemptCount)
        database.backupDao().clearCfopAttempts()
        repository.restore(repository.inspect(ByteArrayInputStream(output.toByteArray())))
        assertEquals(listOf(answer), cfop.attempts.first())
        cfop.save(answer)
        assertEquals(1, database.backupDao().readCfopAttempts().size)
    }

    @Test
    fun trainingPlanPersistenceAndBackupPreserveOtherPreferences() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        assertEquals(CfopTrainingPlan(), preferences.cfopTrainingPlan.first())
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setTimerInspectionEnabled(true)
        preferences.setTimerInspectionSoundsEnabled(true)
        preferences.setSelectedTimerSessionId(2)
        val plan = CfopTrainingPlan(mode = CfopTrainingMode.COMPLETED,
            completedCategories = setOf(AlgorithmCategory.OLL), extraCaseIds = setOf("OLL-5"))
        preferences.setCfopTrainingPlan(plan)
        assertEquals(plan, preferences.cfopTrainingPlan.first())
        val output = ByteArrayOutputStream()
        repository.exportTo(output)
        preferences.setCfopTrainingPlan(CfopTrainingPlan())
        repository.restore(repository.inspect(ByteArrayInputStream(output.toByteArray())))
        assertEquals(plan, preferences.cfopTrainingPlan.first())
        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        assertEquals(true, preferences.timerInspectionEnabled.first())
        assertEquals(true, preferences.timerInspectionSoundsEnabled.first())
        assertEquals(2L, preferences.selectedTimerSessionId.first())
    }

    @Test
    fun confirmedLegacyRestoreReplacesCfopHistoryWithEmpty() = runBlocking {
        database.backupDao().insertTimerSessions(listOf(TimerSessionEntity(1, "Principal", 0)))
        val output = ByteArrayOutputStream()
        repository.exportTo(output)
        val document = com.google.gson.JsonParser.parseString(output.toString("UTF-8")).asJsonObject
        document.addProperty("schemaVersion", 5)
        document.remove("cfopAttempts")
        document.getAsJsonObject("settings").remove("cfopTrainingPlan")
        preferences.setCfopTrainingPlan(
            CfopTrainingPlan(CfopTrainingMode.CASE, AlgorithmCategory.F2L, 1),
        )
        database.backupDao().insertCfopAttempts(listOf(CfopAttemptEntity("later", "F2L", 1, 1, 850)))
        val prepared = repository.inspect(ByteArrayInputStream(document.toString().toByteArray()))
        assertEquals(0, prepared.summary.cfopAttemptCount)
        assertEquals(1, database.backupDao().readCfopAttempts().size)
        repository.restore(prepared)
        assertEquals(0, database.backupDao().readCfopAttempts().size)
        assertEquals(CfopTrainingPlan(), preferences.cfopTrainingPlan.first())
    }

    @Test
    fun restoresRoomDataAndThemeFromValidatedBackup() = runBlocking {
        preferences.setThemeMode(ThemeMode.DARK)
        val reminder = ReminderSettings(
            enabled = true,
            hour = 18,
            minute = 45,
            days = setOf(ReminderDay.TUESDAY, ReminderDay.THURSDAY),
        )
        preferences.setReminderSettings(reminder)
        database.backupDao().insertProgress(
            listOf(AlgorithmProgressEntity("F2L", 1, true, false, 100L)),
        )
        database.backupDao().insertSolveTimes(
            listOf(
                SolveTimeEntity(
                    id = 3L,
                    durationMillis = 12_340L,
                    recordedAtEpochMillis = 200L,
                    scramble = "R U R'",
                    comment = "Teste",
                    penalty = "PLUS_TWO",
                    sessionId = 2L,
                    trainingCategory = "OLL",
                    trainingCaseNumber = 3,
                ),
            ),
        )
        database.backupDao().insertTimerSessions(
            listOf(TimerSessionEntity(2L, "PLL", 150L)),
        )
        val output = ByteArrayOutputStream()
        repository.exportTo(output)

        database.backupDao().clearProgress()
        database.backupDao().clearSolveTimes()
        database.backupDao().clearTimerSessions()
        preferences.setThemeMode(ThemeMode.LIGHT)
        preferences.setReminderSettings(ReminderSettings())

        val prepared = repository.inspect(ByteArrayInputStream(output.toByteArray()))
        repository.restore(prepared)

        assertEquals(1, database.backupDao().readProgress().size)
        assertEquals(12_340L, database.backupDao().readSolveTimes().single().durationMillis)
        assertEquals("R U R'", database.backupDao().readSolveTimes().single().scramble)
        assertEquals("Teste", database.backupDao().readSolveTimes().single().comment)
        assertEquals("PLUS_TWO", database.backupDao().readSolveTimes().single().penalty)
        assertEquals(2L, database.backupDao().readSolveTimes().single().sessionId)
        assertEquals("OLL", database.backupDao().readSolveTimes().single().trainingCategory)
        assertEquals(3, database.backupDao().readSolveTimes().single().trainingCaseNumber)
        assertEquals("PLL", database.backupDao().readTimerSessions().single().name)
        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        assertEquals(reminder, preferences.reminderSettings.first())
    }
}
