package com.gabs.cubo3x3.cube

import cs.min2phase.Tools
import org.junit.Assert.assertEquals
import org.junit.Test

class CubeNetTnoodleCompatibilityTest {
    @Test
    fun everyOuterFaceTurnMatchesTnoodleFacelets() {
        listOf(
            "U", "U'", "U2",
            "R", "R'", "R2",
            "F", "F'", "F2",
            "D", "D'", "D2",
            "L", "L'", "L2",
            "B", "B'", "B2",
        ).forEach(::assertMatchesTnoodle)
    }

    @Test
    fun mixedCompetitionStyleScramblesMatchTnoodleFacelets() {
        listOf(
            "R U R' U'",
            "F R U R' U' F'",
            "R2 U' F2 D L2 B' U2 R F' D2 L B2 U'",
            "U' F' L R2 B L2 B2 R2 B' R2 D2 F U2 F L' B' D' F2 R2 D U'",
        ).forEach(::assertMatchesTnoodle)
    }

    @Test
    fun wcaColoredNetMatchesTnoodleForEveryOuterFaceTurnAndMixedScrambles() {
        val scrambles = listOf(
            "U", "U'", "U2", "R", "R'", "R2", "F", "F'", "F2",
            "D", "D'", "D2", "L", "L'", "L2", "B", "B'", "B2",
            "R U R' U'",
            "R2 U' F2 D L2 B' U2 R F' D2 L B2 U'",
            "U' F' L R2 B L2 B2 R2 B' R2 D2 F U2 F L' B' D' F2 R2 D U'",
        )

        scrambles.forEach { scramble ->
            val state = CubeState.solvedWcaScrambleOrientation()
                .apply(MoveNotation.parseAlgorithm(scramble))
            val actual = CubeNet.from(state).toWcaFacelets()

            assertEquals(
                "Estado WCA divergente para: $scramble",
                Tools.fromScramble(scramble),
                actual,
            )
        }
    }

    private fun assertMatchesTnoodle(scramble: String) {
        val state = CubeState.solved().apply(MoveNotation.parseAlgorithm(scramble))
        val actual = CubeNet.from(state).toMin2phaseFacelets()
        val expected = Tools.fromScramble(scramble)

        assertEquals("Estado divergente para: $scramble", expected, actual)
    }

    private fun CubeNet.toMin2phaseFacelets(): String = buildString(54) {
        append(up.toFacelets())
        append(right.toFacelets())
        append(front.toFacelets())
        append(down.toFacelets())
        append(left.toFacelets())
        append(back.toFacelets())
    }

    private fun List<StickerColor>.toFacelets(): String = joinToString(separator = "") { color ->
        when (color) {
            StickerColor.YELLOW -> "U"
            StickerColor.GREEN -> "R"
            StickerColor.RED -> "F"
            StickerColor.WHITE -> "D"
            StickerColor.BLUE -> "L"
            StickerColor.ORANGE -> "B"
        }
    }

    private fun CubeNet.toWcaFacelets(): String = buildString(54) {
        append(up.toWcaFacelets())
        append(right.toWcaFacelets())
        append(front.toWcaFacelets())
        append(down.toWcaFacelets())
        append(left.toWcaFacelets())
        append(back.toWcaFacelets())
    }

    private fun List<StickerColor>.toWcaFacelets(): String = joinToString(separator = "") { color ->
        when (color) {
            StickerColor.WHITE -> "U"
            StickerColor.RED -> "R"
            StickerColor.GREEN -> "F"
            StickerColor.YELLOW -> "D"
            StickerColor.ORANGE -> "L"
            StickerColor.BLUE -> "B"
        }
    }
}
