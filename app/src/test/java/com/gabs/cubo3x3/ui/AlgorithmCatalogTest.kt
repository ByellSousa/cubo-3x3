package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.cube.MoveNotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmCatalogTest {
    @Test
    fun `catalogo unificado preserva contagens e identificadores unicos`() {
        assertEquals(41, AlgorithmCatalog.entries(AlgorithmCategory.F2L).size)
        assertEquals(57, AlgorithmCatalog.entries(AlgorithmCategory.OLL).size)
        assertEquals(21, AlgorithmCatalog.entries(AlgorithmCategory.PLL).size)

        val all = AlgorithmCategory.entries.flatMap(AlgorithmCatalog::entries)
        assertEquals(119, all.size)
        assertEquals(119, all.map { it.id }.toSet().size)
    }

    @Test
    fun `cada previa representa um estado nao resolvido solucionado pelo algoritmo`() {
        AlgorithmCategory.entries
            .flatMap(AlgorithmCatalog::entries)
            .forEach { entry ->
                val initialState = AlgorithmCatalog.initialState(entry)
                val solvedState = initialState.apply(
                    MoveNotation.parseAlgorithm(entry.notation),
                )

                assertFalse(
                    "${entry.id} iniciou resolvido",
                    initialState.isSolved(),
                )
                assertTrue(
                    "${entry.id} nao terminou resolvido",
                    solvedState.isSolved(),
                )
            }
    }

    @Test
    fun `setup direcionado dos 119 casos termina resolvido com o algoritmo`() {
        AlgorithmCatalog.allEntries().forEach { entry ->
            val setup = MoveNotation.parseAlgorithm(AlgorithmCatalog.setupNotation(entry))
            val solution = MoveNotation.parseAlgorithm(entry.notation)
            val initial = com.gabs.cubo3x3.cube.CubeState.solved().apply(setup)

            assertFalse("${entry.id} recebeu setup resolvido", initial.isSolved())
            assertTrue("${entry.id} nao resolveu apos o setup", initial.apply(solution).isSolved())
        }
    }

    @Test
    fun `preparacao preserva giros duplos primados e reverte sua ordem`() {
        val casesWithPrimedDoubles = AlgorithmCatalog.allEntries().filter { entry ->
            entry.notation.split(Regex("\\s+")).any { it.endsWith("2'") }
        }
        assertTrue("Regressao deve cobrir formulas originais com 2'", casesWithPrimedDoubles.isNotEmpty())
        casesWithPrimedDoubles.forEach { entry ->
            val original = entry.notation.split(Regex("\\s+")).filter { it.endsWith("2'") }
            val preparation = AlgorithmCatalog.setupNotation(entry)
                .split(Regex("\\s+")).filter { it.endsWith("2'") }
            assertEquals(entry.id, original.asReversed(), preparation)
        }
        assertEquals("R U' R'", AlgorithmCatalog.setupNotation(
            AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1),
        ))
    }
}
