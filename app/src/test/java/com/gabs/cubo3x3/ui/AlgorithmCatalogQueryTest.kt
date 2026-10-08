package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmCatalogQueryTest {
    private val entries = listOf(
        AlgorithmEntry(AlgorithmCategory.F2L, 1, "R U R'"),
        AlgorithmEntry(AlgorithmCategory.F2L, 11, "r U2' r'"),
        AlgorithmEntry(AlgorithmCategory.F2L, 2, "R2 U2 R2'"),
        AlgorithmEntry(AlgorithmCategory.OLL, 1, "F R U R' U' F'"),
        AlgorithmEntry(AlgorithmCategory.PLL, 1, "L U2' L'"),
    )
    private val progress = mapOf(
        "F2L-1" to AlgorithmProgress(isFavorite = true),
        "F2L-11" to AlgorithmProgress(isFavorite = true, isCompleted = true),
        "OLL-1" to AlgorithmProgress(isCompleted = true),
        "PLL-1" to AlgorithmProgress(isFavorite = true),
    )
    private fun ids(query: String) =
        AlgorithmCatalogQuery.filter(entries, progress, query).map { it.id }

    @Test fun blankQueryKeepsOriginalOrderAndNotation() {
        assertEquals(entries, AlgorithmCatalogQuery.filter(entries, progress, "  \t "))
    }

    @Test fun eachOfThe119RealCasesCanBeFoundByItsExactIdentifierAndFormula() {
        val catalog = AlgorithmCatalog.allEntries()
        catalog.forEach { entry ->
            assertEquals(entry.id, listOf(entry), AlgorithmCatalogQuery.filter(
                catalog, emptyMap(), "${entry.category.label} caso ${entry.number}",
            ))
            assertTrue(entry.id, entry in AlgorithmCatalogQuery.filter(
                catalog, emptyMap(), entry.notation,
            ))
        }
    }

    @Test fun numberMatchesExactlyInsteadOfMatchingEleven() {
        assertEquals(listOf("F2L-1", "OLL-1", "PLL-1"), ids("1"))
        assertEquals(listOf("F2L-11"), ids("11"))
    }

    @Test fun caseLabelsCategoryHyphenAndHashAreAccepted() {
        listOf("F2L 1", "f2l-1", "F2L caso #1", "f2l cáso 1").forEach {
            assertEquals(it, listOf("F2L-1"), ids(it))
        }
        assertEquals(ids("1"), ids("Caso #1"))
    }

    @Test fun categoryAndDescriptionIgnoreAccentsAndMetadataCase() {
        assertEquals(listOf("OLL-1"), ids("oLl"))
        assertEquals(listOf("OLL-1"), ids("ORIENTAÇÃO"))
    }

    @Test fun notationMatchesWholeTokensAndDistinguishesWideMoves() {
        assertEquals(listOf("F2L-11"), ids("r"))
        assertEquals(listOf("F2L-1", "OLL-1"), ids("R"))
        assertEquals(listOf("F2L-2"), ids("R2"))
        assertEquals(listOf("F2L-1", "OLL-1"), ids("R U R'"))
    }

    @Test fun doublePrimeIsNotSilentlyNormalized() {
        assertEquals(listOf("F2L-11", "PLL-1"), ids("U2'"))
        assertEquals(listOf("F2L-2"), ids("U2"))
        assertEquals(listOf("F2L-2"), ids("R2'"))
    }

    @Test fun pastedPrimesAndWhitespaceAreAcceptedWithoutEditingEntries() {
        assertEquals(ids("R U R'"), ids("  R\t U   R′ "))
        assertEquals(ids("U2'"), ids("U2’"))
        assertEquals("r U2' r'", entries[1].notation)
    }

    @Test fun favoriteFilterIntersectsCategory() {
        val actual = AlgorithmCatalogQuery.filter(
            entries, progress, studyFilter = AlgorithmStudyFilter.FAVORITES,
            category = AlgorithmCategory.F2L,
        )
        assertEquals(listOf("F2L-1", "F2L-11"), actual.map { it.id })
    }

    @Test fun completionAndPendingHaveExplicitMissingProgressDefaults() {
        assertEquals(listOf("F2L-11", "OLL-1"), AlgorithmCatalogQuery.filter(
            entries, progress, studyFilter = AlgorithmStudyFilter.COMPLETED,
        ).map { it.id })
        assertEquals(listOf("F2L-1", "F2L-2", "PLL-1"), AlgorithmCatalogQuery.filter(
            entries, progress, studyFilter = AlgorithmStudyFilter.NOT_COMPLETED,
        ).map { it.id })
    }

    @Test fun queryAndProgressFiltersAreIntersections() {
        assertTrue(AlgorithmCatalogQuery.filter(
            entries, progress, "11", AlgorithmStudyFilter.NOT_COMPLETED,
        ).isEmpty())
    }

    @Test fun noMatchAndOutOfRangeNumbersReturnEmptyWithoutExceptions() {
        listOf("9999999999999999999999", "0", "caso 200", "inexistente", "R U3")
            .forEach { assertTrue(it, ids(it).isEmpty()) }
    }

    @Test fun progressChangesAreReflectedWithoutMutatingInput() {
        val snapshot = progress.toMap()
        assertEquals(3, AlgorithmCatalogQuery.filter(
            entries, progress, studyFilter = AlgorithmStudyFilter.FAVORITES,
        ).size)
        assertEquals(0, AlgorithmCatalogQuery.filter(
            entries, emptyMap(), studyFilter = AlgorithmStudyFilter.FAVORITES,
        ).size)
        assertEquals(snapshot, progress)
        assertEquals(entries, AlgorithmCatalogQuery.filter(entries, progress))
    }
}
