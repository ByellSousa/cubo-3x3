package com.gabs.cubo3x3.data.quiz

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode

@Entity(
    tableName = "quiz_records",
    primaryKeys = ["level", "mode"],
)
data class QuizRecordEntity(
    val level: String,
    val mode: String,
    @ColumnInfo(name = "best_score") val bestScore: Int,
    @ColumnInfo(name = "best_streak") val bestStreak: Int,
    @ColumnInfo(name = "best_accuracy_percent") val bestAccuracyPercent: Int,
    @ColumnInfo(name = "games_played") val gamesPlayed: Int,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
) {
    fun toQuizRecord(): QuizRecord = QuizRecord(
        level = QuizLevel.valueOf(level),
        mode = QuizMode.valueOf(mode),
        bestScore = bestScore,
        bestStreak = bestStreak,
        bestAccuracyPercent = bestAccuracyPercent,
        gamesPlayed = gamesPlayed,
    )
}
