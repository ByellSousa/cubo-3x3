package com.gabs.cubo3x3.data.timer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerSessionRepository(
    private val dao: TimerSessionDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val sessions: Flow<List<TimerSession>> = dao.observeAll().map { rows ->
        rows.map(TimerSessionEntity::toTimerSession)
    }

    suspend fun ensureDefaultSession() {
        dao.insertIfMissing(
            TimerSessionEntity(
                id = DEFAULT_TIMER_SESSION_ID,
                name = DEFAULT_TIMER_SESSION_NAME,
                createdAtEpochMillis = 0L,
            ),
        )
    }

    suspend fun create(name: String): Long {
        val normalized = validatedName(name)
        requireNameIsUnique(normalized)
        return dao.insert(
            TimerSessionEntity(name = normalized, createdAtEpochMillis = now()),
        )
    }

    suspend fun rename(id: Long, name: String) {
        require(id > 0) { "Session id must be positive" }
        val normalized = validatedName(name)
        requireNameIsUnique(normalized, exceptId = id)
        dao.rename(id, normalized)
    }

    suspend fun deleteAndKeepSolves(id: Long) {
        require(id != DEFAULT_TIMER_SESSION_ID) { "The default session cannot be deleted" }
        require(id > 0) { "Session id must be positive" }
        ensureDefaultSession()
        dao.moveSolvesAndDelete(id, DEFAULT_TIMER_SESSION_ID)
    }

    private suspend fun requireNameIsUnique(name: String, exceptId: Long? = null) {
        require(
            dao.readAll().none { row ->
                row.id != exceptId && row.name.equals(name, ignoreCase = true)
            },
        ) { "Session name already exists" }
    }

    private fun validatedName(name: String): String {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Session name cannot be empty" }
        require(normalized.length <= MAX_NAME_LENGTH) { "Session name is too long" }
        return normalized
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
