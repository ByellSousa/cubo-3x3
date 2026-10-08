package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.data.timer.effectiveDurationMillis

enum class AnalysisPeriod(val label: String, val days: Int?) {
    ALL("Tudo", null),
    LAST_7_DAYS("7 dias", 7),
    LAST_30_DAYS("30 dias", 30),
    LAST_90_DAYS("90 dias", 90),
}

data class AnalysisPoint(
    val recordedAtEpochMillis: Long,
    val durationMillis: Long,
)

data class DistributionBucket(
    val minimumMillis: Long,
    val maximumMillis: Long,
    val count: Int,
)

data class TimerAnalysis(
    val filteredSolves: List<SolveTime>,
    val statistics: SolveStatistics,
    val evolution: List<AnalysisPoint>,
    val distribution: List<DistributionBucket>,
    val consistency: List<AnalysisPoint>,
)

object TimerAnalysisCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun calculate(
        solvesNewestFirst: List<SolveTime>,
        period: AnalysisPeriod,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): TimerAnalysis {
        val filtered = filterByPeriod(solvesNewestFirst, period, nowEpochMillis)
        return TimerAnalysis(
            filteredSolves = filtered,
            statistics = SolveStatisticsCalculator.calculate(filtered),
            evolution = filtered
                .mapNotNull { solve ->
                    solve.effectiveDurationMillis?.let { duration ->
                        AnalysisPoint(solve.recordedAtEpochMillis, duration)
                    }
                }
                .sortedBy(AnalysisPoint::recordedAtEpochMillis),
            distribution = distribution(filtered.mapNotNull(SolveTime::effectiveDurationMillis)),
            consistency = rollingAverage(filtered, size = 12),
        )
    }

    fun filterByPeriod(
        solvesNewestFirst: List<SolveTime>,
        period: AnalysisPeriod,
        nowEpochMillis: Long,
    ): List<SolveTime> {
        val days = period.days ?: return solvesNewestFirst
        val cutoff = nowEpochMillis - days * DAY_MILLIS
        return solvesNewestFirst.filter { it.recordedAtEpochMillis >= cutoff }
    }

    private fun rollingAverage(solvesNewestFirst: List<SolveTime>, size: Int): List<AnalysisPoint> =
        (0..(solvesNewestFirst.size - size).coerceAtLeast(-1))
            .mapNotNull { index ->
                SolveStatisticsCalculator.windowAt(
                    solvesNewestFirst = solvesNewestFirst,
                    startIndex = index,
                    size = size,
                    trimmed = true,
                ).durationMillis?.let { duration ->
                    AnalysisPoint(solvesNewestFirst[index].recordedAtEpochMillis, duration)
                }
            }
            .sortedBy(AnalysisPoint::recordedAtEpochMillis)

    private fun distribution(values: List<Long>, bucketCount: Int = 5): List<DistributionBucket> {
        if (values.isEmpty()) return emptyList()
        val minimum = values.min()
        val maximum = values.max()
        if (minimum == maximum) {
            return listOf(DistributionBucket(minimum, maximum, values.size))
        }
        val range = maximum - minimum + 1L
        val actualBucketCount = if (range < bucketCount) range.toInt() else bucketCount
        val width = (range + actualBucketCount - 1L) / actualBucketCount
        return (0 until actualBucketCount).map { index ->
            val lower = minimum + width * index
            val upper = if (index == actualBucketCount - 1) maximum else lower + width
            DistributionBucket(
                minimumMillis = lower,
                maximumMillis = upper,
                count = values.count { value ->
                    if (index == actualBucketCount - 1) value in lower..upper
                    else value >= lower && value < upper
                },
            )
        }
    }
}
