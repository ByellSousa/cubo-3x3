package com.gabs.cubo3x3.data.progress

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "algorithm_progress",
    primaryKeys = ["category", "case_number"],
)
data class AlgorithmProgressEntity(
    val category: String,
    @ColumnInfo(name = "case_number") val caseNumber: Int,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
) {
    val key: String
        get() = "$category-$caseNumber"

    fun toProgress(): AlgorithmProgress = AlgorithmProgress(
        isFavorite = isFavorite,
        isCompleted = isCompleted,
    )
}
