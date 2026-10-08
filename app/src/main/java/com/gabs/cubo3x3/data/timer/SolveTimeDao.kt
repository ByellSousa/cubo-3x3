package com.gabs.cubo3x3.data.timer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolveTimeDao {
    @Query("SELECT * FROM solve_times ORDER BY recorded_at_epoch_millis DESC, id DESC")
    fun observeAll(): Flow<List<SolveTimeEntity>>

    @Query("SELECT * FROM solve_times WHERE training_category IS NOT NULL " +
        "ORDER BY recorded_at_epoch_millis DESC, id DESC")
    fun observeDirected(): Flow<List<SolveTimeEntity>>

    @Query(
        "SELECT * FROM solve_times WHERE session_id = :sessionId " +
            "ORDER BY recorded_at_epoch_millis DESC, id DESC",
    )
    fun observeBySession(sessionId: Long): Flow<List<SolveTimeEntity>>

    @Insert
    suspend fun insert(solveTime: SolveTimeEntity): Long

    @Query("DELETE FROM solve_times WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE solve_times SET comment = :comment, penalty = :penalty WHERE id = :id")
    suspend fun updateDetails(id: Long, comment: String, penalty: String)

    @Query("DELETE FROM solve_times")
    suspend fun clear()

    @Query("DELETE FROM solve_times WHERE session_id = :sessionId")
    suspend fun clearSession(sessionId: Long)
}
