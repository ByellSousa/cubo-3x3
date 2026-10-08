package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmEntry
import kotlin.math.roundToInt

data class CfopCasePerformance(
    val entry: AlgorithmEntry,
    val attempts: Int,
    val correct: Int,
    val recentAttempts: Int,
    val recentErrors: Int,
    val lastAnsweredAtEpochMillis: Long,
) {
    val accuracyPercent: Int get() = (100.0 * correct / attempts).roundToInt()
    val recentErrorRate: Double get() = recentErrors.toDouble() / recentAttempts
}

object CfopReview {
    const val RECENT_WINDOW = 10

    fun performance(attempts: List<CfopAttempt>): List<CfopCasePerformance> =
        attempts.distinctBy { it.id }.groupBy { it.caseId }.values.map { history ->
            val sorted = history.sortedWith(compareBy<CfopAttempt> { it.recordedAtEpochMillis }.thenBy { it.id })
            val latest = sorted.last()
            val recent = sorted.takeLast(RECENT_WINDOW)
            CfopCasePerformance(
                entry = AlgorithmCatalog.entry(AlgorithmCategory.valueOf(latest.category), latest.caseNumber),
                attempts = sorted.size,
                correct = sorted.count { it.isCorrect },
                recentAttempts = recent.size,
                recentErrors = recent.count { !it.isCorrect },
                lastAnsweredAtEpochMillis = latest.recordedAtEpochMillis,
            )
        }.sortedWith(compareBy<CfopCasePerformance> { it.entry.category.ordinal }.thenBy { it.entry.number })

    fun queue(attempts: List<CfopAttempt>, category: AlgorithmCategory): List<CfopCasePerformance> =
        performance(attempts).filter { it.entry.category == category && it.recentErrors > 0 }
            .sortedWith(compareByDescending<CfopCasePerformance> { it.recentErrorRate }
                .thenByDescending { it.recentErrors }
                .thenBy { it.lastAnsweredAtEpochMillis }
                .thenBy { it.entry.number })
}
