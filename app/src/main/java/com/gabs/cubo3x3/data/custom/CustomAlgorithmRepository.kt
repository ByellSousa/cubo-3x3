package com.gabs.cubo3x3.data.custom

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.custom.CustomAlgorithmEntity.Companion.TAG_SEPARATOR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CustomAlgorithmRepository(
    private val dao: CustomAlgorithmDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutationMutex = Mutex()

    val algorithms: Flow<List<CustomAlgorithm>> = dao.observeAll().map { entities ->
        entities.map(CustomAlgorithmEntity::toCustomAlgorithm)
    }

    suspend fun create(
        name: String,
        notation: String,
        tags: List<String>,
        colorScheme: CubeColorScheme,
        viewpoint: CubeViewpoint,
    ): Long = mutationMutex.withLock {
        val normalized = normalize(name, notation, tags)
        val timestamp = now()
        dao.insert(
            CustomAlgorithmEntity(
                name = normalized.name,
                notation = normalized.notation,
                tags = normalized.tags.joinToString(TAG_SEPARATOR),
                colorScheme = colorScheme.name,
                viewpoint = viewpoint.name,
                createdAtEpochMillis = timestamp,
                updatedAtEpochMillis = timestamp,
            ),
        )
    }

    suspend fun update(
        id: Long,
        name: String,
        notation: String,
        tags: List<String>,
        colorScheme: CubeColorScheme,
        viewpoint: CubeViewpoint,
    ) = mutationMutex.withLock {
        val current = requireNotNull(dao.find(id)) { "Algoritmo personalizado inexistente: $id" }
        val normalized = normalize(name, notation, tags)
        dao.update(
            current.copy(
                name = normalized.name,
                notation = normalized.notation,
                tags = normalized.tags.joinToString(TAG_SEPARATOR),
                colorScheme = colorScheme.name,
                viewpoint = viewpoint.name,
                updatedAtEpochMillis = now(),
            ),
        )
    }

    suspend fun duplicate(id: Long): Long = mutationMutex.withLock {
        val current = requireNotNull(dao.find(id)) { "Algoritmo personalizado inexistente: $id" }
        val timestamp = now()
        dao.insert(
            current.copy(
                id = 0L,
                name = "${current.name} (cópia)",
                createdAtEpochMillis = timestamp,
                updatedAtEpochMillis = timestamp,
            ),
        )
    }

    suspend fun delete(id: Long) = mutationMutex.withLock { dao.delete(id) }

    private fun normalize(name: String, notation: String, tags: List<String>): NormalizedInput {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "O nome é obrigatório" }
        val moves = MoveNotation.parseAlgorithm(notation)
        require(moves.isNotEmpty()) { "Adicione pelo menos um movimento" }
        val normalizedTags = tags
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(String::lowercase)
        return NormalizedInput(
            name = normalizedName,
            notation = moves.joinToString(" ") { it.symbol },
            tags = normalizedTags,
        )
    }

    private data class NormalizedInput(
        val name: String,
        val notation: String,
        val tags: List<String>,
    )
}
