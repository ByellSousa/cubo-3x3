package com.gabs.cubo3x3.data.quiz

import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.QuizScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuizRecordRepository(
    private val dao: QuizRecordDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutationMutex = Mutex()

    val records: Flow<Map<String, QuizRecord>> = dao.observeAll().map { entities ->
        entities.associate { entity ->
            val record = entity.toQuizRecord()
            record.key to record
        }
    }

    suspend fun saveResult(level: QuizLevel, mode: QuizMode, score: QuizScore) =
        mutationMutex.withLock {
            require(score.answered >= score.correct)
            val current = dao.find(level.name, mode.name)
            dao.upsert(
                QuizRecordEntity(
                    level = level.name,
                    mode = mode.name,
                    bestScore = maxOf(current?.bestScore ?: 0, score.points),
                    bestStreak = maxOf(current?.bestStreak ?: 0, score.bestStreak),
                    bestAccuracyPercent = maxOf(
                        current?.bestAccuracyPercent ?: 0,
                        score.accuracyPercent,
                    ),
                    gamesPlayed = (current?.gamesPlayed ?: 0) + 1,
                    updatedAtEpochMillis = now(),
                ),
            )
        }

    suspend fun clear() = mutationMutex.withLock { dao.clear() }
}
