package com.gabs.cubo3x3.data.backup

import androidx.room.withTransaction
import com.gabs.cubo3x3.data.custom.CustomAlgorithmEntity
import com.gabs.cubo3x3.data.custom.CustomAlgorithmEntity.Companion.TAG_SEPARATOR
import com.gabs.cubo3x3.data.progress.AlgorithmProgressEntity
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.quiz.QuizRecordEntity
import com.gabs.cubo3x3.data.quiz.CfopAttemptEntity
import com.gabs.cubo3x3.data.formula.PreferredFormulaEntity
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import com.gabs.cubo3x3.preferences.AppPreferences
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRepository(
    private val database: CuboDatabase,
    private val preferences: AppPreferences,
    private val appVersion: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.backupDao()

    suspend fun exportTo(output: OutputStream): BackupSummary {
        val themeMode = preferences.themeMode.first()
        val reminderSettings = preferences.reminderSettings.first()
        val trainingPlan = preferences.cfopTrainingPlan.first()
        val goals = preferences.dailyGoals.first()
        val backup = database.withTransaction {
            BackupPackage(
                appVersion = appVersion,
                exportedAtEpochMillis = now(),
                themeMode = themeMode,
                reminderSettings = reminderSettings,
                cfopTrainingPlan = trainingPlan,
                dailyGoals = goals,
                cfopAttempts = dao.readCfopAttempts().map { it.toAttempt() },
                preferredFormulas = dao.readPreferredFormulas().map { it.toFormula() },
                progress = dao.readProgress().map {
                    BackupProgress(
                        it.category,
                        it.caseNumber,
                        it.isFavorite,
                        it.isCompleted,
                        it.updatedAtEpochMillis,
                    )
                },
                timerSessions = dao.readTimerSessions().map {
                    BackupTimerSession(it.id, it.name, it.createdAtEpochMillis)
                },
                solveTimes = dao.readSolveTimes().map {
                    BackupSolveTime(
                        it.id,
                        it.durationMillis,
                        it.recordedAtEpochMillis,
                        it.scramble,
                        it.comment,
                        it.penalty,
                        it.sessionId,
                        it.trainingCategory,
                        it.trainingCaseNumber,
                    )
                },
                quizRecords = dao.readQuizRecords().map {
                    BackupQuizRecord(
                        it.level,
                        it.mode,
                        it.bestScore,
                        it.bestStreak,
                        it.bestAccuracyPercent,
                        it.gamesPlayed,
                        it.updatedAtEpochMillis,
                    )
                },
                customAlgorithms = dao.readCustomAlgorithms().map { entity ->
                    val value = entity.toCustomAlgorithm()
                    BackupCustomAlgorithm(
                        id = value.id,
                        name = value.name,
                        notation = value.notation,
                        tags = value.tags,
                        colorScheme = value.colorScheme,
                        viewpoint = value.viewpoint,
                        createdAtEpochMillis = value.createdAtEpochMillis,
                        updatedAtEpochMillis = value.updatedAtEpochMillis,
                    )
                },
            )
        }
        val encoded = BackupCodec.encode(backup)
        val bytes = encoded.toByteArray(Charsets.UTF_8)
        if (bytes.size > BackupCodec.MAX_BACKUP_BYTES) {
            throw BackupException.Invalid("Os dados locais excedem o limite de backup de 5 MB.")
        }
        withContext(Dispatchers.Default) { BackupCodec.decode(encoded) }
        withContext(Dispatchers.IO) {
            output.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
        }
        return backup.summary()
    }

    suspend fun inspect(input: InputStream): PreparedBackup {
        val bytes = withContext(Dispatchers.IO) { input.use(::readLimited) }
        val backup = withContext(Dispatchers.Default) { BackupCodec.decode(bytes.toString(Charsets.UTF_8)) }
        return PreparedBackup(backup, backup.summary())
    }

    suspend fun restore(prepared: PreparedBackup) {
        val backup = prepared.packageData
        database.withTransaction {
            dao.clearProgress()
            dao.clearSolveTimes()
            dao.clearTimerSessions()
            dao.clearQuizRecords()
            dao.clearCustomAlgorithms()
            dao.clearCfopAttempts()
            dao.clearPreferredFormulas()
            dao.insertPreferredFormulas(backup.preferredFormulas.map {
                PreferredFormulaEntity(it.category, it.caseNumber, it.notation, it.updatedAtEpochMillis)
            })
            dao.insertCfopAttempts(backup.cfopAttempts.map {
                CfopAttemptEntity(it.id, it.category, it.caseNumber,
                    it.selectedCaseNumber, it.recordedAtEpochMillis)
            })
            dao.insertProgress(
                backup.progress.map {
                    AlgorithmProgressEntity(
                        it.category,
                        it.caseNumber,
                        it.isFavorite,
                        it.isCompleted,
                        it.updatedAtEpochMillis,
                    )
                },
            )
            dao.insertTimerSessions(
                backup.timerSessions.map {
                    TimerSessionEntity(it.id, it.name, it.createdAtEpochMillis)
                },
            )
            dao.insertSolveTimes(
                backup.solveTimes.map {
                    SolveTimeEntity(
                        it.id,
                        it.durationMillis,
                        it.recordedAtEpochMillis,
                        it.scramble,
                        it.comment,
                        it.penalty,
                        it.sessionId,
                        it.trainingCategory,
                        it.trainingCaseNumber,
                    )
                },
            )
            dao.insertQuizRecords(
                backup.quizRecords.map {
                    QuizRecordEntity(
                        it.level,
                        it.mode,
                        it.bestScore,
                        it.bestStreak,
                        it.bestAccuracyPercent,
                        it.gamesPlayed,
                        it.updatedAtEpochMillis,
                    )
                },
            )
            dao.insertCustomAlgorithms(
                backup.customAlgorithms.map {
                    CustomAlgorithmEntity(
                        id = it.id,
                        name = it.name,
                        notation = it.notation,
                        tags = it.tags.joinToString(TAG_SEPARATOR),
                        colorScheme = it.colorScheme.name,
                        viewpoint = it.viewpoint.name,
                        createdAtEpochMillis = it.createdAtEpochMillis,
                        updatedAtEpochMillis = it.updatedAtEpochMillis,
                    )
                },
            )
        }
        preferences.setThemeMode(backup.themeMode)
        preferences.setReminderSettings(backup.reminderSettings)
        preferences.setCfopTrainingPlan(backup.cfopTrainingPlan)
        preferences.setDailyGoals(backup.dailyGoals)
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > BackupCodec.MAX_BACKUP_BYTES) {
                throw BackupException.Invalid("O arquivo excede o limite de 5 MB.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
