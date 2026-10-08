package com.gabs.cubo3x3.cube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serializa o motor bloqueante. Cancelar a fila nao inicia trabalho nem publica resultado. */
internal class SerialCaseCalculator(
    private val compute: (String, CubeState, () -> Unit) -> CaseSolution,
) {
    private val mutex = Mutex()

    suspend fun calculate(source: String, target: CubeState): CaseSolution =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                ensureActive()
                val result = compute(source, target) { ensureActive() }
                // Busca Java nao cooperativa pode retornar mesmo depois de cancelada.
                ensureActive()
                result
            }
        }
}
