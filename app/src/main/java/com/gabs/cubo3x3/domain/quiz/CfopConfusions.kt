package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmEntry

data class CfopComparisonKey(
    val category: AlgorithmCategory,
    val firstCaseNumber: Int,
    val secondCaseNumber: Int,
) {
    init {
        val maximum = AlgorithmCatalog.entries(category).size
        require(firstCaseNumber in 1..maximum && secondCaseNumber in 1..maximum)
        require(firstCaseNumber < secondCaseNumber)
    }
    val id: String get() = "${category.name}-$firstCaseNumber-$secondCaseNumber"
    val first: AlgorithmEntry get() = AlgorithmCatalog.entry(category, firstCaseNumber)
    val second: AlgorithmEntry get() = AlgorithmCatalog.entry(category, secondCaseNumber)

    companion object {
        fun fromId(id: String): CfopComparisonKey? = runCatching {
            val parts = id.split("-")
            require(parts.size == 3)
            CfopComparisonKey(AlgorithmCategory.valueOf(parts[0]), parts[1].toInt(), parts[2].toInt())
                .also { require(it.id == id) }
        }.getOrNull()
    }
}

data class CfopConfusionPair(
    val key: CfopComparisonKey,
    val firstChosenAsSecond: Int,
    val secondChosenAsFirst: Int,
    val lastConfusedAtEpochMillis: Long,
) {
    val total: Int get() = firstChosenAsSecond + secondChosenAsFirst
}

object CfopConfusions {
    const val RECENT_WINDOW = CfopReview.RECENT_WINDOW

    fun pairs(attempts: List<CfopAttempt>): List<CfopConfusionPair> {
        attempts.forEach { it.validate() }
        val recentErrors = attempts.distinctBy { it.id }.groupBy { it.caseId }.values.flatMap { history ->
            // Include correct answers in the window before filtering: old errors must age out.
            history.sortedWith(compareBy<CfopAttempt> { it.recordedAtEpochMillis }.thenBy { it.id })
                .takeLast(RECENT_WINDOW).filterNot { it.isCorrect }
        }
        return recentErrors.groupBy {
            CfopComparisonKey(AlgorithmCategory.valueOf(it.category),
                minOf(it.caseNumber, it.selectedCaseNumber), maxOf(it.caseNumber, it.selectedCaseNumber))
        }.map { (key, history) ->
            CfopConfusionPair(
                key, history.count { it.caseNumber == key.firstCaseNumber },
                history.count { it.caseNumber == key.secondCaseNumber },
                history.maxOf { it.recordedAtEpochMillis },
            )
        }.sortedWith(compareByDescending<CfopConfusionPair> { it.total }
            .thenByDescending { it.lastConfusedAtEpochMillis }
            .thenBy { it.key.category.ordinal }
            .thenBy { it.key.firstCaseNumber }
            .thenBy { it.key.secondCaseNumber })
    }
}
