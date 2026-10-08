package com.gabs.cubo3x3.data.progress

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlgorithmProgressDao {
    @Query("SELECT * FROM algorithm_progress ORDER BY category, case_number")
    fun observeAll(): Flow<List<AlgorithmProgressEntity>>

    @Query(
        "SELECT * FROM algorithm_progress " +
            "WHERE category = :category AND case_number = :caseNumber LIMIT 1",
    )
    suspend fun find(category: String, caseNumber: Int): AlgorithmProgressEntity?

    @Upsert
    suspend fun upsert(progress: AlgorithmProgressEntity)

    @Query(
        "DELETE FROM algorithm_progress " +
            "WHERE category = :category AND case_number = :caseNumber",
    )
    suspend fun delete(category: String, caseNumber: Int)

    @Query("DELETE FROM algorithm_progress")
    suspend fun clear()
}
