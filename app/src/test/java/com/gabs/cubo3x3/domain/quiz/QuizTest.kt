package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.cube.MoveNotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizTest {
    @Test
    fun levelsExposeIncreasingValidMovePools() {
        val essential = QuizQuestionFactory.tokenPool(QuizLevel.ESSENTIAL)
        val intermediate = QuizQuestionFactory.tokenPool(QuizLevel.INTERMEDIATE)
        val advanced = QuizQuestionFactory.tokenPool(QuizLevel.ADVANCED)

        assertEquals(18, essential.size)
        assertEquals(36, intermediate.size)
        assertEquals(54, advanced.size)
        assertTrue(intermediate.containsAll(essential))
        assertTrue(advanced.containsAll(intermediate))
        advanced.forEach { MoveNotation.parseToken(it) }
    }

    @Test
    fun generatedQuestionHasFourUniqueOptionsAndIsDeterministic() {
        val first = QuizQuestionFactory.create(QuizLevel.ADVANCED, 47)
        val second = QuizQuestionFactory.create(QuizLevel.ADVANCED, 47)

        assertEquals(first, second)
        assertEquals(4, first.options.distinct().size)
        assertTrue(first.correctToken in first.options)
    }

    @Test
    fun scoreTracksPointsAccuracyAndStreaks() {
        val score = QuizScore()
            .answer(true)
            .answer(true)
            .answer(false)
            .answer(true)

        assertEquals(4, score.answered)
        assertEquals(3, score.correct)
        assertEquals(75, score.accuracyPercent)
        assertEquals(1, score.currentStreak)
        assertEquals(2, score.bestStreak)
        assertEquals(310, score.points)
    }

    @Test
    fun timerUsesMonotonicElapsedTimeAndExpiresAtLimit() {
        var now = 10_000L
        val timer = QuizTimer(elapsedRealtime = { now }, durationMillis = 1_000L)
        val startedAt = timer.start()

        now = 10_999L
        assertEquals(1L, timer.remainingMillis(startedAt))
        assertFalse(timer.isExpired(startedAt))

        now = 11_000L
        assertEquals(0L, timer.remainingMillis(startedAt))
        assertTrue(timer.isExpired(startedAt))
    }

    @Test
    fun timerDoesNotGainTimeWhenClockMovesBackwards() {
        var now = 5_000L
        val timer = QuizTimer(elapsedRealtime = { now }, durationMillis = 2_000L)
        val startedAt = timer.start()

        now = 4_000L
        assertEquals(2_000L, timer.remainingMillis(startedAt))
    }
}
