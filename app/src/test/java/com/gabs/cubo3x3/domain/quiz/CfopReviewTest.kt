package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test

class CfopReviewTest {
    private fun answer(id: Int, case: Int, correct: Boolean, category: String = "F2L", at: Long = id.toLong()) =
        CfopAttempt("answer-$id", category, case, if (correct) case else if (case == 1) 2 else 1, at)

    @Test fun noHistoryDoesNotInventDifficultCases() {
        assertTrue(CfopReview.performance(emptyList()).isEmpty())
        AlgorithmCategory.entries.forEach { assertTrue(CfopReview.queue(emptyList(), it).isEmpty()) }
    }

    @Test fun accuracyAndRecentErrorsAreComputedForTargetNotWrongOption() {
        val history = listOf(answer(1, 3, false), answer(2, 3, true), answer(3, 3, true))
        val result = CfopReview.performance(history).single()
        assertEquals("F2L-3", result.entry.id)
        assertEquals(3, result.attempts)
        assertEquals(2, result.correct)
        assertEquals(67, result.accuracyPercent)
        assertEquals(1, result.recentErrors)
        assertEquals(3L, result.lastAnsweredAtEpochMillis)
    }

    @Test fun duplicateIdsDoNotInflateStats() {
        val one = answer(1, 1, false)
        assertEquals(1, CfopReview.performance(listOf(one, one)).single().attempts)
    }

    @Test fun errorsAreRankedByProportionNotLifetimeVolume() {
        val halfWrong = (1..10).map { answer(it, 1, it > 5) }
        val allWrong = listOf(answer(11, 2, false))
        assertEquals(listOf(2, 1),
            CfopReview.queue(halfWrong + allWrong, AlgorithmCategory.F2L).map { it.entry.number })
    }

    @Test fun equalErrorRatePrefersMoreErrorsThenOlderLastAnswer() {
        val rows = listOf(answer(1, 1, false, at = 50), answer(2, 1, false, at = 60),
            answer(3, 2, false, at = 1), answer(4, 3, false, at = 100))
        assertEquals(listOf(1, 2, 3), CfopReview.queue(rows, AlgorithmCategory.F2L).map { it.entry.number })
    }

    @Test fun tenRecentCorrectAnswersRetireOldErrorWithoutErasingHistory() {
        val rows = listOf(answer(0, 1, false)) + (1..10).map { answer(it, 1, true) }
        assertTrue(CfopReview.queue(rows, AlgorithmCategory.F2L).isEmpty())
        val result = CfopReview.performance(rows).single()
        assertEquals(11, result.attempts)
        assertEquals(10, result.correct)
        assertEquals(10, result.recentAttempts)
        assertEquals(0, result.recentErrors)
    }

    @Test fun ninthCorrectStillKeepsRecentErrorAndNewErrorReturnsCaseToQueue() {
        val nine = listOf(answer(0, 1, false)) + (1..9).map { answer(it, 1, true) }
        assertEquals(1, CfopReview.queue(nine, AlgorithmCategory.F2L).size)
        val ten = nine + answer(10, 1, true)
        assertTrue(CfopReview.queue(ten, AlgorithmCategory.F2L).isEmpty())
        assertEquals(1, CfopReview.queue(ten + answer(11, 1, false), AlgorithmCategory.F2L).size)
    }

    @Test fun categoriesDoNotMixAndCorrectOnlyCasesAreNotDifficult() {
        val rows = listOf(answer(1, 1, true), answer(2, 2, false, "OLL"),
            answer(3, 3, false, "PLL"))
        assertTrue(CfopReview.queue(rows, AlgorithmCategory.F2L).isEmpty())
        assertEquals(listOf("OLL-2"), CfopReview.queue(rows, AlgorithmCategory.OLL).map { it.entry.id })
        assertEquals(listOf("PLL-3"), CfopReview.queue(rows, AlgorithmCategory.PLL).map { it.entry.id })
    }

    @Test fun recencyUsesDateNotInputOrder() {
        val rows = listOf(answer(0, 1, false)) + (1..10).map { answer(it, 1, true) }
        assertEquals(CfopReview.performance(rows), CfopReview.performance(rows.reversed()))
    }

    @Test fun priorityIsDeterministicWhenEverythingTies() {
        val rows = listOf(answer(1, 5, false, at = 1), answer(2, 2, false, at = 1))
        assertEquals(listOf(2, 5), CfopReview.queue(rows, AlgorithmCategory.F2L).map { it.entry.number })
    }
}
