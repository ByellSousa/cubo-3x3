package com.gabs.cubo3x3.data.timer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SolveTimeRepository(
    private val dao: SolveTimeDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val solveTimes: Flow<List<SolveTime>> = dao.observeAll().map { entities ->
        entities.map(SolveTimeEntity::toSolveTime)
    }

    val directedTimes: Flow<List<SolveTime>> = dao.observeDirected().map { entities ->
        entities.map(SolveTimeEntity::toSolveTime)
    }

    fun observeSession(sessionId: Long): Flow<List<SolveTime>> =
        dao.observeBySession(sessionId).map { entities ->
            entities.map(SolveTimeEntity::toSolveTime)
        }

    suspend fun add(
        durationMillis: Long,
        scramble: String,
        penalty: SolvePenalty = SolvePenalty.NONE,
        sessionId: Long = DEFAULT_TIMER_SESSION_ID,
        trainingCategory: String? = null,
        trainingCaseNumber: Int? = null,
    ): Long {
        require(durationMillis >= 0) { "Duration cannot be negative" }
        require(scramble.length <= MAX_SCRAMBLE_LENGTH) { "Scramble is too long" }
        require(sessionId > 0) { "Session id must be positive" }
        require((trainingCategory == null) == (trainingCaseNumber == null)) {
            "Training category and case must be provided together"
        }
        if (trainingCategory != null) {
            val maximumCase = TRAINING_CATEGORY_LIMITS[trainingCategory]
            require(maximumCase != null) { "Unknown training category" }
            require(trainingCaseNumber != null && trainingCaseNumber in 1..maximumCase) {
                "Training case is outside the category range"
            }
        }
        return dao.insert(
            SolveTimeEntity(
                durationMillis = durationMillis,
                recordedAtEpochMillis = now(),
                scramble = scramble.trim(),
                penalty = penalty.name,
                sessionId = sessionId,
                trainingCategory = trainingCategory,
                trainingCaseNumber = trainingCaseNumber,
            ),
        )
    }

    suspend fun updateDetails(id: Long, comment: String, penalty: SolvePenalty) {
        require(id > 0) { "Solve id must be positive" }
        require(comment.length <= MAX_COMMENT_LENGTH) { "Comment is too long" }
        dao.updateDetails(id, comment.trim(), penalty.name)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()

    suspend fun clearSession(sessionId: Long) {
        require(sessionId > 0) { "Session id must be positive" }
        dao.clearSession(sessionId)
    }

    companion object {
        const val MAX_SCRAMBLE_LENGTH = 500
        const val MAX_COMMENT_LENGTH = 2_000
        private val TRAINING_CATEGORY_LIMITS = mapOf(
            "F2L" to 41,
            "OLL" to 57,
            "PLL" to 21,
        )
    }
}
