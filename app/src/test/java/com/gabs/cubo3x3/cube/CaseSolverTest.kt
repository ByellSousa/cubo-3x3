package com.gabs.cubo3x3.cube

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import cs.min2phase.Tools
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class CaseSolverTest {
    @Test fun codecMatchesIndependentEngineAndRoundTrips() {
        val random = Random(20261007)
        repeat(80) {
            val tokens = List(25) {
                "URFDLB"[random.nextInt(6)].toString() + listOf("", "'", "2")[random.nextInt(3)]
            }.joinToString(" ")
            val state = CubeState.solved().apply(MoveNotation.parseAlgorithm(tokens))
            assertEquals(Tools.fromScramble(tokens), CubeFacelets.encode(state))
            assertEquals(CubeFacelets.encode(state), CubeFacelets.encode(CubeFacelets.decode(
                CubeFacelets.encode(state))))
        }
    }

    @Test fun everyPreparationExactlyMatchesCatalogIncludingCenters() {
        AlgorithmCatalog.allEntries().forEach { entry ->
            val setup = AlgorithmCatalog.setupNotation(entry)
            val actual = CubeState.solved().apply(MoveNotation.parseAlgorithm(setup))
            val target = AlgorithmCatalog.initialState(entry)
            assertEquals(entry.id, CubeFacelets.encode(target), CubeFacelets.encode(actual))
            assertNull(entry.id, CaseSolver.validationError(CubeFacelets.encode(target)))
            assertTrue(actual.apply(MoveNotation.parseAlgorithm(entry.notation)).isSolved())
        }
    }

    @Test fun directSolverReachesAll119CasesFromSeededRandomStates() {
        val random = Random(4321)
        val durations = mutableListOf<Long>()
        AlgorithmCatalog.allEntries().forEach { entry ->
            repeat(2) {
                val source = Tools.randomCube(random)
                val target = AlgorithmCatalog.initialState(entry)
                val result = CaseSolver.compute(source, target)
                durations += result.elapsedMillis
                val actual = CubeFacelets.decode(source).apply(MoveNotation.parseAlgorithm(result.notation))
                assertEquals(entry.id, CubeFacelets.encode(target), CubeFacelets.encode(actual))
                assertTrue(MoveNotation.parseAlgorithm(result.notation).size <= 21)
            }
        }
        println("CaseSolver JVM: cold=${durations.first()}ms warmMax=${durations.drop(1).max()}ms " +
            "warmMedian=${durations.drop(1).sorted()[durations.size / 2]}ms; 238 transformations")
    }

    @Test fun targetAlreadyReachedDoesNotNeedMoves() {
        val target = AlgorithmCatalog.initialState(AlgorithmCatalog.allEntries().first())
        assertEquals("", CaseSolver.compute(CubeFacelets.encode(target), target).notation)
    }

    @Test fun rejectsCountsCentersFlipTwistParityAndDuplicatedPieces() {
        fun changed(vararg swaps: Pair<Int, Int>): String {
            val text = CubeFacelets.solved.toCharArray()
            swaps.forEach { (a, b) -> val value = text[a]; text[a] = text[b]; text[b] = value }
            return text.concatToString()
        }
        assertNull(CaseSolver.validationError(CubeFacelets.solved))
        assertNotNull(CaseSolver.validationError("U"))
        assertNotNull(CaseSolver.validationError(CubeFacelets.solved.replaceRange(0, 1, "R")))
        assertNotNull(CaseSolver.validationError(changed(4 to 13)))
        assertTrue(CaseSolver.validationError(changed(7 to 19))!!.contains("aresta está invertida"))
        assertTrue(CaseSolver.validationError(changed(8 to 9, 9 to 20))!!.contains("canto está torcido"))
        assertTrue(CaseSolver.validationError(changed(19 to 10))!!.contains("troca impossível"))
        assertNotNull(CaseSolver.validationError(changed(0 to 12)))
    }
}
