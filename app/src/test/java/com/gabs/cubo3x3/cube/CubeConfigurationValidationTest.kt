package com.gabs.cubo3x3.cube

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import cs.min2phase.Tools
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class CubeConfigurationValidationTest {
    private fun changed(vararg swaps: Pair<Int, Int>): String {
        val text = CubeFacelets.solved.toCharArray()
        swaps.forEach { (a, b) -> val value = text[a]; text[a] = text[b]; text[b] = value }
        return text.concatToString()
    }

    @Test fun legalRandomCubesAndAllCatalogTargetsHaveNoReviewMarks() {
        val random = Random(20261017)
        val states = List(500) { Tools.randomCube(random) } + AlgorithmCatalog.allEntries().map {
            CubeFacelets.encode(AlgorithmCatalog.initialState(it))
        } + CubeFacelets.solved
        states.forEach { state ->
            val report = CubeConfigurationValidation.inspect(state)
            assertTrue(state, report.isValid)
            assertNull(report.message)
            assertEquals(List(6) { 9 }, report.colorCounts)
            assertTrue(report.reviewStickerIndices.isEmpty())
        }
    }

    @Test fun countsArePreciseAndDoNotPretendToLocateTheWrongSticker() {
        val source = CubeFacelets.solved.replaceRange(0, 1, "F")
        val report = CubeConfigurationValidation.inspect(source)
        assertEquals(CubeConfigurationProblem.COLOR_COUNT, report.problem)
        assertEquals(listOf(8, 9, 10, 9, 9, 9), report.colorCounts)
        assertTrue(report.reviewStickerIndices.isEmpty())
        assertEquals('F', source[0])
    }

    @Test fun invalidAndDuplicatedEdgesMarkEveryAffectedPieceNotJustOneCopy() {
        // UF passa a FF; FR passa a UR, duplicando o UR ja existente.
        val source = changed(7 to 23)
        val report = CubeConfigurationValidation.inspect(source)
        assertEquals(CubeConfigurationProblem.EDGES, report.problem)
        assertEquals(setOf(5, 7, 10, 12, 19, 23), report.reviewStickerIndices)
        assertEquals(source, CubeFacelets.encode(CubeFacelets.decode(source)))
    }

    @Test fun incompatibleCornerColorsMarkAllThreeStickersOfEachPiece() {
        val report = CubeConfigurationValidation.inspect(changed(0 to 9))
        assertEquals(CubeConfigurationProblem.CORNERS, report.problem)
        assertEquals(setOf(0, 36, 47, 8, 9, 20), report.reviewStickerIndices)
        assertTrue(report.reviewStickerIndices.none { it % 9 == 4 })
    }

    @Test fun flipTwistAndParityDoNotInventASpecificStickerCorrection() {
        listOf(
            changed(7 to 19) to CubeConfigurationProblem.EDGE_FLIP,
            changed(8 to 9, 9 to 20) to CubeConfigurationProblem.CORNER_TWIST,
            changed(19 to 10) to CubeConfigurationProblem.PARITY,
        ).forEach { (state, problem) ->
            val report = CubeConfigurationValidation.inspect(state)
            assertEquals(problem, report.problem)
            assertFalse(report.isValid)
            assertNotNull(report.message)
            assertTrue(report.reviewStickerIndices.isEmpty())
            assertEquals(report.message, CaseSolver.validationError(state))
        }
    }

    @Test fun formatAndCentersAreRejectedBeforePhysicalVerification() {
        listOf("", "U", "X".repeat(54), CubeFacelets.solved + "U").forEach { state ->
            assertEquals(CubeConfigurationProblem.FORMAT, CubeConfigurationValidation.inspect(state).problem)
        }
        val report = CubeConfigurationValidation.inspect(changed(4 to 13))
        assertEquals(CubeConfigurationProblem.CENTERS, report.problem)
        assertEquals(setOf(4, 13), report.reviewStickerIndices)
    }

    @Test fun singleFlipAndSingleTwistAreRejectedAtEveryPiecePosition() {
        val groups = CubeFacelets.decode(CubeFacelets.solved).stickers.withIndex()
            .groupBy { it.value.position }.values.map { piece -> piece.map { it.index } }
        groups.filter { it.size == 2 }.forEach { indices ->
            val report = CubeConfigurationValidation.inspect(changed(indices[0] to indices[1]))
            assertEquals(CubeConfigurationProblem.EDGE_FLIP, report.problem)
            assertTrue(report.reviewStickerIndices.isEmpty())
        }
        groups.filter { it.size == 3 }.forEach { indices ->
            repeat(2) { direction ->
                val report = CubeConfigurationValidation.inspect(changed(
                    indices[0] to indices[1 + direction], indices[1 + direction] to indices[2 - direction]))
                assertEquals(CubeConfigurationProblem.CORNER_TWIST, report.problem)
                assertTrue(report.reviewStickerIndices.isEmpty())
            }
        }
    }

    @Test fun mirroredCornerIsRejectedAtAllEightPositionsAndAllThreeColorSwaps() {
        val groups = CubeFacelets.decode(CubeFacelets.solved).stickers.withIndex()
            .groupBy { it.value.position }.values.map { piece -> piece.map { it.index } }
        groups.filter { it.size == 3 }.forEach { indices ->
            listOf(0 to 1, 0 to 2, 1 to 2).forEach { (a, b) ->
                val report = CubeConfigurationValidation.inspect(changed(indices[a] to indices[b]))
                assertEquals(CubeConfigurationProblem.CORNERS, report.problem)
                assertEquals(indices.toSet(), report.reviewStickerIndices)
            }
        }
    }
}
