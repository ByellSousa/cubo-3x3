package com.gabs.cubo3x3.data.custom

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint

@Entity(tableName = "custom_algorithms")
data class CustomAlgorithmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val notation: String,
    val tags: String,
    @ColumnInfo(name = "color_scheme") val colorScheme: String,
    val viewpoint: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
) {
    fun toCustomAlgorithm(): CustomAlgorithm = CustomAlgorithm(
        id = id,
        name = name,
        notation = notation,
        tags = tags.split(TAG_SEPARATOR).filter(String::isNotBlank),
        colorScheme = CubeColorScheme.valueOf(colorScheme),
        viewpoint = CubeViewpoint.valueOf(viewpoint),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        const val TAG_SEPARATOR = "\u001F"
    }
}
