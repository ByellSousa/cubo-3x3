package com.gabs.cubo3x3.data

import com.gabs.cubo3x3.cube.Axis
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.Move
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PLLAlgorithmsTest {
    @Test
    fun `catalogo contem os 21 casos em ordem`() {
        assertEquals((1..21).toList(), PLLAlgorithms.all.map { it.number })
        assertEquals(21, PLLAlgorithms.all.map { it.number }.toSet().size)
    }

    @Test
    fun `todos os algoritmos usam notacao suportada`() {
        PLLAlgorithms.all.forEach { case ->
            assertTrue(
                "Caso ${case.number} nao foi interpretado",
                MoveNotation.parseAlgorithm(case.notation).isNotEmpty(),
            )
        }
    }

    @Test
    fun `todos os algoritmos resolvem o estado reconstruido`() {
        val solved = CubeState.solved()

        PLLAlgorithms.all.forEach { case ->
            val moves = MoveNotation.parseAlgorithm(case.notation)
            val target = orientedSolvedTarget(solved, moves)
            val state = target.apply(moves.asReversed().map { it.inverse() })

            assertEquals("Falha no caso ${case.number}", target, state.apply(moves))
        }
    }

    @Test
    fun `todos os casos representam PLL valido e nao resolvido`() {
        val solved = CubeState.solved()

        PLLAlgorithms.all.forEach { case ->
            val state = stateBeforeAlgorithm(solved, case.notation)
            assertTrue("Caso ${case.number} nao representa PLL valido", isValidPll(state))
        }
    }

    @Test
    fun `catalogo nao repete permutacoes equivalentes por ajuste U`() {
        val solved = CubeState.solved()
        val semanticKeys = PLLAlgorithms.all.map { case ->
            semanticKey(stateBeforeAlgorithm(solved, case.notation))
        }

        assertEquals(21, semanticKeys.toSet().size)
    }

    private fun stateBeforeAlgorithm(solved: CubeState, notation: String): CubeState {
        val moves = MoveNotation.parseAlgorithm(notation)
        return orientedSolvedTarget(solved, moves)
            .apply(moves.asReversed().map { it.inverse() })
    }

    private fun orientedSolvedTarget(solved: CubeState, moves: List<Move>): CubeState {
        val centerResult = solved.apply(moves)
        val centers = centerResult.stickers
            .filter { it.position == it.normal && it.position.nonZeroCount() == 1 }
            .associate { it.color to it.normal }
        val right = requireNotNull(centers[StickerColor.GREEN])
        val up = requireNotNull(centers[StickerColor.YELLOW])
        val front = requireNotNull(centers[StickerColor.RED])

        fun oriented(vector: Vec3i): Vec3i = Vec3i(
            x = right.x * vector.x + up.x * vector.y + front.x * vector.z,
            y = right.y * vector.x + up.y * vector.y + front.y * vector.z,
            z = right.z * vector.x + up.z * vector.y + front.z * vector.z,
        )

        return CubeState(
            solved.stickers.map { sticker ->
                sticker.copy(
                    position = oriented(sticker.position),
                    normal = oriented(sticker.normal),
                )
            },
        )
    }

    private fun isValidPll(state: CubeState): Boolean {
        val stickers = canonicalStickers(state)
        val oriented = stickers
            .filter { it.color == StickerColor.YELLOW }
            .all { it.normal == UP }
        val lowerLayersSolved = stickers
            .filter { it.position.y < 1 }
            .all { it.color == solvedColor(it.normal) }
        val fullySolved = stickers.all { it.color == solvedColor(it.normal) }
        return oriented && lowerLayersSolved && !fullySolved
    }

    private fun semanticKey(state: CubeState): String {
        val top = canonicalStickers(state).filter { it.position.y == 1 }
        return (0..3).map { turns ->
            top.map { sticker ->
                var position = sticker.position
                var normal = sticker.normal
                repeat(turns) {
                    position = position.rotateQuarter(Axis.Y, -1)
                    normal = normal.rotateQuarter(Axis.Y, -1)
                }
                sticker.copy(position = position, normal = normal)
            }.sortedWith(
                compareBy<CanonicalSticker>(
                    { it.position.x }, { it.position.y }, { it.position.z },
                    { it.normal.x }, { it.normal.y }, { it.normal.z }, { it.color.ordinal },
                ),
            ).joinToString("|") { sticker ->
                "${sticker.position.x},${sticker.position.y},${sticker.position.z}:" +
                    "${sticker.normal.x},${sticker.normal.y},${sticker.normal.z}:" +
                    sticker.color.name
            }
        }.min()
    }

    private fun canonicalStickers(state: CubeState): List<CanonicalSticker> {
        val centers = state.stickers
            .filter { it.position == it.normal && it.position.nonZeroCount() == 1 }
            .associate { it.color to it.normal }
        val right = requireNotNull(centers[StickerColor.GREEN])
        val up = requireNotNull(centers[StickerColor.YELLOW])
        val front = requireNotNull(centers[StickerColor.RED])

        fun canonical(vector: Vec3i): Vec3i = Vec3i(
            vector.dot(right),
            vector.dot(up),
            vector.dot(front),
        )

        return state.stickers.map { sticker ->
            CanonicalSticker(
                position = canonical(sticker.position),
                normal = canonical(sticker.normal),
                color = sticker.color,
            )
        }
    }

    private fun solvedColor(normal: Vec3i): StickerColor = when (normal) {
        UP -> StickerColor.YELLOW
        Vec3i(0, -1, 0) -> StickerColor.WHITE
        Vec3i(1, 0, 0) -> StickerColor.GREEN
        Vec3i(-1, 0, 0) -> StickerColor.BLUE
        Vec3i(0, 0, 1) -> StickerColor.RED
        Vec3i(0, 0, -1) -> StickerColor.ORANGE
        else -> error("Normal invalida: $normal")
    }

    private fun Vec3i.dot(other: Vec3i): Int =
        x * other.x + y * other.y + z * other.z

    private fun Vec3i.nonZeroCount(): Int = listOf(x, y, z).count { it != 0 }

    private data class CanonicalSticker(
        val position: Vec3i,
        val normal: Vec3i,
        val color: StickerColor,
    )

    private companion object {
        val UP = Vec3i(0, 1, 0)
    }
}
