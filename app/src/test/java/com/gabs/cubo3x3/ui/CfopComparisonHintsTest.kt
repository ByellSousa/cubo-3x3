package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import com.gabs.cubo3x3.domain.quiz.CfopComparisonKey
import org.junit.Assert.*
import org.junit.Test

class CfopComparisonHintsTest {
    @Test fun f2lCuesDescribeTheActualTrackedCubiesInEveryCase() {
        val corner = setOf(StickerColor.WHITE, StickerColor.RED, StickerColor.GREEN)
        val edge = setOf(StickerColor.RED, StickerColor.GREEN)
        AlgorithmCatalog.entries(AlgorithmCategory.F2L).forEach { entry ->
            val key = CfopComparisonKey(AlgorithmCategory.F2L, minOf(1, entry.number),
                if (entry.number == 1) 2 else entry.number)
            val cues = comparisonCues(key)
            val values = cues.map { if (entry.number == 1) it.first else it.second }
            val state = AlgorithmCatalog.initialState(entry)
            listOf(corner, edge).forEachIndexed { index, colors ->
                val piece = state.stickers.groupBy { it.position }.values.single {
                    it.map { sticker -> sticker.color }.toSet() == colors
                }
                val position = piece.first().position
                val words = values[index * 2].split(" · ")
                assertTrue(words.contains(when {
                    position.y > 0 -> "acima"; position.y < 0 -> "abaixo"; else -> "camada do meio"
                }))
                assertEquals(position.x != 0, words.any { it in listOf("esquerda", "direita") })
                assertEquals(position.z != 0, words.any { it in listOf("frente", "atrás") })
                if (position.x != 0) assertTrue(words.contains(if (position.x > 0) "direita" else "esquerda"))
                if (position.z != 0) assertTrue(words.contains(if (position.z > 0) "frente" else "atrás"))
                piece.forEach { sticker ->
                    val color = mapOf(StickerColor.WHITE to "branco", StickerColor.RED to "vermelho",
                        StickerColor.GREEN to "verde").getValue(sticker.color)
                    val face = mapOf(Vec3i(0, 1, 0) to "acima", Vec3i(0, -1, 0) to "abaixo",
                        Vec3i(0, 0, 1) to "frente", Vec3i(0, 0, -1) to "atrás",
                        Vec3i(1, 0, 0) to "direita", Vec3i(-1, 0, 0) to "esquerda").getValue(sticker.normal)
                    assertTrue(values[index * 2 + 1].contains("$color → $face"))
                }
            }
        }
    }

    @Test fun ollTopAndSideCoordinatesMatchAllFiftySevenCanonicalDiagrams() {
        AlgorithmCatalog.entries(AlgorithmCategory.OLL).forEach { entry ->
            val key = CfopComparisonKey(AlgorithmCategory.OLL, 1, maxOf(2, entry.number))
            val cues = comparisonCues(key)
            val values = cues.map { if (entry.number == 1) it.first else it.second }
            val pattern = ollPattern(AlgorithmCatalog.initialState(entry))
            assertEquals(pattern.top, values[0].split(", ").map {
                DiagramCell(it[3].digitToInt() - 1, it[1].digitToInt() - 1)
            }.toSet())
            val markers = if (values[1] == "nenhum") emptySet() else values[1].split("; ").map {
                val (side, coordinate) = it.split(" ")
                SideMarker(when (side) {
                    "atrás" -> DiagramSide.NORTH; "direita" -> DiagramSide.EAST
                    "frente" -> DiagramSide.SOUTH; "esquerda" -> DiagramSide.WEST
                    else -> error("Lateral desconhecida")
                }, coordinate[1].digitToInt() - 1)
            }.toSet()
            assertEquals(pattern.markers, markers)
            assertEquals(9, pattern.top.size + markers.size)
        }
    }

    @Test fun pllDestinationCuesFollowArrowsRatherThanReversingThem() {
        AlgorithmCatalog.entries(AlgorithmCategory.PLL).forEach { entry ->
            val key = CfopComparisonKey(AlgorithmCategory.PLL, 1, maxOf(2, entry.number))
            val arrows = pllPermutationArrows(AlgorithmCatalog.initialState(entry)).associate { it.from to it.to }
            val cues = comparisonCues(key)
            assertEquals(8, cues.size)
            val destinations = cues.map { cue ->
                val sourceName = cue.label.split(" ")[1]
                val source = DiagramCell(sourceName[3].digitToInt() - 1, sourceName[1].digitToInt() - 1)
                val text = if (entry.number == 1) cue.first else cue.second
                val target = DiagramCell(text[3].digitToInt() - 1, text[1].digitToInt() - 1)
                assertEquals(arrows[source] ?: source, target)
                assertEquals(source == target, text.endsWith("(fixo)"))
                assertEquals(source.row != 1 && source.column != 1, cue.label.startsWith("Canto"))
                target
            }
            assertEquals(8, destinations.distinct().size)
        }
    }

    @Test fun everyPairHasTruthfulDifferencesWithoutMutatingCanonicalStates() {
        AlgorithmCategory.entries.forEach { category ->
            val entries = AlgorithmCatalog.entries(category)
            val before = entries.map { AlgorithmCatalog.initialState(it) }
            for (first in 1 until entries.size) for (second in first + 1..entries.size) {
                val cues = comparisonCues(CfopComparisonKey(category, first, second))
                assertTrue(cues.isNotEmpty())
                assertTrue("$category-$first-$second", cues.any { it.differs })
                assertEquals(cues.size, cues.map { it.label }.distinct().size)
                cues.forEach { assertEquals(it.first != it.second, it.differs) }
            }
            assertEquals(before, entries.map { AlgorithmCatalog.initialState(it) })
        }
    }
}
