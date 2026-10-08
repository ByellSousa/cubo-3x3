package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test

class CfopConfusionsTest {
    private fun answer(id: String, target: Int = 1, chosen: Int = 2,
        category: String = "F2L", at: Long = 0L) =
        CfopAttempt(id, category, target, chosen, at)

    @Test fun emptyAndCorrectOnlyHistoryDoNotInventPairs() {
        assertTrue(CfopConfusions.pairs(emptyList()).isEmpty())
        assertTrue(CfopConfusions.pairs(AlgorithmCategory.entries.map {
            answer(it.name, 1, 1, it.name)
        }).isEmpty())
    }

    @Test fun bothDirectionsShareOnePairWithoutLosingTheirCounts() {
        val pair = CfopConfusions.pairs(listOf(
            answer("a", 1, 2, at = 1), answer("b", 2, 1, at = 2),
            answer("c", 1, 2, at = 3), answer("correct", 1, 1, at = 4),
        )).single()
        assertEquals("F2L-1-2", pair.key.id)
        assertEquals(2, pair.firstChosenAsSecond)
        assertEquals(1, pair.secondChosenAsFirst)
        assertEquals(3, pair.total)
        assertEquals(3L, pair.lastConfusedAtEpochMillis)
    }

    @Test fun selectingAnotherCaseDoesNotInventAnAnswerForThatCase() {
        val pair = CfopConfusions.pairs(listOf(answer("a", 3, 1))).single()
        assertEquals("F2L-1-3", pair.key.id)
        assertEquals(0, pair.firstChosenAsSecond)
        assertEquals(1, pair.secondChosenAsFirst)
    }

    @Test fun duplicateIdsCannotInflateCountsOrConsumeWindowSlots() {
        val error = answer("wrong", at = 0)
        val rows = listOf(error, error) + (1..9).flatMap {
            val correct = answer("correct-$it", 1, 1, at = it.toLong())
            listOf(correct, correct)
        }
        assertEquals(1, CfopConfusions.pairs(rows).single().total)
    }

    @Test fun tenNewCorrectAnswersRetireErrorWithoutDeletingInput() {
        val rows = listOf(answer("wrong")) + (1..10).map {
            answer("correct-$it", 1, 1, at = it.toLong())
        }
        val before = rows.toList()
        assertTrue(CfopConfusions.pairs(rows).isEmpty())
        assertEquals(before, rows)
        assertEquals(11, rows.size)
    }

    @Test fun windowIsPerTargetNotPerPairOrCategory() {
        val rows = listOf(answer("wrong-a", 1, 2), answer("wrong-b", 2, 1)) +
            (1..10).map { answer("correct-$it", 1, 1, at = it.toLong()) } +
            (1..50).map { answer("other-$it", 3, 3, at = it.toLong()) }
        val pair = CfopConfusions.pairs(rows).single()
        assertEquals(0, pair.firstChosenAsSecond)
        assertEquals(1, pair.secondChosenAsFirst)
    }

    @Test fun eachDirectionHasAtMostTenAnswersAndTotalAtMostTwenty() {
        val rows = (1..50).flatMap {
            listOf(answer("a-$it", 1, 2, at = it.toLong()),
                answer("b-$it", 2, 1, at = it.toLong()))
        }
        val pair = CfopConfusions.pairs(rows).single()
        assertEquals(10, pair.firstChosenAsSecond)
        assertEquals(10, pair.secondChosenAsFirst)
        assertEquals(20, pair.total)
    }

    @Test fun categoryIdentityAndCaseLimitsStaySeparate() {
        val rows = listOf(answer("f2l", 41, 1), answer("oll", 57, 1, "OLL"),
            answer("pll", 21, 1, "PLL"))
        assertEquals(listOf("F2L-1-41", "OLL-1-57", "PLL-1-21"),
            CfopConfusions.pairs(rows).map { it.key.id })
    }

    @Test fun priorityUsesCountThenLastErrorThenCategoryAndNumbers() {
        val rows = listOf(
            answer("frequent-a", 3, 4, at = 1), answer("frequent-b", 4, 3, at = 2),
            answer("recent", 5, 6, at = 100), answer("same-f2l", 1, 2, at = 50),
            answer("same-oll", 1, 2, "OLL", 50), answer("same-pll", 1, 2, "PLL", 50),
            answer("same-number", 1, 3, at = 50),
        )
        assertEquals(listOf("F2L-3-4", "F2L-5-6", "F2L-1-2", "F2L-1-3", "OLL-1-2", "PLL-1-2"),
            CfopConfusions.pairs(rows).map { it.key.id })
    }

    @Test fun inputOrderDoesNotChangeWindowEvenWhenDatesTie() {
        val rows = listOf(answer("a-error", at = 10)) +
            (1..10).map { answer("z-correct-$it", 1, 1, at = 10) }
        assertTrue(CfopConfusions.pairs(rows).isEmpty())
        assertEquals(CfopConfusions.pairs(rows), CfopConfusions.pairs(rows.reversed()))
    }

    @Test fun recentErrorTotalsExactlyMatchExistingErrorReviewWindow() {
        val rows = (0..30).flatMap {
            listOf(answer("f-$it", 1, if (it % 3 == 0) 2 else 1, at = it.toLong()),
                answer("r-$it", 2, if (it % 2 == 0) 1 else 2, at = it.toLong()),
                answer("o-$it", 7, if (it % 4 == 0) 8 else 7, "OLL", it.toLong()))
        }
        assertEquals(CfopReview.performance(rows).sumOf { it.recentErrors },
            CfopConfusions.pairs(rows).sumOf { it.total })
        assertEquals(CfopReview.RECENT_WINDOW, CfopConfusions.RECENT_WINDOW)
    }

    @Test fun invalidAnswersFailClosedIncludingInvalidCorrectAnswers() {
        listOf(answer("", 1, 2), answer("a", 0, 1), answer("b", 1, 42),
            answer("c", 58, 58, "OLL"), answer("d", 1, 2, "OTHER"),
            answer("e", at = -1)).forEach {
            assertThrows(IllegalArgumentException::class.java) { CfopConfusions.pairs(listOf(it)) }
        }
    }

    @Test fun everyCanonicalKeyRoundTripsAndLoadsExactCatalogEntries() {
        AlgorithmCategory.entries.forEach { category ->
            val entries = AlgorithmCatalog.entries(category)
            for (first in 1 until entries.size) for (second in first + 1..entries.size) {
                val key = CfopComparisonKey(category, first, second)
                assertEquals(key, CfopComparisonKey.fromId(key.id))
                assertEquals(entries[first - 1], key.first)
                assertEquals(entries[second - 1], key.second)
            }
        }
    }

    @Test fun malformedSavedKeysCannotOpenAnInvalidOrReversedPair() {
        listOf("", "F2L-1", "F2L-1-2-extra", "F2L-01-2", "F2L-1-1", "F2L-2-1",
            "F2L-0-2", "F2L-1-42", "OLL-1-58", "PLL-1-22", "PLL-x-2",
            "f2l-1-2", "OTHER-1-2", " F2L-1-2").forEach {
            assertNull(it, CfopComparisonKey.fromId(it))
        }
    }
}
