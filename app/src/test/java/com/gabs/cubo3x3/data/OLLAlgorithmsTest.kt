package com.gabs.cubo3x3.data

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.Sticker
import com.gabs.cubo3x3.cube.Vec3i
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OLLAlgorithmsTest {
    @Test
    fun `catalogo contem os 57 casos em ordem`() {
        assertEquals((1..57).toList(), OLLAlgorithms.all.map { it.number })
        assertEquals(57, OLLAlgorithms.all.map { it.number }.toSet().size)
    }

    @Test
    fun `todos os algoritmos usam notacao suportada`() {
        OLLAlgorithms.all.forEach { case ->
            assertTrue(
                "Caso ${case.number} nao foi interpretado",
                MoveNotation.parseAlgorithm(case.notation).isNotEmpty(),
            )
        }
    }

    @Test
    fun `todo algoritmo e seu inverso restauram o cubo`() {
        val solved = CubeState.solved()

        OLLAlgorithms.all.forEach { case ->
            val moves = MoveNotation.parseAlgorithm(case.notation)
            val inverse = moves.asReversed().map { it.inverse() }
            val restored = solved.apply(moves).apply(inverse)

            assertEquals("Falha no caso ${case.number}", solved, restored)
        }
    }

    @Test
    fun `todos os casos representam OLL valido e nao resolvido`() {
        val solved = CubeState.solved()
        val solvedSignature = orientationSignature(solved)

        OLLAlgorithms.all.forEach { case ->
            val state = stateBeforeAlgorithm(solved, case.notation)

            assertTrue(
                "Caso ${case.number} move uma peca da ultima camada para fora dela",
                isLastLayerSetPreserved(state),
            )
            assertNotEquals(
                "Caso ${case.number} representa OLL ja resolvido",
                solvedSignature,
                orientationSignature(state),
            )
        }
    }

    @Test
    fun `catalogo nao repete orientacoes equivalentes por ajuste U`() {
        val solved = CubeState.solved()
        val semanticKeys = OLLAlgorithms.all.map { case ->
            val state = stateBeforeAlgorithm(solved, case.notation)

            generateSequence(state) { current ->
                current.apply(MoveNotation.parseToken("U"))
            }.take(4)
                .map(::orientationSignature)
                .min()
        }

        assertEquals(57, semanticKeys.toSet().size)
    }

    private fun stateBeforeAlgorithm(solved: CubeState, notation: String): CubeState {
        val inverse = MoveNotation.parseAlgorithm(notation)
            .asReversed()
            .map { it.inverse() }
        return solved.apply(inverse)
    }

    private fun isLastLayerSetPreserved(state: CubeState): Boolean {
        val upCenter = state.stickers.single { sticker ->
            sticker.position == UP && sticker.normal == UP
        }
        return state.stickers
            .filter { it.color == upCenter.color }
            .all { it.position.y == 1 }
    }

    private fun orientationSignature(state: CubeState): String {
        val upCenter = state.stickers.single { sticker ->
            sticker.position == UP && sticker.normal == UP
        }
        return state.stickers
            .filter { it.color == upCenter.color }
            .sortedWith(compareBy<Sticker>({ it.position.z }, { it.position.x }))
            .joinToString("|") { sticker ->
                "${sticker.position.x},${sticker.position.z}:" +
                    "${sticker.normal.x},${sticker.normal.y},${sticker.normal.z}"
            }
    }

    private companion object {
        val UP = Vec3i(0, 1, 0)
    }
}
