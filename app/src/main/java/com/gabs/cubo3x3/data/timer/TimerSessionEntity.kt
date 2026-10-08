package com.gabs.cubo3x3.data.timer

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_sessions")
data class TimerSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
) {
    fun toTimerSession() = TimerSession(id, name, createdAtEpochMillis)
}
