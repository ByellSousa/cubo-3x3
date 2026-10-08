package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.cube.MoveNotation
import kotlin.math.roundToInt
import kotlin.random.Random

enum class QuizLevel(val label: String) {
    ESSENTIAL("Essencial"),
    INTERMEDIATE("Intermediário"),
    ADVANCED("Avançado"),
}

enum class QuizMode(val label: String) {
    PRACTICE("Prática"),
    TIMED("60 segundos"),
}

data class QuizQuestion(
    val correctToken: String,
    val options: List<String>,
) {
    init {
        require(options.size == 4)
        require(options.distinct().size == options.size)
        require(correctToken in options)
    }
}

object QuizQuestionFactory {
    fun tokenPool(level: QuizLevel): List<String> {
        val roots = when (level) {
            QuizLevel.ESSENTIAL -> MoveNotation.roots.take(6)
            QuizLevel.INTERMEDIATE -> MoveNotation.roots.take(12)
            QuizLevel.ADVANCED -> MoveNotation.roots
        }
        return roots.flatMap { root -> listOf(root, "$root'", "${root}2") }
    }

    fun create(level: QuizLevel, questionIndex: Int): QuizQuestion {
        require(questionIndex >= 0)
        val pool = tokenPool(level)
        val correct = pool[questionIndex % pool.size]
        val root = correct.removeSuffix("'").removeSuffix("2")
        val sameRoot = pool.filter { token ->
            token != correct && token.removeSuffix("'").removeSuffix("2") == root
        }
        val random = Random(level.ordinal * 100_003 + questionIndex)
        val others = pool.filter { it != correct && it !in sameRoot }.shuffled(random)
        val options = (sameRoot + others).take(3).plus(correct).shuffled(random)
        return QuizQuestion(correctToken = correct, options = options)
    }
}

data class QuizScore(
    val answered: Int = 0,
    val correct: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val points: Int = 0,
) {
    val accuracyPercent: Int
        get() = if (answered == 0) 0 else ((correct * 100.0) / answered).roundToInt()

    fun answer(isCorrect: Boolean): QuizScore {
        if (!isCorrect) {
            return copy(answered = answered + 1, currentStreak = 0)
        }
        val nextStreak = currentStreak + 1
        return copy(
            answered = answered + 1,
            correct = correct + 1,
            currentStreak = nextStreak,
            bestStreak = maxOf(bestStreak, nextStreak),
            points = points + 100 + (currentStreak * 10),
        )
    }
}

class QuizTimer(
    private val elapsedRealtime: () -> Long,
    private val durationMillis: Long = 60_000L,
) {
    init {
        require(durationMillis > 0L)
    }

    fun start(): Long = elapsedRealtime()

    fun remainingMillis(startedAtRealtimeMillis: Long): Long =
        (durationMillis -
            (elapsedRealtime() - startedAtRealtimeMillis).coerceAtLeast(0L))
            .coerceAtLeast(0L)

    fun isExpired(startedAtRealtimeMillis: Long): Boolean =
        remainingMillis(startedAtRealtimeMillis) == 0L
}
