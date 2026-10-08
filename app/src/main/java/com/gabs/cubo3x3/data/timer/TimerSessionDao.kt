package com.gabs.cubo3x3.data.timer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDao {
    @Query("SELECT * FROM timer_sessions ORDER BY created_at_epoch_millis, id")
    fun observeAll(): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM timer_sessions ORDER BY created_at_epoch_millis, id")
    suspend fun readAll(): List<TimerSessionEntity>

    @Insert
    suspend fun insert(session: TimerSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(session: TimerSessionEntity): Long

    @Query("UPDATE timer_sessions SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE solve_times SET session_id = :targetId WHERE session_id = :sourceId")
    suspend fun moveSolves(sourceId: Long, targetId: Long)

    @Query("DELETE FROM timer_sessions WHERE id = :id")
    suspend fun deleteRow(id: Long)

    @Transaction
    suspend fun moveSolvesAndDelete(sourceId: Long, targetId: Long) {
        moveSolves(sourceId, targetId)
        deleteRow(sourceId)
    }
}
