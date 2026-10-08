package com.gabs.cubo3x3.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmProgressRepositoryTest {
    @Test
    fun favoriteAndCompletedAreCombinedAndPersisted() = runBlocking {
        val dao = FakeAlgorithmProgressDao()
        val repository = AlgorithmProgressRepository(dao, now = { 1234L })

        repository.setFavorite("OLL", 7, true)
        repository.setCompleted("OLL", 7, true)

        val saved = repository.progress.first().getValue("OLL-7")
        assertTrue(saved.isFavorite)
        assertTrue(saved.isCompleted)
        assertEquals(1234L, dao.find("OLL", 7)?.updatedAtEpochMillis)
    }

    @Test
    fun clearingBothFlagsRemovesTheEmptyDatabaseRow() = runBlocking {
        val dao = FakeAlgorithmProgressDao()
        val repository = AlgorithmProgressRepository(dao)

        repository.setFavorite("PLL", 3, true)
        repository.setFavorite("PLL", 3, false)

        assertFalse(repository.progress.first().containsKey("PLL-3"))
        assertEquals(null, dao.find("PLL", 3))
    }

    @Test
    fun clearRemovesEverySavedMarking() = runBlocking {
        val dao = FakeAlgorithmProgressDao()
        val repository = AlgorithmProgressRepository(dao)

        repository.setFavorite("F2L", 1, true)
        repository.setCompleted("OLL", 2, true)
        repository.setFavorite("PLL", 3, true)
        repository.clear()

        assertTrue(repository.progress.first().isEmpty())
    }
}

private class FakeAlgorithmProgressDao : AlgorithmProgressDao {
    private val rows = MutableStateFlow<List<AlgorithmProgressEntity>>(emptyList())

    override fun observeAll(): Flow<List<AlgorithmProgressEntity>> = rows

    override suspend fun find(category: String, caseNumber: Int): AlgorithmProgressEntity? =
        rows.value.singleOrNull { it.category == category && it.caseNumber == caseNumber }

    override suspend fun upsert(progress: AlgorithmProgressEntity) {
        rows.value = rows.value
            .filterNot {
                it.category == progress.category && it.caseNumber == progress.caseNumber
            } + progress
    }

    override suspend fun delete(category: String, caseNumber: Int) {
        rows.value = rows.value.filterNot {
            it.category == category && it.caseNumber == caseNumber
        }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}
