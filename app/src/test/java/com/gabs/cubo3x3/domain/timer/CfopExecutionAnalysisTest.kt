package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import org.junit.Assert.*
import org.junit.Test

class CfopExecutionAnalysisTest {
    private fun solve(id: Long, category: String? = "F2L", case: Int? = 1,
        duration: Long = 1_000, penalty: SolvePenalty = SolvePenalty.NONE, at: Long = id) =
        SolveTime(id, duration, at, "R U R'", "", penalty, trainingCategory = category,
            trainingCaseNumber = case)

    @Test fun emptyOrFreeHistoryDoesNotInventExecutionStats() {
        assertTrue(CfopExecutionAnalysis.calculate(emptyList()).isEmpty())
        assertTrue(CfopExecutionAnalysis.calculate(listOf(solve(1, null, null))).isEmpty())
    }

    @Test fun separatesCategoriesAndNumbersWithStableCatalogOrder() {
        val results = CfopExecutionAnalysis.calculate(listOf(
            solve(1, "PLL", 1), solve(2, "F2L", 2), solve(3, "OLL", 1), solve(4), solve(5),
        ))
        assertEquals(listOf("F2L-1", "F2L-2", "OLL-1", "PLL-1"), results.map { it.entry.id })
        assertEquals(listOf(2, 1, 1, 1), results.map { it.attemptCount })
    }

    @Test fun penaltiesIncludedAndDnfExcludedFromMeanButCounted() {
        val result = CfopExecutionAnalysis.calculate(listOf(
            solve(1), solve(2, duration = 1_000, penalty = SolvePenalty.PLUS_TWO),
            solve(3, duration = 500, penalty = SolvePenalty.DNF),
        )).single()
        assertEquals(3, result.attemptCount)
        assertEquals(1, result.dnfCount)
        assertEquals(1_000L, result.bestMillis)
        assertEquals(2_000L, result.averageValidMillis)
        assertTrue(result.latestResult.isDnf)
        assertEquals(listOf(1_000L, 3_000L), result.evolution.map { it.durationMillis })
    }

    @Test fun allDnfKeepsAttemptsWithoutNumericStatsOrGraph() {
        val result = CfopExecutionAnalysis.calculate(listOf(
            solve(1, penalty = SolvePenalty.DNF), solve(2, penalty = SolvePenalty.DNF),
        )).single()
        assertEquals(2, result.attemptCount)
        assertEquals(2, result.dnfCount)
        assertNull(result.bestMillis)
        assertNull(result.averageValidMillis)
        assertTrue(result.evolution.isEmpty())
    }

    @Test fun unsortedRowsUseChronologicalLatestAndDeduplicateIds() {
        val rows = listOf(solve(2, at = 100, duration = 3_000), solve(1, at = 99),
            solve(3, at = 100, duration = 5_000))
        val result = CfopExecutionAnalysis.calculate(rows + rows).single()
        assertEquals(3, result.attemptCount)
        assertEquals(5_000L, result.latestResult.durationMillis)
        assertEquals(listOf(1_000L, 3_000L, 5_000L), result.evolution.map { it.durationMillis })
    }

    @Test fun invalidOrIncompleteLinksAreIgnoredNotMergedIntoRealCase() {
        val result = CfopExecutionAnalysis.calculate(listOf(solve(1), solve(2, "F2L", 42),
            solve(3, "PLL", 22), solve(4, "INVALID", 1), solve(5, null, 1), solve(6, "OLL", null)))
        assertEquals(listOf("F2L-1"), result.map { it.entry.id })
        assertEquals(1, result.single().attemptCount)
    }

    @Test fun usesOnlyProvidedSessionAndPeriodAndDoesNotChangeRawHistory() {
        val rows = listOf(solve(1, at = 0), solve(2, at = 86_400_000L * 9),
            solve(3, "OLL", 1, at = 86_400_000L * 10))
        val filtered = TimerAnalysisCalculator.filterByPeriod(rows,
            AnalysisPeriod.LAST_7_DAYS, 86_400_000L * 10)
        val result = CfopExecutionAnalysis.calculate(filtered)
        assertEquals(listOf(1, 1), result.map { it.attemptCount })
        assertEquals(3, rows.size)
    }
}
