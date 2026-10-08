package com.gabs.cubo3x3.data.custom

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomAlgorithmRepositoryTest {
    @Test
    fun createNormalizesInputAndPersistsPreviewSettings() = runBlocking {
        val repository = CustomAlgorithmRepository(FakeCustomAlgorithmDao(), now = { 10L })

        repository.create(
            name = "  Sexy move  ",
            notation = "R   U R' U'",
            tags = listOf("Treino", " treino ", "Rápido"),
            colorScheme = CubeColorScheme.HIGH_CONTRAST,
            viewpoint = CubeViewpoint.RIGHT,
        )

        val saved = repository.algorithms.first().single()
        assertEquals("Sexy move", saved.name)
        assertEquals("R U R' U'", saved.notation)
        assertEquals(listOf("Treino", "Rápido"), saved.tags)
        assertEquals(CubeColorScheme.HIGH_CONTRAST, saved.colorScheme)
        assertEquals(CubeViewpoint.RIGHT, saved.viewpoint)
    }

    @Test
    fun updateDuplicateAndDeleteKeepIndependentRows() = runBlocking {
        val dao = FakeCustomAlgorithmDao()
        var now = 1L
        val repository = CustomAlgorithmRepository(dao, now = { now++ })
        val originalId = repository.create(
            "Caso", "U2'", emptyList(), CubeColorScheme.STANDARD, CubeViewpoint.FRONT,
        )

        repository.update(
            originalId, "Caso editado", "U2' R", listOf("OLL"),
            CubeColorScheme.STANDARD, CubeViewpoint.LEFT,
        )
        val copyId = repository.duplicate(originalId)
        repository.delete(originalId)

        val remaining = repository.algorithms.first().single()
        assertEquals(copyId, remaining.id)
        assertEquals("Caso editado (cópia)", remaining.name)
        assertEquals("U2' R", remaining.notation)
        assertEquals(CubeViewpoint.LEFT, remaining.viewpoint)
    }

    @Test
    fun rejectsMissingNameEmptySequenceAndInvalidNotation() = runBlocking {
        val repository = CustomAlgorithmRepository(FakeCustomAlgorithmDao())
        expectIllegalArgument {
            repository.create("", "R", emptyList(), CubeColorScheme.STANDARD, CubeViewpoint.FRONT)
        }
        expectIllegalArgument {
            repository.create("Nome", "", emptyList(), CubeColorScheme.STANDARD, CubeViewpoint.FRONT)
        }
        expectIllegalArgument {
            repository.create("Nome", "Q", emptyList(), CubeColorScheme.STANDARD, CubeViewpoint.FRONT)
        }
        assertTrue(repository.algorithms.first().isEmpty())
    }
}

private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
    try {
        block()
        fail("Era esperada uma IllegalArgumentException")
    } catch (_: IllegalArgumentException) {
        // Resultado esperado.
    }
}

private class FakeCustomAlgorithmDao : CustomAlgorithmDao {
    private val rows = MutableStateFlow<List<CustomAlgorithmEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<CustomAlgorithmEntity>> = rows

    override suspend fun find(id: Long): CustomAlgorithmEntity? = rows.value.singleOrNull { it.id == id }

    override suspend fun insert(algorithm: CustomAlgorithmEntity): Long {
        val id = if (algorithm.id == 0L) nextId++ else algorithm.id
        rows.value = rows.value + algorithm.copy(id = id)
        return id
    }

    override suspend fun update(algorithm: CustomAlgorithmEntity) {
        rows.value = rows.value.map { if (it.id == algorithm.id) algorithm else it }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
