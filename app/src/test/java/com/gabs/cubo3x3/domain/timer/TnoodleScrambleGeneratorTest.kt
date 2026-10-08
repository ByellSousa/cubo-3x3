package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.cube.MoveNotation
import java.util.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class TnoodleScrambleGeneratorTest {
    @Test
    fun generatesParseableRandomStateScramble() {
        val scramble = TnoodleScrambleGenerator(Random(1234L)).nextScramble()
        val moves = MoveNotation.parseAlgorithm(scramble)

        assertTrue(moves.size in 2..21)
        assertTrue(moves.all { it.symbol.first() in "UDFBRL" })
    }
}
