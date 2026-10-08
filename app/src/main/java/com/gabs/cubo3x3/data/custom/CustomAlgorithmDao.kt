package com.gabs.cubo3x3.data.custom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlgorithmDao {
    @Query("SELECT * FROM custom_algorithms ORDER BY updated_at_epoch_millis DESC, id DESC")
    fun observeAll(): Flow<List<CustomAlgorithmEntity>>

    @Query("SELECT * FROM custom_algorithms WHERE id = :id LIMIT 1")
    suspend fun find(id: Long): CustomAlgorithmEntity?

    @Insert
    suspend fun insert(algorithm: CustomAlgorithmEntity): Long

    @Update
    suspend fun update(algorithm: CustomAlgorithmEntity)

    @Query("DELETE FROM custom_algorithms WHERE id = :id")
    suspend fun delete(id: Long)
}
