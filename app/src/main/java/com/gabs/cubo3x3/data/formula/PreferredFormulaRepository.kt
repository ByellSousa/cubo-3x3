package com.gabs.cubo3x3.data.formula

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Entity(tableName = "preferred_case_formulas", primaryKeys = ["category", "case_number"])
data class PreferredFormulaEntity(
    val category: String,
    @ColumnInfo(name = "case_number") val caseNumber: Int,
    val notation: String,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
) {
    fun toFormula() = PreferredCaseFormula(category, caseNumber, notation, updatedAtEpochMillis)
}

@Dao
interface PreferredFormulaDao {
    @Query("SELECT * FROM preferred_case_formulas ORDER BY category, case_number")
    fun observeAll(): Flow<List<PreferredFormulaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(formula: PreferredFormulaEntity)

    @Query("DELETE FROM preferred_case_formulas WHERE category = :category AND case_number = :number")
    suspend fun delete(category: String, number: Int)
}

class PreferredFormulaRepository(
    private val dao: PreferredFormulaDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val formulas: Flow<Map<String, PreferredCaseFormula>> = dao.observeAll().map { rows ->
        rows.map { it.toFormula() }.associateBy { it.caseId }
    }

    suspend fun change(category: AlgorithmCategory, number: Int, notation: String?) {
        AlgorithmCatalog.entry(category, number)
        if (notation == null) {
            dao.delete(category.name, number)
        } else {
            val valid = withContext(Dispatchers.Default) {
                PreferredCaseFormula(category.name, number, notation, now()).validated()
            }
            dao.put(PreferredFormulaEntity(valid.category, valid.caseNumber,
                valid.notation, valid.updatedAtEpochMillis))
        }
    }
}
