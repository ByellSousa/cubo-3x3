package com.gabs.cubo3x3.data.quiz

import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode

data class QuizRecord(
    val level: QuizLevel,
    val mode: QuizMode,
    val bestScore: Int,
    val bestStreak: Int,
    val bestAccuracyPercent: Int,
    val gamesPlayed: Int,
) {
    val key: String
        get() = key(level, mode)

    companion object {
        fun key(level: QuizLevel, mode: QuizMode): String = "${level.name}-${mode.name}"
    }
}
