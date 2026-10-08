package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.data.timer.effectiveDurationMillis

data class AverageResult(
    val durationMillis: Long? = null,
    val isDnf: Boolean = false,
) {
    val isAvailable: Boolean get() = durationMillis != null || isDnf
}

data class SolveStatistics(
    val currentTime: AverageResult,
    val bestTime: AverageResult,
    val currentMo3: AverageResult,
    val bestMo3: AverageResult,
    val currentAo5: AverageResult,
    val bestAo5: AverageResult,
    val currentAo12: AverageResult,
    val bestAo12: AverageResult,
    val currentAo100: AverageResult,
    val bestAo100: AverageResult,
)

object SolveStatisticsCalculator {
    fun calculate(solvesNewestFirst: List<SolveTime>): SolveStatistics = SolveStatistics(
        currentTime = solvesNewestFirst.firstOrNull().toResult(),
        bestTime = solvesNewestFirst.mapNotNull(SolveTime::effectiveDurationMillis)
            .minOrNull()
            .toResult(),
        currentMo3 = windowAt(solvesNewestFirst, 0, 3, trimmed = false),
        bestMo3 = bestWindow(solvesNewestFirst, 3, trimmed = false),
        currentAo5 = windowAt(solvesNewestFirst, 0, 5, trimmed = true),
        bestAo5 = bestWindow(solvesNewestFirst, 5, trimmed = true),
        currentAo12 = windowAt(solvesNewestFirst, 0, 12, trimmed = true),
        bestAo12 = bestWindow(solvesNewestFirst, 12, trimmed = true),
        currentAo100 = windowAt(solvesNewestFirst, 0, 100, trimmed = true),
        bestAo100 = bestWindow(solvesNewestFirst, 100, trimmed = true),
    )

    fun windowAt(
        solvesNewestFirst: List<SolveTime>,
        startIndex: Int,
        size: Int,
        trimmed: Boolean,
    ): AverageResult {
        if (startIndex < 0 || size <= 0 || startIndex + size > solvesNewestFirst.size) {
            return AverageResult()
        }
        val window = solvesNewestFirst.subList(startIndex, startIndex + size)
        val values = window.map(SolveTime::effectiveDurationMillis)
        if (!trimmed) {
            if (values.any { it == null }) return AverageResult(isDnf = true)
            return AverageResult(values.filterNotNull().averageRounded())
        }
        val trimCount = ((size + 19) / 20).coerceAtLeast(1)
        val dnfCount = values.count { it == null }
        if (dnfCount > trimCount) return AverageResult(isDnf = true)
        val numeric = values.filterNotNull().sorted()
        val included = numeric
            .drop(trimCount)
            .dropLast(trimCount - dnfCount)
        if (included.isEmpty()) return AverageResult()
        return AverageResult(included.averageRounded())
    }

    private fun bestWindow(
        solvesNewestFirst: List<SolveTime>,
        size: Int,
        trimmed: Boolean,
    ): AverageResult {
        val numeric = (0..(solvesNewestFirst.size - size).coerceAtLeast(-1))
            .map { windowAt(solvesNewestFirst, it, size, trimmed) }
            .mapNotNull(AverageResult::durationMillis)
        return AverageResult(numeric.minOrNull())
    }

    private fun List<Long>.averageRounded(): Long = (sum().toDouble() / size).toLong()

    private fun SolveTime?.toResult(): AverageResult = AverageResult(
        durationMillis = this?.effectiveDurationMillis,
        isDnf = this != null && effectiveDurationMillis == null,
    )

    private fun Long?.toResult(): AverageResult = AverageResult(durationMillis = this)
}
