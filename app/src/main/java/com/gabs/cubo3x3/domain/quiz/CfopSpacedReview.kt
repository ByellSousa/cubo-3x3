package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCategory

data class CfopReviewSchedule(
    val performance: CfopCasePerformance,
    val successfulReviews: Int,
    val intervalDays: Int,
    val dueAtEpochMillis: Long,
    val lastAnswerCorrect: Boolean,
) {
    fun isDue(nowEpochMillis: Long): Boolean = dueAtEpochMillis <= nowEpochMillis
}

/** A derived agenda: no second persisted state to drift from the answer history. */
object CfopSpacedReview {
    const val DAY_MILLIS = 86_400_000L
    private val intervals = listOf(1, 3, 7, 14, 30)

    fun schedule(attempts: List<CfopAttempt>): List<CfopReviewSchedule> {
        val unique = attempts.distinctBy { it.id }
        val histories = unique.groupBy { it.caseId }
        return CfopReview.performance(unique).map { performance ->
            val history = histories.getValue(performance.entry.id)
                .sortedWith(compareBy<CfopAttempt> { it.recordedAtEpochMillis }.thenBy { it.id })
            var level = 0
            var dueAt = 0L
            history.forEach { answer ->
                if (!answer.isCorrect) {
                    level = 0
                    dueAt = answer.recordedAtEpochMillis
                } else if (level == 0 || answer.recordedAtEpochMillis >= dueAt) {
                    // An early successful repetition never advances or postpones a review.
                    level = (level + 1).coerceAtMost(intervals.size)
                    val duration = intervals[level - 1] * DAY_MILLIS
                    dueAt = if (answer.recordedAtEpochMillis > Long.MAX_VALUE - duration)
                        Long.MAX_VALUE else answer.recordedAtEpochMillis + duration
                }
            }
            CfopReviewSchedule(performance, level, if (level == 0) 0 else intervals[level - 1],
                dueAt, history.last().isCorrect)
        }
    }

    fun due(
        schedules: List<CfopReviewSchedule>,
        category: AlgorithmCategory,
        nowEpochMillis: Long,
    ): List<CfopReviewSchedule> {
        require(nowEpochMillis >= 0L)
        return schedules.filter { it.performance.entry.category == category && it.isDue(nowEpochMillis) }
            .sortedWith(compareByDescending<CfopReviewSchedule> { !it.lastAnswerCorrect }
                .thenByDescending { it.performance.recentErrorRate }
                .thenByDescending { it.performance.recentErrors }
                .thenBy { it.dueAtEpochMillis }
                .thenBy { it.performance.entry.number })
    }

    fun upcoming(
        schedules: List<CfopReviewSchedule>,
        category: AlgorithmCategory,
        nowEpochMillis: Long,
    ): List<CfopReviewSchedule> {
        require(nowEpochMillis >= 0L)
        return schedules.filter { it.performance.entry.category == category && !it.isDue(nowEpochMillis) }
            .sortedWith(compareBy<CfopReviewSchedule> { it.dueAtEpochMillis }
                .thenBy { it.performance.entry.number })
    }
}
