package com.gabs.cubo3x3.data.quiz

import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CfopAttemptRepositoryTest {
    @Test fun restoredAnswerIsSavedOnlyOnce() = runBlocking {
        val dao = FakeCfopDao()
        val repository = CfopAttemptRepository(dao)
        val answer = CfopAttempt("round-0", "PLL", 9, 10, 100L)
        repository.save(answer)
        repository.save(answer)
        repository.save(answer.copy(id = "round-1", selectedCaseNumber = 9))
        assertEquals(listOf(answer, answer.copy(id = "round-1", selectedCaseNumber = 9)),
            repository.attempts.first())
    }

    @Test fun invalidAnswersNeverReachPersistence() = runBlocking {
        val dao = FakeCfopDao()
        val repository = CfopAttemptRepository(dao)
        try {
            repository.save(CfopAttempt("round-0", "PLL", 22, 1, 1))
            fail("Invalid answer saved")
        } catch (_: IllegalArgumentException) { }
        assertTrue(repository.attempts.first().isEmpty())
    }
}

private class FakeCfopDao : CfopAttemptDao {
    val rows = MutableStateFlow<List<CfopAttemptEntity>>(emptyList())
    override fun observeAll(): Flow<List<CfopAttemptEntity>> = rows
    override suspend fun insert(attempt: CfopAttemptEntity) {
        if (rows.value.none { it.id == attempt.id }) rows.value += attempt
    }
}
