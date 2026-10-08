package com.gabs.cubo3x3.data.timer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSessionRepositoryTest {
    @Test
    fun createsRenamesAndRejectsDuplicateNames() = runBlocking {
        val dao = FakeTimerSessionDao()
        val repository = TimerSessionRepository(dao, now = { 123L })
        repository.ensureDefaultSession()

        val id = repository.create("Treino PLL")
        repository.rename(id, "Treino rápido")

        assertEquals(listOf("Principal", "Treino rápido"), repository.sessions.first().map { it.name })
        assertTrue(runCatching { repository.create("treino RÁPIDO") }.isFailure)
    }

    @Test
    fun deletingSessionMovesItsSolvesToDefault() = runBlocking {
        val dao = FakeTimerSessionDao()
        val repository = TimerSessionRepository(dao)
        repository.ensureDefaultSession()
        val id = repository.create("OLL")
        dao.solveSessionIds += id

        repository.deleteAndKeepSolves(id)

        assertEquals(listOf(DEFAULT_TIMER_SESSION_ID), dao.solveSessionIds)
        assertEquals(listOf(DEFAULT_TIMER_SESSION_ID), repository.sessions.first().map { it.id })
    }
}

private class FakeTimerSessionDao : TimerSessionDao {
    private val rows = MutableStateFlow<List<TimerSessionEntity>>(emptyList())
    private var nextId = 2L
    val solveSessionIds = mutableListOf<Long>()

    override fun observeAll(): Flow<List<TimerSessionEntity>> = rows
    override suspend fun readAll(): List<TimerSessionEntity> = rows.value

    override suspend fun insert(session: TimerSessionEntity): Long {
        val id = if (session.id == 0L) nextId++ else session.id
        rows.value = rows.value + session.copy(id = id)
        return id
    }

    override suspend fun insertIfMissing(session: TimerSessionEntity): Long {
        if (rows.value.none { it.id == session.id }) rows.value = rows.value + session
        return session.id
    }

    override suspend fun rename(id: Long, name: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun moveSolves(sourceId: Long, targetId: Long) {
        solveSessionIds.replaceAll { if (it == sourceId) targetId else it }
    }

    override suspend fun deleteRow(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
