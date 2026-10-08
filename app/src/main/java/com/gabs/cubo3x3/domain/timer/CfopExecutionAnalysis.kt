package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.data.timer.effectiveDurationMillis
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmEntry

data class CfopExecutionPerformance(
    val entry: AlgorithmEntry,
    val attemptCount: Int,
    val dnfCount: Int,
    val bestMillis: Long?,
    val averageValidMillis: Long?,
    val latestResult: AverageResult,
    val evolution: List<AnalysisPoint>,
)

/** Execution only: no recognition quiz scores, invented times or separate persisted aggregates. */
object CfopExecutionAnalysis {
    fun calculate(solves: List<SolveTime>): List<CfopExecutionPerformance> {
        val entries = AlgorithmCatalog.allEntries().associateBy { it.id }
        return solves.distinctBy { it.id }
            .filter { it.trainingCategory != null && it.trainingCaseNumber != null }
            .groupBy { "${it.trainingCategory}-${it.trainingCaseNumber}" }
            .mapNotNull { (caseId, history) ->
                val entry = entries[caseId] ?: return@mapNotNull null
                val ordered = history.sortedWith(
                    compareByDescending<SolveTime> { it.recordedAtEpochMillis }.thenByDescending { it.id },
                )
                val valid = ordered.mapNotNull(SolveTime::effectiveDurationMillis)
                CfopExecutionPerformance(
                    entry = entry,
                    attemptCount = ordered.size,
                    dnfCount = ordered.count { it.penalty == SolvePenalty.DNF },
                    bestMillis = valid.minOrNull(),
                    averageValidMillis = valid.takeIf { it.isNotEmpty() }?.average()?.toLong(),
                    latestResult = AverageResult(ordered.first().effectiveDurationMillis,
                        ordered.first().penalty == SolvePenalty.DNF),
                    evolution = ordered.asReversed().mapNotNull {
                        it.effectiveDurationMillis?.let { duration ->
                            AnalysisPoint(it.recordedAtEpochMillis, duration)
                        }
                    },
                )
            }
            .sortedWith(compareBy<CfopExecutionPerformance> { it.entry.category.ordinal }
                .thenBy { it.entry.number })
    }
}
