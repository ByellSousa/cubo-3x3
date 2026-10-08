package com.gabs.cubo3x3.data.transfer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerTransferRepositoryTest {
    private lateinit var database: CuboDatabase
    private lateinit var repository: TimerTransferRepository

    @Before
    fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), CuboDatabase::class.java,
        ).build()
        repository = TimerTransferRepository(database, now = { 1_800_000_000_000L })
        database.timerSessionDao().insertIfMissing(TimerSessionEntity(1L, "Principal", 0L))
        database.solveTimeDao().insert(SolveTimeEntity(durationMillis = 777, recordedAtEpochMillis = 900, sessionId = 1))
        Unit
    }

    @After
    fun close() { database.close() }

    @Test
    fun inspectionDoesNotWriteAndImportCreatesUniqueNewSessionsWithoutChangingOldSolves() = runBlocking {
        val original = database.backupDao().readSolveTimes().single()
        val json = """{"session1":[[[2000,12340],"U2' R","comentário",1700000000]],
          "properties":{"sessionData":{"1":{"name":"principal","scr":"333"}}}}"""
        val preview = repository.inspect(ByteArrayInputStream(json.toByteArray()))
        assertEquals(1, database.timerSessionDao().readAll().size)
        assertEquals(listOf(original), database.backupDao().readSolveTimes())
        val ids = repository.importAsNewSessions(preview)
        assertEquals("principal (2)", database.timerSessionDao().readAll().first { it.id == ids.single() }.name)
        val added = database.backupDao().readSolveTimes().first { it.sessionId == ids.single() }
        assertEquals(12_340L, added.durationMillis)
        assertEquals("PLUS_TWO", added.penalty)
        assertEquals("U2' R", added.scramble)
        assertEquals(1_700_000_012_340L, added.recordedAtEpochMillis)
        assertEquals(original, database.backupDao().readSolveTimes().first { it.id == original.id })
        repository.importAsNewSessions(preview)
        assertTrue(database.timerSessionDao().readAll().any { it.name == "principal (3)" })
    }

    @Test
    fun failedInsertionRollsBackAllImportedSessionsAndTimes() = runBlocking {
        val originalSessions = database.timerSessionDao().readAll()
        val originalSolves = database.backupDao().readSolveTimes()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER transfer_failure BEFORE INSERT ON solve_times " +
                "WHEN NEW.comment = 'force failure' BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
        val preview = TimerTransferPreview(listOf(TransferredSession("Importada", listOf(
            TransferredSolve(1000, 0, "R", "ok", SolvePenalty.NONE),
            TransferredSolve(2000, 0, "U", "force failure", SolvePenalty.DNF),
        ))))
        assertTrue(runCatching { repository.importAsNewSessions(preview) }.isFailure)
        assertEquals(originalSessions, database.timerSessionDao().readAll())
        assertEquals(originalSolves, database.backupDao().readSolveTimes())
    }

    @Test
    fun exportKeepsTheReviewedSnapshotEvenIfLocalDataChanges() = runBlocking {
        val prepared = repository.prepareExport()
        database.solveTimeDao().insert(SolveTimeEntity(durationMillis = 1000, recordedAtEpochMillis = 0, sessionId = 1))
        val output = ByteArrayOutputStream()
        repository.exportTo(prepared, output)
        val decoded = CsTimerCodec.decode(output.toString("UTF-8"))
        assertEquals(1, decoded.solveCount)
        assertEquals(777L, decoded.sessions.single().solves.single().durationMillis)
    }

    @Test
    fun malformedUtf8AndInvalidJsonDoNotChangeLocalData() = runBlocking {
        val original = database.backupDao().readSolveTimes()
        assertTrue(runCatching {
            repository.inspect(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
        }.exceptionOrNull() is TimerTransferException)
        assertTrue(runCatching {
            repository.inspect(ByteArrayInputStream("{\"session1\":null}".toByteArray()))
        }.exceptionOrNull() is TimerTransferException)
        assertEquals(original, database.backupDao().readSolveTimes())
        assertEquals(1, database.timerSessionDao().readAll().size)
    }
}
