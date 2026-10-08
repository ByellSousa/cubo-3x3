package com.gabs.cubo3x3.data.formula

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabs.cubo3x3.data.progress.AlgorithmProgressEntity
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.quiz.CfopAttemptEntity
import com.gabs.cubo3x3.ui.AlgorithmCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreferredFormulaRepositoryTest {
    private lateinit var db: CuboDatabase
    private lateinit var repository: PreferredFormulaRepository
    @Before fun create() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<android.content.Context>(),
            CuboDatabase::class.java).build()
        repository = PreferredFormulaRepository(db.preferredFormulaDao(), now = { 900L })
    }
    @After fun close() { db.close() }

    @Test fun preferenceNormalizesAndReplacesOnlyItsCase() = runBlocking {
        repository.change(AlgorithmCategory.F2L, 1, " R U R' ")
        assertEquals("R U R'", repository.formulas.first().getValue("F2L-1").notation)
        repository.change(AlgorithmCategory.F2L, 1, "R U R' U2'")
        val rows = repository.formulas.first()
        assertEquals(1, rows.size)
        assertEquals("R U R' U2'", rows.getValue("F2L-1").notation)
        assertEquals(900L, rows.getValue("F2L-1").updatedAtEpochMillis)
    }

    @Test fun wrongAlternativeNeverReplacesExistingChoice() = runBlocking {
        repository.change(AlgorithmCategory.F2L, 1, "R U R'")
        try { repository.change(AlgorithmCategory.F2L, 1, "U"); fail("Deveria recusar") }
        catch (_: IllegalArgumentException) { }
        assertEquals("R U R'", repository.formulas.first().getValue("F2L-1").notation)
    }

    @Test fun removalDoesNotChangeMarksOrRecognitionHistory() = runBlocking {
        val mark = AlgorithmProgressEntity("F2L", 1, true, true, 100)
        val answer = CfopAttemptEntity("before", "F2L", 1, 2, 500)
        db.backupDao().insertProgress(listOf(mark))
        db.backupDao().insertCfopAttempts(listOf(answer))
        repository.change(AlgorithmCategory.F2L, 1, "R U R'")
        repository.change(AlgorithmCategory.F2L, 1, null)
        assertTrue(repository.formulas.first().isEmpty())
        assertEquals(listOf(mark), db.backupDao().readProgress())
        assertEquals(listOf(answer), db.backupDao().readCfopAttempts())
    }
}
