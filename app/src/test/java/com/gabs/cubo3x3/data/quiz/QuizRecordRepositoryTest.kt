package com.gabs.cubo3x3.data.quiz

import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.QuizScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizRecordRepositoryTest {
    @Test
    fun savesBestValuesAndCountsEveryFinishedGame() = runBlocking {
        val dao = FakeQuizRecordDao()
        val repository = QuizRecordRepository(dao, now = { 99L })
        val key = QuizRecord.key(QuizLevel.ESSENTIAL, QuizMode.PRACTICE)

        repository.saveResult(
            QuizLevel.ESSENTIAL,
            QuizMode.PRACTICE,
            QuizScore(answered = 4, correct = 3, bestStreak = 3, points = 330),
        )
        repository.saveResult(
            QuizLevel.ESSENTIAL,
            QuizMode.PRACTICE,
            QuizScore(answered = 2, correct = 2, bestStreak = 2, points = 210),
        )

        val saved = repository.records.first().getValue(key)
        assertEquals(330, saved.bestScore)
        assertEquals(3, saved.bestStreak)
        assertEquals(100, saved.bestAccuracyPercent)
        assertEquals(2, saved.gamesPlayed)
        assertEquals(99L, dao.find(QuizLevel.ESSENTIAL.name, QuizMode.PRACTICE.name)?.updatedAtEpochMillis)
    }

    @Test
    fun clearRemovesAllQuizRecords() = runBlocking {
        val dao = FakeQuizRecordDao()
        val repository = QuizRecordRepository(dao)
        repository.saveResult(
            QuizLevel.ADVANCED,
            QuizMode.TIMED,
            QuizScore(answered = 1, correct = 1, bestStreak = 1, points = 100),
        )

        repository.clear()

        assertTrue(repository.records.first().isEmpty())
    }
}

private class FakeQuizRecordDao : QuizRecordDao {
    private val rows = MutableStateFlow<List<QuizRecordEntity>>(emptyList())

    override fun observeAll(): Flow<List<QuizRecordEntity>> = rows

    override suspend fun find(level: String, mode: String): QuizRecordEntity? =
        rows.value.singleOrNull { it.level == level && it.mode == mode }

    override suspend fun upsert(record: QuizRecordEntity) {
        rows.value = rows.value.filterNot {
            it.level == record.level && it.mode == record.mode
        } + record
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}
