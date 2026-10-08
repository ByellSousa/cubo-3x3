package com.gabs.cubo3x3.data.quiz

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "cfop_attempts")
data class CfopAttemptEntity(
    @PrimaryKey val id: String,
    val category: String,
    @ColumnInfo(name = "case_number") val caseNumber: Int,
    @ColumnInfo(name = "selected_case_number") val selectedCaseNumber: Int,
    @ColumnInfo(name = "recorded_at_epoch_millis") val recordedAtEpochMillis: Long,
) {
    fun toAttempt() = CfopAttempt(id, category, caseNumber, selectedCaseNumber, recordedAtEpochMillis)
}

@Dao
interface CfopAttemptDao {
    @Query("SELECT * FROM cfop_attempts ORDER BY recorded_at_epoch_millis, id")
    fun observeAll(): Flow<List<CfopAttemptEntity>>

    // A restored question can retry the same write without counting the answer twice.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(attempt: CfopAttemptEntity)
}

class CfopAttemptRepository(private val dao: CfopAttemptDao) {
    val attempts: Flow<List<CfopAttempt>> = dao.observeAll().map { rows -> rows.map { it.toAttempt() } }

    suspend fun save(attempt: CfopAttempt) {
        attempt.validate()
        dao.insert(CfopAttemptEntity(
            attempt.id, attempt.category, attempt.caseNumber,
            attempt.selectedCaseNumber, attempt.recordedAtEpochMillis,
        ))
    }
}
