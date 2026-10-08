package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAnalysisTest {
    @Test
    fun periodFilterIncludesBoundaryAndLeavesSourceUntouched() {
        val day = 24L * 60L * 60L * 1_000L
        val now = 100L * day
        val solves = listOf(
            solve(1, now, 10_000L),
            solve(2, now - 7L * day, 11_000L),
            solve(3, now - 7L * day - 1L, 12_000L),
        )

        val filtered = TimerAnalysisCalculator.filterByPeriod(
            solves,
            AnalysisPeriod.LAST_7_DAYS,
            now,
        )

        assertEquals(listOf(1L, 2L), filtered.map(SolveTime::id))
        assertEquals(3, solves.size)
    }

    @Test
    fun buildsChronologicalSeriesAndExcludesDnfFromCharts() {
        val solves = (0 until 12).map { index ->
            solve(
                id = index.toLong() + 1L,
                recordedAt = 12_000L - index,
                duration = 10_000L + index * 100L,
                penalty = if (index == 5) SolvePenalty.DNF else SolvePenalty.NONE,
            )
        }

        val analysis = TimerAnalysisCalculator.calculate(
            solves,
            AnalysisPeriod.ALL,
            nowEpochMillis = 20_000L,
        )

        assertEquals(11, analysis.evolution.size)
        assertTrue(
            analysis.evolution.zipWithNext().all { (first, second) ->
                first.recordedAtEpochMillis <= second.recordedAtEpochMillis
            },
        )
        assertEquals(11, analysis.distribution.sumOf(DistributionBucket::count))
        assertEquals(1, analysis.consistency.size)
    }

    @Test
    fun distributionUsesOnlyValidRangesForNarrowDurations() {
        val analysis = TimerAnalysisCalculator.calculate(
            listOf(
                solve(1, 2_000L, 10_001L),
                solve(2, 1_000L, 10_000L),
            ),
            AnalysisPeriod.ALL,
            nowEpochMillis = 3_000L,
        )

        assertEquals(2, analysis.distribution.size)
        assertTrue(
            analysis.distribution.all { bucket ->
                bucket.minimumMillis <= bucket.maximumMillis
            },
        )
        assertEquals(2, analysis.distribution.sumOf(DistributionBucket::count))
    }

    private fun solve(
        id: Long,
        recordedAt: Long,
        duration: Long,
        penalty: SolvePenalty = SolvePenalty.NONE,
    ) = SolveTime(
        id = id,
        durationMillis = duration,
        recordedAtEpochMillis = recordedAt,
        scramble = "R U R'",
        comment = "",
        penalty = penalty,
    )
}
