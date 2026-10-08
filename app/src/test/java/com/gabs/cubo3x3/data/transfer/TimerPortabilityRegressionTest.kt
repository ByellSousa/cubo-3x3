package com.gabs.cubo3x3.data.transfer

import com.gabs.cubo3x3.data.backup.BackupCodec
import com.gabs.cubo3x3.data.backup.BackupPackage
import com.gabs.cubo3x3.data.backup.BackupSolveTime
import com.gabs.cubo3x3.data.backup.BackupTimerSession
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.domain.timer.SolveStatisticsCalculator
import com.gabs.cubo3x3.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerPortabilityRegressionTest {
    @Test
    fun importedRecordsSurviveFullBackupAndReturnToThePublicFormat() {
        val imported = CsTimerCodec.decode("""
            {"session1":[[[0,12001],"R U2' R'","manhã ☀",1700000000],
                [[2000,15002],"F2 U'","+2 manual",1700000100],
                [[-1,18003],"L2' D","DNF",1700000200]],
             "properties":{"sessionData":{"1":{"name":"Treino &amp; café","opt":{"scrType":"333"}}}}}
        """.trimIndent())
        val original = backupOf(imported.sessions)
        val restored = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original, restored)
        val returned = CsTimerCodec.decode(CsTimerCodec.encode(sessionsOf(restored)))
        assertEquals(imported.sessions, returned.sessions)
        assertEquals(listOf("NONE", "PLUS_TWO", "DNF"), restored.solveTimes.map { it.penalty })
    }

    @Test
    fun undatedRecordsRemainUndatedAcrossBothFormats() {
        val imported = CsTimerCodec.decode("""
            {"session1":[[[2000,12345],"U2'","sem data"]],"session2":[],
             "properties":{"sessionData":{"1":{"name":"Histórico"},"2":{"name":"Vazia"}}}}
        """.trimIndent())
        val restored = BackupCodec.decode(BackupCodec.encode(backupOf(imported.sessions)))
        val returned = CsTimerCodec.decode(CsTimerCodec.encode(sessionsOf(restored)))
        assertEquals(0L, restored.solveTimes.single().recordedAtEpochMillis)
        assertEquals(0L, returned.sessions.first().solves.single().recordedAtEpochMillis)
        assertEquals(2, returned.sessions.size)
        assertTrue(returned.sessions.last().solves.isEmpty())
        assertTrue(returned.warnings.any { it.contains("sem data") })
    }

    @Test
    fun penaltyAndAverageResultsRemainStableAfterPublicAndFullBackupRoundTrips() {
        val original = listOf(TransferredSession("Médias", listOf(
            TransferredSolve(10000, 0, "R", "", SolvePenalty.PLUS_TWO),
            TransferredSolve(14000, 0, "U", "", SolvePenalty.NONE),
            TransferredSolve(16000, 0, "F", "", SolvePenalty.NONE),
            TransferredSolve(20000, 0, "D", "", SolvePenalty.NONE),
            TransferredSolve(1000, 0, "L", "", SolvePenalty.DNF),
        )))
        val imported = CsTimerCodec.decode(CsTimerCodec.encode(original))
        val restored = BackupCodec.decode(BackupCodec.encode(backupOf(imported.sessions)))
        val solves = restored.solveTimes.reversed().map {
            SolveTimeEntity(
                id = it.id, durationMillis = it.durationMillis, recordedAtEpochMillis = it.recordedAtEpochMillis,
                penalty = it.penalty, sessionId = it.sessionId,
            ).toSolveTime()
        }
        val statistics = SolveStatisticsCalculator.calculate(solves)
        assertEquals(10000L, restored.solveTimes.first().durationMillis)
        assertEquals(12000L, statistics.bestTime.durationMillis)
        assertEquals(16666L, statistics.currentAo5.durationMillis)
        assertTrue(statistics.currentMo3.isDnf)
        assertTrue(statistics.currentTime.isDnf)
    }

    private fun backupOf(sessions: List<TransferredSession>): BackupPackage {
        var solveId = 1L
        return BackupPackage(
            appVersion = "1.0.0-rc13", exportedAtEpochMillis = 1800000000000,
            themeMode = ThemeMode.DARK, reminderSettings = ReminderSettings(), progress = emptyList(),
            timerSessions = sessions.mapIndexed { index, session -> BackupTimerSession(index + 1L, session.name, 0) },
            solveTimes = sessions.flatMapIndexed { index, session -> session.solves.map { solve ->
                BackupSolveTime(solveId++, solve.durationMillis, solve.recordedAtEpochMillis,
                    solve.scramble, solve.comment, solve.penalty.name, index + 1L)
            } },
            quizRecords = emptyList(), customAlgorithms = emptyList(),
        )
    }

    private fun sessionsOf(backup: BackupPackage) = backup.timerSessions.map { session ->
        TransferredSession(session.name, backup.solveTimes.filter { it.sessionId == session.id }.map { solve ->
            TransferredSolve(solve.durationMillis, solve.recordedAtEpochMillis,
                solve.scramble, solve.comment, SolvePenalty.valueOf(solve.penalty))
        })
    }
}
