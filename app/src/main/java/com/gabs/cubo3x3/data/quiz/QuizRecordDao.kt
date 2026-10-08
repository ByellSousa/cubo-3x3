package com.gabs.cubo3x3.data.quiz

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizRecordDao {
    @Query("SELECT * FROM quiz_records ORDER BY level, mode")
    fun observeAll(): Flow<List<QuizRecordEntity>>

    @Query("SELECT * FROM quiz_records WHERE level = :level AND mode = :mode LIMIT 1")
    suspend fun find(level: String, mode: String): QuizRecordEntity?

    @Upsert
    suspend fun upsert(record: QuizRecordEntity)

    @Query("DELETE FROM quiz_records")
    suspend fun clear()
}
