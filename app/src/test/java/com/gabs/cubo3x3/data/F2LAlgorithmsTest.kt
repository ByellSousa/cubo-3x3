package com.gabs.cubo3x3.data

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.Sticker
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class F2LAlgorithmsTest {
    @Test
    fun `catalogo contem os 41 casos nao resolvidos em ordem`() {
        assertEquals((1..41).toList(), F2LAlgorithms.all.map { it.number })
        assertEquals(41, F2LAlgorithms.all.map { it.number }.toSet().size)
    }

    @Test
    fun `todos os algoritmos usam notacao suportada`() {
        F2LAlgorithms.all.forEach { case ->
            assertTrue(
                "Caso ${case.number} nao foi interpretado",
                MoveNotation.parseAlgorithm(case.notation).isNotEmpty(),
            )
        }
    }

    @Test
    fun `todo algoritmo e seu inverso restauram o cubo`() {
        val solved = CubeState.solved()

        F2LAlgorithms.all.forEach { case ->
            val moves = MoveNotation.parseAlgorithm(case.notation)
            val inverse = moves.asReversed().map { it.inverse() }
            val restored = solved.apply(moves).apply(inverse)

            assertEquals("Falha no caso ${case.number}", solved, restored)
        }
    }

    @Test
    fun `catalogo nao repete casos equivalentes por ajuste U`() {
        val solved = CubeState.solved()
        val semanticKeys = F2LAlgorithms.all.map { case ->
            val inverse = MoveNotation.parseAlgorithm(case.notation)
                .asReversed()
                .map { it.inverse() }
            val state = solved.apply(inverse)
            val targetSlot = slots.single { slot ->
                pairSignature(state, slot) != SOLVED_PAIR
            }

            generateSequence(state) { current ->
                current.apply(MoveNotation.parseToken("U"))
            }.take(4)
                .mapNotNull { current -> pairSignature(current, targetSlot) }
                .min()
        }

        assertEquals(41, semanticKeys.toSet().size)
    }

    private fun pairSignature(state: CubeState, slot: Slot): String? {
        val centers = state.stickers
            .filter { sticker ->
                sticker.position == sticker.normal && sticker.position.nonZeroCount() == 1
            }.associate { it.normal to it.color }
        val downColor = centers[Vec3i(0, -1, 0)] ?: return null
        val frontColor = centers[slot.front] ?: return null
        val rightColor = centers[slot.right] ?: return null
        val cubies = state.stickers.groupBy(Sticker::position)
        val corner = cubies.values.singleOrNull { stickers ->
            stickers.map(Sticker::color).toSet() == setOf(downColor, frontColor, rightColor)
        } ?: return null
        val edge = cubies.values.singleOrNull { stickers ->
            stickers.map(Sticker::color).toSet() == setOf(frontColor, rightColor)
        } ?: return null

        fun roleNormal(stickers: List<Sticker>, color: StickerColor): Vec3i =
            slot.logical(stickers.single { it.color == color }.normal)

        return buildString {
            append("C")
            append(slot.logical(corner.first().position).compact())
            append("D")
            append(roleNormal(corner, downColor).compact())
            append("F")
            append(roleNormal(corner, frontColor).compact())
            append("R")
            append(roleNormal(corner, rightColor).compact())
            append("E")
            append(slot.logical(edge.first().position).compact())
            append("F")
            append(roleNormal(edge, frontColor).compact())
            append("R")
            append(roleNormal(edge, rightColor).compact())
        }
    }

    private data class Slot(val front: Vec3i, val right: Vec3i) {
        fun logical(vector: Vec3i): Vec3i = Vec3i(
            x = vector.x * right.x + vector.z * right.z,
            y = vector.y,
            z = vector.x * front.x + vector.z * front.z,
        )
    }

    private fun Vec3i.nonZeroCount(): Int = listOf(x, y, z).count { it != 0 }
    private fun Vec3i.compact(): String = "($x,$y,$z)"

    private companion object {
        val slots = listOf(
            Slot(Vec3i(0, 0, 1), Vec3i(1, 0, 0)),
            Slot(Vec3i(1, 0, 0), Vec3i(0, 0, -1)),
            Slot(Vec3i(0, 0, -1), Vec3i(-1, 0, 0)),
            Slot(Vec3i(-1, 0, 0), Vec3i(0, 0, 1)),
        )
        const val SOLVED_PAIR =
            "C(1,-1,1)D(0,-1,0)F(0,0,1)R(1,0,0)E(1,0,1)F(0,0,1)R(1,0,0)"
    }
}
