package com.gabs.cubo3x3.data.timer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolveTimeRepositoryTest {
    @Test fun goalsObserveDirectedTimesAcrossSessionsWithoutFreeRows() = runBlocking {
        var timestamp = 100L
        val repository = SolveTimeRepository(FakeSolveTimeDao(), now = { timestamp++ })
        repository.add(1_000, "R", sessionId = 1, trainingCategory = "F2L", trainingCaseNumber = 1)
        val directed = repository.add(2_000, "U", sessionId = 2,
            trainingCategory = "PLL", trainingCaseNumber = 21, penalty = SolvePenalty.DNF)
        repository.add(3_000, "F", sessionId = 2)
        assertEquals(listOf(directed, 1L), repository.directedTimes.first().map { it.id })
        assertEquals(3, repository.solveTimes.first().size)
        repository.delete(directed)
        assertEquals(listOf(1L), repository.directedTimes.first().map { it.id })
    }

    @Test
    fun timesAreStoredNewestFirstAndCanBeRemoved() = runBlocking {
        var timestamp = 100L
        val dao = FakeSolveTimeDao()
        val repository = SolveTimeRepository(dao, now = { timestamp++ })

        repository.add(4_321, "R U R'")
        val secondId = repository.add(5_678, "F U F'")

        assertEquals(listOf(5_678L, 4_321L), repository.solveTimes.first().map { it.durationMillis })
        repository.delete(secondId)
        assertEquals(listOf(4_321L), repository.solveTimes.first().map { it.durationMillis })
    }

    @Test
    fun negativeTimesAreRejected() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())

        val result = runCatching { repository.add(-1, "R") }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun detailsAndPenaltyCanBeUpdated() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())
        val id = repository.add(10_000, "R U2 R'")

        repository.updateDetails(id, "Boa execução", SolvePenalty.PLUS_TWO)

        val solve = repository.solveTimes.first().single()
        assertEquals("R U2 R'", solve.scramble)
        assertEquals("Boa execução", solve.comment)
        assertEquals(SolvePenalty.PLUS_TWO, solve.penalty)
    }

    @Test
    fun automaticInspectionPenaltyIsStoredWithSolve() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())

        repository.add(9_870, "F R U R'", SolvePenalty.DNF)

        assertEquals(SolvePenalty.DNF, repository.solveTimes.first().single().penalty)
    }

    @Test
    fun solvesCanBeFilteredAndClearedBySession() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())
        repository.add(10_000, "R", sessionId = 1L)
        repository.add(11_000, "U", sessionId = 2L)

        assertEquals(listOf(11_000L), repository.observeSession(2L).first().map { it.durationMillis })

        repository.clearSession(2L)
        assertEquals(emptyList<SolveTime>(), repository.observeSession(2L).first())
        assertEquals(1, repository.solveTimes.first().size)
    }

    @Test
    fun directedTrainingOriginIsStoredWithFullSetup() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())

        repository.add(
            durationMillis = 8_765,
            scramble = "R U' R'",
            trainingCategory = "F2L",
            trainingCaseNumber = 7,
        )

        val solve = repository.solveTimes.first().single()
        assertEquals("R U' R'", solve.scramble)
        assertEquals("F2L", solve.trainingCategory)
        assertEquals(7, solve.trainingCaseNumber)
    }

    @Test
    fun incompleteTrainingOriginIsRejected() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())

        val result = runCatching {
            repository.add(
                durationMillis = 8_765,
                scramble = "R",
                trainingCategory = "OLL",
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun trainingCaseOutsideCategoryRangeIsRejected() = runBlocking {
        val repository = SolveTimeRepository(FakeSolveTimeDao())

        val result = runCatching {
            repository.add(
                durationMillis = 8_765,
                scramble = "R",
                trainingCategory = "PLL",
                trainingCaseNumber = 22,
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}

private class FakeSolveTimeDao : SolveTimeDao {
    private val rows = MutableStateFlow<List<SolveTimeEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<SolveTimeEntity>> = rows

    override fun observeDirected(): Flow<List<SolveTimeEntity>> =
        rows.map { values -> values.filter { it.trainingCategory != null } }

    override fun observeBySession(sessionId: Long): Flow<List<SolveTimeEntity>> =
        rows.map { values -> values.filter { it.sessionId == sessionId } }

    override suspend fun insert(solveTime: SolveTimeEntity): Long {
        val id = nextId++
        rows.value = (rows.value + solveTime.copy(id = id)).sortedWith(
            compareByDescending<SolveTimeEntity> { it.recordedAtEpochMillis }
                .thenByDescending { it.id },
        )
        return id
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun updateDetails(id: Long, comment: String, penalty: String) {
        rows.value = rows.value.map { row ->
            if (row.id == id) row.copy(comment = comment, penalty = penalty) else row
        }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun clearSession(sessionId: Long) {
        rows.value = rows.value.filterNot { it.sessionId == sessionId }
    }
}
