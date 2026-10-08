package com.gabs.cubo3x3.cube

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class CubePlaybackCheckpointTest {
    @Test fun reconstructsEveryCompletedStepOfAll119PreparationSequences() {
        AlgorithmCatalog.allEntries().forEach { entry ->
            val moves = MoveNotation.parseAlgorithm(AlgorithmCatalog.setupNotation(entry))
            var expected = CubeState.solved()
            (0..moves.size).forEach { index ->
                val checkpoint = CubePlaybackCheckpoint(entry.id, index)
                assertEquals("${entry.id} passo $index", CubeFacelets.encode(expected),
                    CubeFacelets.encode(checkpoint.stateFor(CubeState.solved(), moves, entry.id)))
                if (index < moves.size) expected = expected.apply(moves[index])
            }
        }
    }

    @Test fun reconstructionAfterBackwardStepMatchesActualInverseIncludingDoublePrimes() {
        val moves = MoveNotation.parseAlgorithm("R U2' F' x y2' M2 L'")
        val initial = CubeState.solved()
        (1..moves.size).forEach { index ->
            val forward = initial.apply(moves.take(index))
            val backward = forward.apply(moves[index - 1].copy(
                quarterTurns = -moves[index - 1].quarterTurns))
            val restored = CubePlaybackCheckpoint("sequence", index - 1)
                .stateFor(initial, moves, "sequence")
            assertEquals(CubeFacelets.encode(backward), CubeFacelets.encode(restored))
        }
    }

    @Test fun rejectsCheckpointFromAnotherSourceOrFormulaAndClampsInvalidIndices() {
        assertEquals(0, CubePlaybackCheckpoint("old", 8).indexFor("new", 12))
        assertEquals(12, CubePlaybackCheckpoint("same", 99).indexFor("same", 12))
        assertEquals(0, CubePlaybackCheckpoint("same", -8).indexFor("same", 12))
        assertEquals(0, CubePlaybackCheckpoint("same", 1).indexFor("same", 0))
    }
}
