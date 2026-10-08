package com.gabs.cubo3x3.cube

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import cs.min2phase.Tools
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class CubePaintHistoryTest {
    @Test fun initialBoundariesDoNotCreateChanges() {
        val history = CubePaintHistory.solved()
        assertEquals(CubeFacelets.solved, history.facelets)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertSame(history, history.undo())
        assertSame(history, history.redo())
        assertSame(history, history.reset())
    }

    @Test fun everyEditableStickerChangesOnlyItsOwnColorAndKeepsAllCenters() {
        val initial = CubePaintHistory.solved()
        for (index in 0 until 54) {
            if (index % 9 == 4) continue
            for (color in CubeFacelets.faces) {
                val actual = initial.paint(index, color)
                assertEquals(CubeFacelets.solved.replaceRange(index, index + 1, color.toString()),
                    actual.facelets)
                assertEquals(CubeFacelets.solved, actual.undo().facelets)
                assertEquals(color != CubeFacelets.solved[index], actual.canUndo)
            }
        }
    }

    @Test fun undoAndRedoRestoreFullPaintingAcrossFaces() {
        val initial = CubePaintHistory.solved()
        val top = initial.paint(0, 'F')
        val right = top.paint(9, 'L')
        assertEquals(top.facelets, right.undo().facelets)
        assertEquals(initial.facelets, right.undo().undo().facelets)
        assertEquals(right.facelets, right.undo().undo().redo().redo().facelets)
        assertEquals(1, right.undo().changedFaceFrom(right))
        assertEquals(0, top.undo().changedFaceFrom(top))
        assertNull(right.changedFaceFrom(initial))
    }

    @Test fun paintingSameColorKeepsPendingRedoAndAddsNoStep() {
        val previous = CubePaintHistory.solved().paint(0, 'F').paint(9, 'L')
        val undone = previous.undo()
        val same = undone.paint(9, 'R')
        assertSame(undone, same)
        assertTrue(same.canRedo)
        assertEquals(previous.facelets, same.redo().facelets)
    }

    @Test fun newPaintingAfterUndoDiscardsOnlyTheRedoBranch() {
        val top = CubePaintHistory.solved().paint(0, 'F')
        val discarded = top.paint(9, 'L')
        val replacement = discarded.undo().paint(18, 'B')
        assertFalse(replacement.canRedo)
        assertEquals(top.facelets, replacement.undo().facelets)
        assertEquals(CubeFacelets.solved, replacement.undo().undo().facelets)
        assertNotEquals(discarded.facelets, replacement.facelets)
    }

    @Test fun confirmedResetIsOneUndoableStepEvenAcrossAllSixFaces() {
        var painted = CubePaintHistory.solved()
        CubeFacelets.faces.indices.forEach { face ->
            painted = painted.paint(face * 9, CubeFacelets.faces[(face + 1) % 6])
        }
        val reset = painted.reset()
        assertEquals(CubeFacelets.solved, reset.facelets)
        assertEquals(painted.facelets, reset.undo().facelets)
        assertEquals(reset.facelets, reset.undo().redo().facelets)
        assertNull(reset.changedFaceFrom(painted))
    }

    @Test fun historyRetainsExactlyTheLatest100Changes() {
        var history = CubePaintHistory.solved()
        val expected = mutableListOf(history.facelets)
        repeat(140) { index ->
            history = history.paint(0, if (index % 2 == 0) 'F' else 'L')
            expected += history.facelets
        }
        val final = history.facelets
        repeat(CubePaintHistory.MAX_UNDO_STEPS) { history = history.undo() }
        assertFalse(history.canUndo)
        assertEquals(expected[40], history.facelets)
        repeat(CubePaintHistory.MAX_UNDO_STEPS) { history = history.redo() }
        assertFalse(history.canRedo)
        assertEquals(final, history.facelets)
        assertEquals(CubePaintHistory.MAX_UNDO_STEPS + 3, history.checkpoint().size)
    }

    @Test fun checkpointRestoresInvalidDraftAndPendingUndoRedoWithoutAliasing() {
        val original = CubePaintHistory.solved().paint(0, 'F').paint(9, 'L').undo()
        val checkpoint = original.checkpoint().toMutableList()
        val restored = requireNotNull(CubePaintHistory.restore(checkpoint))
        checkpoint[2] = "corrupted"
        assertEquals(original.facelets, restored.facelets)
        assertEquals(original.undo().facelets, restored.undo().facelets)
        assertEquals(original.redo().facelets, restored.redo().facelets)
        assertTrue(restored.canUndo)
        assertTrue(restored.canRedo)
        assertFalse(CubeConfigurationValidation.inspect(restored.facelets).isValid)
    }

    @Test fun malformedCheckpointsAreRejectedAsAWhole() {
        val solved = CubeFacelets.solved
        val badCenter = solved.replaceRange(4, 5, "F")
        val invalid = listOf(
            emptyList(), listOf("1", "0"), listOf("2", "0", solved),
            listOf("1", "bad", solved), listOf("1", "-1", solved), listOf("1", "1", solved),
            listOf("1", "0", solved.dropLast(1)), listOf("1", "0", solved + "U"),
            listOf("1", "0", solved.replaceRange(0, 1, "X")),
            listOf("1", "0", badCenter), listOf("1", "0", solved, badCenter),
            listOf("1", "0") + List(CubePaintHistory.MAX_UNDO_STEPS + 2) { solved },
        )
        invalid.forEach { assertNull(CubePaintHistory.restore(it)) }
    }

    @Test fun restoredPaintingAfterUndoStillReachesExactF2LOLLAndPLLTargets() {
        val random = Random(20261019)
        listOf(AlgorithmCategory.F2L, AlgorithmCategory.OLL, AlgorithmCategory.PLL).forEach { category ->
            val source = Tools.randomCube(random)
            var painted = CubePaintHistory.solved()
            source.indices.filter { it % 9 != 4 }.forEach { index ->
                painted = painted.paint(index, source[index])
            }
            val wrongColor = CubeFacelets.faces.first { it != source[0] }
            val restored = requireNotNull(CubePaintHistory.restore(
                painted.paint(0, wrongColor).undo().checkpoint()))
            assertEquals(source, restored.facelets)
            assertTrue(restored.canRedo)
            assertTrue(CubeConfigurationValidation.inspect(restored.facelets).isValid)
            val beforeCalculation = restored.checkpoint()
            val target = AlgorithmCatalog.initialState(AlgorithmCatalog.entry(category, 1))
            val solution = CaseSolver.compute(restored.facelets, target)
            val actual = CubeFacelets.decode(restored.facelets)
                .apply(MoveNotation.parseAlgorithm(solution.notation))
            assertEquals(CubeFacelets.encode(target), CubeFacelets.encode(actual))
            assertEquals(beforeCalculation, restored.checkpoint())
        }
    }

    @Test fun invalidPaintingRequestsCannotChangeCentersOrHistory() {
        val history = CubePaintHistory.solved().paint(0, 'F')
        val before = history.checkpoint()
        listOf(-1, 54, 4, 13, 22, 31, 40, 49).forEach { index ->
            assertThrows(IllegalArgumentException::class.java) { history.paint(index, 'F') }
        }
        assertThrows(IllegalArgumentException::class.java) { history.paint(0, 'X') }
        assertEquals(before, history.checkpoint())
    }

    @Test fun randomOperationsMatchIndependentBoundedTimelineAndRestoreEveryStep() {
        val random = Random(20261019)
        var history = CubePaintHistory.solved()
        var timeline = listOf(CubeFacelets.solved)
        var position = 0
        repeat(1_000) {
            when (random.nextInt(5)) {
                0 -> { history = history.undo(); if (position > 0) position-- }
                1 -> { history = history.redo(); if (position < timeline.lastIndex) position++ }
                else -> {
                    val reset = random.nextInt(12) == 0
                    var index = random.nextInt(54)
                    while (index % 9 == 4) index = random.nextInt(54)
                    val color = CubeFacelets.faces[random.nextInt(6)]
                    val next = if (reset) CubeFacelets.solved
                        else timeline[position].replaceRange(index, index + 1, color.toString())
                    history = if (reset) history.reset() else history.paint(index, color)
                    if (next != timeline[position]) {
                        timeline = (timeline.take(position + 1) + next).takeLast(101)
                        position = timeline.lastIndex
                    }
                }
            }
            assertEquals(timeline[position], history.facelets)
            assertEquals(position > 0, history.canUndo)
            assertEquals(position < timeline.lastIndex, history.canRedo)
            val restored = requireNotNull(CubePaintHistory.restore(history.checkpoint()))
            assertEquals(history.checkpoint(), restored.checkpoint())
        }
    }
}
