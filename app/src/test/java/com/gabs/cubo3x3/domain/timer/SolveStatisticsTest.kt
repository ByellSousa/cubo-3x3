package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolveStatisticsTest {
    @Test
    fun calculatesCurrentCompetitionAverages() {
        val solves = (10..21).mapIndexed { index, seconds -> solve(index, seconds * 1_000L) }

        val statistics = SolveStatisticsCalculator.calculate(solves)

        assertEquals(11_000L, statistics.currentMo3.durationMillis)
        assertEquals(12_000L, statistics.currentAo5.durationMillis)
        assertEquals(15_500L, statistics.currentAo12.durationMillis)
    }

    @Test
    fun plusTwoCountsAndSingleDnfIsDiscardedFromTrimmedAverage() {
        val solves = listOf(
            solve(0, 8_000L, SolvePenalty.PLUS_TWO),
            solve(1, 11_000L),
            solve(2, 12_000L),
            solve(3, 13_000L),
            solve(4, 14_000L, SolvePenalty.DNF),
        )

        val average = SolveStatisticsCalculator.windowAt(solves, 0, 5, trimmed = true)

        assertEquals(12_000L, average.durationMillis)
        assertFalse(average.isDnf)
    }

    @Test
    fun twoDnfsMakeTrimmedAverageDnf() {
        val solves = listOf(
            solve(0, 10_000L, SolvePenalty.DNF),
            solve(1, 11_000L),
            solve(2, 12_000L),
            solve(3, 13_000L),
            solve(4, 14_000L, SolvePenalty.DNF),
        )

        val average = SolveStatisticsCalculator.windowAt(solves, 0, 5, trimmed = true)

        assertTrue(average.isDnf)
        assertEquals(null, average.durationMillis)
    }

    @Test
    fun ao100DiscardsFiveBestAndFiveWorstResults() {
        val solves = (1..100).mapIndexed { index, seconds ->
            solve(index, seconds * 1_000L)
        }

        val statistics = SolveStatisticsCalculator.calculate(solves)

        assertEquals(50_500L, statistics.currentAo100.durationMillis)
        assertEquals(50_500L, statistics.bestAo100.durationMillis)
    }

    @Test
    fun ao100AllowsFiveDnfsButSixMakeItDnf() {
        val fiveDnfs = (1..100).mapIndexed { index, seconds ->
            solve(
                index,
                seconds * 1_000L,
                if (index < 5) SolvePenalty.DNF else SolvePenalty.NONE,
            )
        }
        val sixDnfs = fiveDnfs.mapIndexed { index, solve ->
            if (index == 5) solve.copy(penalty = SolvePenalty.DNF) else solve
        }

        assertFalse(
            SolveStatisticsCalculator.windowAt(fiveDnfs, 0, 100, trimmed = true).isDnf,
        )
        assertTrue(
            SolveStatisticsCalculator.windowAt(sixDnfs, 0, 100, trimmed = true).isDnf,
        )
    }

    private fun solve(index: Int, duration: Long, penalty: SolvePenalty = SolvePenalty.NONE) =
        SolveTime(
            id = index.toLong() + 1,
            durationMillis = duration,
            recordedAtEpochMillis = 1_000L - index,
            scramble = "R U R'",
            comment = "",
            penalty = penalty,
        )
}
