package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.CubeVisualMode
import com.gabs.cubo3x3.cube.Sticker
import com.gabs.cubo3x3.cube.isColoredSticker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmDiagramTest {
    @Test
    fun `F2L colore somente os 33 adesivos das duas primeiras camadas`() {
        AlgorithmCatalog.entries(AlgorithmCategory.F2L).forEach { entry ->
            assertEquals(
                "Máscara incorreta no F2L ${entry.number}",
                33,
                coloredStickerCount(
                    state = AlgorithmCatalog.initialState(entry),
                    mode = CubeVisualMode.F2L_LAYERS,
                ),
            )
        }
    }

    @Test
    fun `OLL colore somente os nove adesivos amarelos da ultima camada`() {
        AlgorithmCatalog.entries(AlgorithmCategory.OLL).forEach { entry ->
            assertEquals(
                "Máscara incorreta no OLL ${entry.number}",
                9,
                coloredStickerCount(
                    state = AlgorithmCatalog.initialState(entry),
                    mode = CubeVisualMode.OLL_YELLOW,
                ),
            )
        }
    }

    @Test
    fun `PLL colore os 21 adesivos completos da ultima camada`() {
        AlgorithmCatalog.entries(AlgorithmCategory.PLL).forEach { entry ->
            assertEquals(
                "Máscara incorreta no PLL ${entry.number}",
                21,
                coloredStickerCount(
                    state = AlgorithmCatalog.initialState(entry),
                    mode = CubeVisualMode.PLL_LAST_LAYER,
                ),
            )
        }
    }

    @Test
    fun `os 57 diagramas OLL mostram nove adesivos amarelos e nao se repetem`() {
        val patterns = AlgorithmCatalog.entries(AlgorithmCategory.OLL).map { entry ->
            ollPattern(AlgorithmCatalog.initialState(entry))
        }

        patterns.forEach { pattern ->
            assertEquals(9, pattern.top.size + pattern.markers.size)
            assertTrue(DiagramCell(column = 1, row = 1) in pattern.top)
        }
        assertEquals(57, patterns.toSet().size)
    }

    @Test
    fun `os 21 diagramas PLL tem setas distintas e terminam sem diferencas`() {
        val entries = AlgorithmCatalog.entries(AlgorithmCategory.PLL)
        val arrowSets = entries.map { entry ->
            pllPermutationArrows(AlgorithmCatalog.initialState(entry)).toSet()
        }

        arrowSets.forEach { arrows -> assertTrue(arrows.isNotEmpty()) }
        assertEquals(21, arrowSets.toSet().size)

        entries.forEach { entry ->
            val finalState = AlgorithmCatalog.initialState(entry).apply(
                MoveNotation.parseAlgorithm(entry.notation),
            )
            assertTrue(pllPermutationArrows(finalState).isEmpty())
        }
    }

    private fun coloredStickerCount(state: CubeState, mode: CubeVisualMode): Int {
        val colorsByPosition = state.stickers
            .groupBy(Sticker::position)
            .mapValues { (_, stickers) -> stickers.map(Sticker::color).toSet() }
        return state.stickers.count { sticker ->
            mode.isColoredSticker(
                color = sticker.color,
                cubieColors = colorsByPosition.getValue(sticker.position),
            )
        }
    }
}
