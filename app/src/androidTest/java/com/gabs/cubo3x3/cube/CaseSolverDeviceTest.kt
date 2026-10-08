package com.gabs.cubo3x3.cube

import android.util.Log
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import cs.min2phase.Tools
import java.util.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Medicao controlada com cubos artificiais; nao le/escreve dados pessoais.
 * Executar esta classe sozinha em novo processo para medir o primeiro pedido
 * sem calculos anteriores da suite Compose. Nao e garantia para todos os cubos.
 */
class CaseSolverDeviceTest {
    @Test fun sixSeededInputsReachF2lOllAndPllWithMeasuredLatency() = runBlocking {
        val random = Random(20261007L)
        val cases = listOf(
            AlgorithmCategory.F2L to 1, AlgorithmCategory.OLL to 2,
            AlgorithmCategory.PLL to 18, AlgorithmCategory.F2L to 33,
            AlgorithmCategory.OLL to 57, AlgorithmCategory.PLL to 21,
        )
        cases.forEachIndexed { index, (category, number) ->
            val source = Tools.randomCube(random)
            val target = AlgorithmCatalog.initialState(AlgorithmCatalog.entry(category, number))
            val started = System.nanoTime()
            val result = CaseSolver.calculate(source, target)
            val wallMillis = (System.nanoTime() - started) / 1_000_000L
            val moves = MoveNotation.parseAlgorithm(result.notation)
            val actual = CubeFacelets.decode(source).apply(moves)
            assertEquals("Pedido $index", CubeFacelets.encode(target), CubeFacelets.encode(actual))
            assertTrue("Limite de movimentos", moves.size <= 21)
            Log.i("3x3CaseBenchmark", "sample=$index case=${category.name}-$number " +
                "workerMs=${result.elapsedMillis} wallMs=$wallMillis moves=${moves.size} verified54=true")
        }
    }
}
