package com.gabs.cubo3x3.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AlgorithmProgressRepository(
    private val dao: AlgorithmProgressDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutationMutex = Mutex()

    val progress: Flow<Map<String, AlgorithmProgress>> = dao.observeAll().map { entities ->
        entities.associate { entity -> entity.key to entity.toProgress() }
    }

    suspend fun setFavorite(category: String, caseNumber: Int, isFavorite: Boolean) {
        update(category, caseNumber) { current -> current.copy(isFavorite = isFavorite) }
    }

    suspend fun setCompleted(category: String, caseNumber: Int, isCompleted: Boolean) {
        update(category, caseNumber) { current -> current.copy(isCompleted = isCompleted) }
    }

    suspend fun clear() = mutationMutex.withLock {
        dao.clear()
    }

    private suspend fun update(
        category: String,
        caseNumber: Int,
        transform: (AlgorithmProgress) -> AlgorithmProgress,
    ) = mutationMutex.withLock {
        val current = dao.find(category, caseNumber)?.toProgress() ?: AlgorithmProgress()
        val updated = transform(current)
        if (!updated.isFavorite && !updated.isCompleted) {
            dao.delete(category, caseNumber)
        } else {
            dao.upsert(
                AlgorithmProgressEntity(
                    category = category,
                    caseNumber = caseNumber,
                    isFavorite = updated.isFavorite,
                    isCompleted = updated.isCompleted,
                    updatedAtEpochMillis = now(),
                ),
            )
        }
    }
}
