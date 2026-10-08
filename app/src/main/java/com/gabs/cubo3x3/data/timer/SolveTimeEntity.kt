package com.gabs.cubo3x3.data.timer

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "solve_times",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["training_category", "training_case_number"]),
    ],
)
data class SolveTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "recorded_at_epoch_millis") val recordedAtEpochMillis: Long,
    @ColumnInfo(name = "scramble") val scramble: String = "",
    @ColumnInfo(name = "comment") val comment: String = "",
    @ColumnInfo(name = "penalty") val penalty: String = SolvePenalty.NONE.name,
    @ColumnInfo(name = "session_id") val sessionId: Long = DEFAULT_TIMER_SESSION_ID,
    @ColumnInfo(name = "training_category") val trainingCategory: String? = null,
    @ColumnInfo(name = "training_case_number") val trainingCaseNumber: Int? = null,
) {
    fun toSolveTime(): SolveTime = SolveTime(
        id = id,
        durationMillis = durationMillis,
        recordedAtEpochMillis = recordedAtEpochMillis,
        scramble = scramble,
        comment = comment,
        penalty = SolvePenalty.entries.firstOrNull { it.name == penalty } ?: SolvePenalty.NONE,
        sessionId = sessionId,
        trainingCategory = trainingCategory,
        trainingCaseNumber = trainingCaseNumber,
    )
}
