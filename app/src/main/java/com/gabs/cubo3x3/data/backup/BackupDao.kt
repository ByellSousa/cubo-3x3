package com.gabs.cubo3x3.data.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gabs.cubo3x3.data.custom.CustomAlgorithmEntity
import com.gabs.cubo3x3.data.progress.AlgorithmProgressEntity
import com.gabs.cubo3x3.data.quiz.QuizRecordEntity
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import com.gabs.cubo3x3.data.quiz.CfopAttemptEntity
import com.gabs.cubo3x3.data.formula.PreferredFormulaEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM preferred_case_formulas ORDER BY category, case_number")
    suspend fun readPreferredFormulas(): List<PreferredFormulaEntity>

    @Query("DELETE FROM preferred_case_formulas")
    suspend fun clearPreferredFormulas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferredFormulas(rows: List<PreferredFormulaEntity>)

    @Query("SELECT * FROM cfop_attempts ORDER BY recorded_at_epoch_millis, id")
    suspend fun readCfopAttempts(): List<CfopAttemptEntity>

    @Query("DELETE FROM cfop_attempts")
    suspend fun clearCfopAttempts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCfopAttempts(rows: List<CfopAttemptEntity>)

    @Query("SELECT * FROM algorithm_progress ORDER BY category, case_number")
    suspend fun readProgress(): List<AlgorithmProgressEntity>

    @Query("SELECT * FROM solve_times ORDER BY recorded_at_epoch_millis DESC, id DESC")
    suspend fun readSolveTimes(): List<SolveTimeEntity>

    @Query("SELECT * FROM timer_sessions ORDER BY created_at_epoch_millis, id")
    suspend fun readTimerSessions(): List<TimerSessionEntity>

    @Query("SELECT * FROM quiz_records ORDER BY level, mode")
    suspend fun readQuizRecords(): List<QuizRecordEntity>

    @Query("SELECT * FROM custom_algorithms ORDER BY id")
    suspend fun readCustomAlgorithms(): List<CustomAlgorithmEntity>

    @Query("DELETE FROM algorithm_progress")
    suspend fun clearProgress()

    @Query("DELETE FROM solve_times")
    suspend fun clearSolveTimes()

    @Query("DELETE FROM timer_sessions")
    suspend fun clearTimerSessions()

    @Query("DELETE FROM quiz_records")
    suspend fun clearQuizRecords()

    @Query("DELETE FROM custom_algorithms")
    suspend fun clearCustomAlgorithms()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(rows: List<AlgorithmProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolveTimes(rows: List<SolveTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimerSessions(rows: List<TimerSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizRecords(rows: List<QuizRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomAlgorithms(rows: List<CustomAlgorithmEntity>)
}
