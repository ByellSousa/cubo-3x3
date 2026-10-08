package com.gabs.cubo3x3.cube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeModelTest {
    @Test
    fun solvedCubeHasNineStickersOfEachColor() {
        val state = CubeState.solved()

        assertEquals(CubeState.STICKER_COUNT, state.stickers.size)
        StickerColor.entries.forEach { color ->
            assertEquals(9, state.stickers.count { it.color == color })
        }
        assertTrue(state.isSolved())
    }

    @Test
    fun solvedCubeUsesCfopViewingOrientation() {
        val centers = CubeState.solved().stickers
            .filter { it.position == it.normal }
            .associate { it.normal to it.color }

        assertEquals(StickerColor.YELLOW, centers[Vec3i(0, 1, 0)])
        assertEquals(StickerColor.WHITE, centers[Vec3i(0, -1, 0)])
        assertEquals(StickerColor.RED, centers[Vec3i(0, 0, 1)])
        assertEquals(StickerColor.GREEN, centers[Vec3i(1, 0, 0)])
        assertEquals(StickerColor.ORANGE, centers[Vec3i(0, 0, -1)])
        assertEquals(StickerColor.BLUE, centers[Vec3i(-1, 0, 0)])
    }

    @Test
    fun wcaScrambleCubeUsesOfficialStartingOrientation() {
        val centers = CubeState.solvedWcaScrambleOrientation().stickers
            .filter { it.position == it.normal }
            .associate { it.normal to it.color }

        assertEquals(StickerColor.WHITE, centers[Vec3i(0, 1, 0)])
        assertEquals(StickerColor.YELLOW, centers[Vec3i(0, -1, 0)])
        assertEquals(StickerColor.GREEN, centers[Vec3i(0, 0, 1)])
        assertEquals(StickerColor.RED, centers[Vec3i(1, 0, 0)])
        assertEquals(StickerColor.BLUE, centers[Vec3i(0, 0, -1)])
        assertEquals(StickerColor.ORANGE, centers[Vec3i(-1, 0, 0)])
    }

    @Test
    fun parserCoversAllFiftyFourCanonicalMoves() {
        assertEquals(54, MoveNotation.allCanonicalTokens.size)

        MoveNotation.allCanonicalTokens.forEach { token ->
            assertEquals(token, MoveNotation.parseToken(token).symbol)
        }
    }

    @Test
    fun parserAlsoAcceptsUppercaseWideNotation() {
        val wide = MoveNotation.parseAlgorithm("Rw Uw' Fw2")

        assertEquals(setOf(0, 1), wide[0].layers)
        assertEquals(setOf(0, 1), wide[1].layers)
        assertEquals(2, wide[2].repetitions)
    }

    @Test
    fun parserPreservesCounterclockwiseDoubleTurns() {
        MoveNotation.roots.forEach { root ->
            val clockwise = MoveNotation.parseToken(root + "2")
            val counterclockwise = MoveNotation.parseToken(root + "2'")

            assertEquals(root + "2'", counterclockwise.symbol)
            assertEquals(2, counterclockwise.repetitions)
            assertEquals(-clockwise.quarterTurns, counterclockwise.quarterTurns)
            assertEquals(
                CubeState.solved().apply(clockwise),
                CubeState.solved().apply(counterclockwise),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun parserRejectsUnknownMove() {
        MoveNotation.parseToken("Q")
    }

    @Test
    fun everyMoveFollowedByItsInverseReturnsToSolvedState() {
        val solved = CubeState.solved()

        MoveNotation.allCanonicalTokens.forEach { token ->
            val move = MoveNotation.parseToken(token)
            val result = solved.apply(move).apply(move.inverse())
            assertEquals("Falhou em " + token, solved, result)
        }
    }

    @Test
    fun fourQuarterTurnsReturnToSolvedState() {
        val solved = CubeState.solved()

        MoveNotation.roots.forEach { token ->
            val move = MoveNotation.parseToken(token)
            val result = solved.apply(List(4) { move })
            assertEquals("Falhou em " + token, solved, result)
        }
    }

    @Test
    fun sexyMoveHasOrderSix() {
        val solved = CubeState.solved()
        val sequence = MoveNotation.parseAlgorithm("R U R' U'")
        val result = solved.apply(List(6) { sequence }.flatten())

        assertEquals(solved, result)
    }

    @Test
    fun aSingleFaceTurnChangesTheCube() {
        val result = CubeState.solved().apply(MoveNotation.parseToken("R"))

        assertFalse(result.isSolved())
    }
}
