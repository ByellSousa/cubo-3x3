package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmEntry
import kotlin.random.Random

enum class CfopTrainingMode { FREE, CATEGORY, CASE, COMPLETED }

/** Reusable selection, not a history of attempts or a completion mark. */
data class CfopTrainingPlan(
    val mode: CfopTrainingMode = CfopTrainingMode.FREE,
    val category: AlgorithmCategory? = null,
    val caseNumber: Int? = null,
    val completedCategories: Set<AlgorithmCategory> = AlgorithmCategory.entries.toSet(),
    val extraCaseIds: Set<String> = emptySet(),
) {
    fun validate() {
        require(completedCategories.isNotEmpty())
        require(extraCaseIds.size <= 119)
        val catalog = AlgorithmCatalog.allEntries().associateBy(AlgorithmEntry::id)
        require(extraCaseIds.all { catalog[it]?.category in completedCategories })
        when (mode) {
            CfopTrainingMode.FREE -> require(category == null && caseNumber == null)
            CfopTrainingMode.CATEGORY -> require(category != null && caseNumber == null)
            CfopTrainingMode.CASE -> require(
                category != null && AlgorithmCatalog.entries(category).any { it.number == caseNumber },
            )
            CfopTrainingMode.COMPLETED -> require(category == null && caseNumber == null)
        }
        if (mode != CfopTrainingMode.COMPLETED) {
            require(extraCaseIds.isEmpty() && completedCategories == AlgorithmCategory.entries.toSet())
        }
    }

    fun entries(progress: Map<String, AlgorithmProgress>): List<AlgorithmEntry> {
        validate()
        return when (mode) {
            CfopTrainingMode.FREE -> emptyList()
            CfopTrainingMode.CATEGORY -> AlgorithmCatalog.entries(requireNotNull(category))
            CfopTrainingMode.CASE -> listOf(
                AlgorithmCatalog.entry(requireNotNull(category), requireNotNull(caseNumber)),
            )
            CfopTrainingMode.COMPLETED -> AlgorithmCatalog.allEntries().filter {
                it.category in completedCategories &&
                    (progress[it.id]?.isCompleted == true || it.id in extraCaseIds)
            }
        }
    }
}

/** Bounded, explicit versioned preference; the same strict codec is used by backup. */
object CfopTrainingPlanCodec {
    fun encode(plan: CfopTrainingPlan): String {
        plan.validate()
        return listOf(
            "1", plan.mode.name, plan.category?.name.orEmpty(),
            (plan.caseNumber ?: 0).toString(),
            AlgorithmCategory.entries.filter { it in plan.completedCategories }.joinToString(","),
            AlgorithmCatalog.allEntries().filter { it.id in plan.extraCaseIds }
                .joinToString(",") { it.id },
        ).joinToString("|")
    }

    fun decode(value: String): CfopTrainingPlan {
        require(value.length <= 2_000)
        val fields = value.split('|')
        require(fields.size == 6 && fields[0] == "1")
        val categories = fields[4].split(',').map(AlgorithmCategory::valueOf)
        require(categories.distinct().size == categories.size)
        val extras = fields[5].takeIf(String::isNotEmpty)?.split(',').orEmpty()
        require(extras.distinct().size == extras.size)
        val number = fields[3].toInt()
        require(number >= 0)
        return CfopTrainingPlan(
            mode = CfopTrainingMode.valueOf(fields[1]),
            category = fields[2].takeIf(String::isNotEmpty)?.let(AlgorithmCategory::valueOf),
            caseNumber = number.takeIf { it > 0 },
            completedCategories = categories.toSet(),
            extraCaseIds = extras.toSet(),
        ).also(CfopTrainingPlan::validate)
    }
}

/** Frozen membership/order; previews and repeated attempts cannot inflate coverage. */
data class CfopTrainingRound(
    val caseIds: List<String>,
    val practicedIds: Set<String> = emptySet(),
) {
    init {
        require(caseIds.size <= 119 && caseIds.distinct().size == caseIds.size)
        val knownIds = AlgorithmCatalog.allEntries().map(AlgorithmEntry::id).toSet()
        require(caseIds.all { it in knownIds } && practicedIds.all { it in caseIds })
    }

    val isComplete: Boolean get() = caseIds.isNotEmpty() && practicedIds.size == caseIds.size

    fun nextCase(afterId: String? = null): String? {
        val start = (caseIds.indexOf(afterId) + 1).coerceAtLeast(0)
        return (caseIds.drop(start) + caseIds.take(start)).firstOrNull { it !in practicedIds }
    }

    fun recordAttempt(caseId: String): CfopTrainingRound =
        if (caseId in caseIds) copy(practicedIds = practicedIds + caseId) else this

    companion object {
        fun start(
            plan: CfopTrainingPlan,
            progress: Map<String, AlgorithmProgress>,
            random: Random = Random.Default,
        ): CfopTrainingRound = CfopTrainingRound(plan.entries(progress).map { it.id }.shuffled(random))
    }
}
