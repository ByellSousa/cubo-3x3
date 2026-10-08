package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test

class CfopSpacedReviewTest {
    private val day = CfopSpacedReview.DAY_MILLIS
    private fun answer(id: Int, case: Int = 1, correct: Boolean = true, at: Long = 0L,
        category: String = "F2L") =
        CfopAttempt("answer-%03d".format(id), category, case,
            if (correct) case else if (case == 1) 2 else 1, at)
    private fun agenda(rows: List<CfopAttempt>) = CfopSpacedReview.schedule(rows)
    private fun due(rows: List<CfopAttempt>, now: Long) =
        CfopSpacedReview.due(agenda(rows), AlgorithmCategory.F2L, now)

    @Test fun noHistoryDoesNotInventAgendaOrCases() {
        assertTrue(agenda(emptyList()).isEmpty())
        AlgorithmCategory.entries.forEach {
            assertTrue(CfopSpacedReview.due(emptyList(), it, 0).isEmpty())
            assertTrue(CfopSpacedReview.upcoming(emptyList(), it, 0).isEmpty())
        }
    }

    @Test fun firstCorrectSchedulesOneDayWithInclusiveDeadline() {
        val rows = listOf(answer(1, at = 1234))
        val schedule = agenda(rows).single()
        assertEquals(1, schedule.successfulReviews)
        assertEquals(1, schedule.intervalDays)
        assertEquals(day + 1234, schedule.dueAtEpochMillis)
        assertTrue(due(rows, day + 1233).isEmpty())
        assertEquals(1, due(rows, day + 1234).size)
        assertEquals(1, due(rows, day + 1235).size)
    }

    @Test fun wrongAnswerIsImmediatelyDueWithoutSuccessfulLevel() {
        val rows = listOf(answer(1, correct = false, at = 1234))
        val schedule = agenda(rows).single()
        assertEquals(0, schedule.successfulReviews)
        assertEquals(0, schedule.intervalDays)
        assertFalse(schedule.lastAnswerCorrect)
        assertTrue(due(rows, 1233).isEmpty())
        assertEquals(1, due(rows, 1234).size)
    }

    @Test fun dueCorrectReviewsAdvanceIntervalsAndCapAtThirtyDays() {
        val rows = mutableListOf<CfopAttempt>()
        var now = 1L
        listOf(1, 3, 7, 14, 30, 30, 30).forEachIndexed { index, days ->
            rows += answer(index + 1, at = now)
            val schedule = agenda(rows).single()
            assertEquals(days, schedule.intervalDays)
            assertEquals(minOf(index + 1, 5), schedule.successfulReviews)
            assertEquals(now + days * day, schedule.dueAtEpochMillis)
            assertTrue(due(rows, schedule.dueAtEpochMillis - 1).isEmpty())
            now = schedule.dueAtEpochMillis
        }
    }

    @Test fun rapidCorrectRepetitionsDoNotAdvanceOrPostponeDeadline() {
        val rows = (1..30).map { answer(it, at = it.toLong()) }
        val schedule = agenda(rows).single()
        assertEquals(1, schedule.successfulReviews)
        assertEquals(day + 1, schedule.dueAtEpochMillis)
        assertEquals(30, schedule.performance.attempts)
        assertEquals(30L, schedule.performance.lastAnsweredAtEpochMillis)
    }

    @Test fun earlyCorrectAfterLongerIntervalDoesNotDelayOrAdvanceIt() {
        val rows = listOf(answer(1), answer(2, at = day), answer(3, at = day * 3))
        val schedule = agenda(rows).single()
        assertEquals(3, schedule.intervalDays)
        assertEquals(day * 4, schedule.dueAtEpochMillis)
    }

    @Test fun simultaneousCorrectAnswersAtDeadlineAdvanceOnlyOnce() {
        val rows = listOf(answer(1)) + (2..15).map { answer(it, at = day) }
        val schedule = agenda(rows).single()
        assertEquals(2, schedule.successfulReviews)
        assertEquals(day * 4, schedule.dueAtEpochMillis)
        assertEquals(15, schedule.performance.attempts)
    }

    @Test fun earlyErrorResetsEvenLongIntervalAndKeepsAllHistory() {
        val rows = listOf(answer(1), answer(2, at = day), answer(3, at = day * 4),
            answer(4, correct = false, at = day * 5))
        val schedule = agenda(rows).single()
        assertEquals(0, schedule.intervalDays)
        assertEquals(day * 5, schedule.dueAtEpochMillis)
        assertEquals(4, schedule.performance.attempts)
        assertEquals(3, schedule.performance.correct)
        assertEquals(1, due(rows, day * 5).size)
    }

    @Test fun correctAfterErrorRestartsAtOneDayNotPriorLevel() {
        val rows = listOf(answer(1), answer(2, at = day), answer(3, correct = false, at = day * 2),
            answer(4, at = day * 2 + 100))
        val schedule = agenda(rows).single()
        assertEquals(1, schedule.successfulReviews)
        assertEquals(day * 3 + 100, schedule.dueAtEpochMillis)
        assertEquals(1, schedule.performance.recentErrors)
        assertTrue(due(rows, day * 2 + 100).isEmpty())
        // Original error review remains independently available before the new deadline.
        assertEquals(1, CfopReview.queue(rows, AlgorithmCategory.F2L).size)
    }

    @Test fun duplicateAnswerIdCannotInflateScheduleOrStatistics() {
        val rows = listOf(answer(1), answer(2, at = day))
        assertEquals(agenda(rows), agenda(rows + rows))
    }

    @Test fun chronologyUsesTimestampNotInputOrder() {
        val rows = listOf(answer(1), answer(2, at = day), answer(3, correct = false, at = day * 2),
            answer(4, at = day * 3))
        assertEquals(agenda(rows), agenda(rows.reversed()))
    }

    @Test fun simultaneousConflictingAnswersUseStableIdOrder() {
        val wrong = answer(1, correct = false)
        val correct = answer(2)
        assertEquals(agenda(listOf(wrong, correct)), agenda(listOf(correct, wrong)))
        assertEquals(day, agenda(listOf(correct, wrong)).single().dueAtEpochMillis)
        assertTrue(agenda(listOf(correct, wrong)).single().lastAnswerCorrect)
    }

    @Test fun categoriesAndTargetCasesNeverMixWithWrongOption() {
        val rows = listOf(answer(1, 41, correct = false),
            answer(2, 57, at = day, category = "OLL"),
            answer(3, 21, at = 0, category = "PLL"))
        val schedules = agenda(rows)
        assertEquals(listOf("F2L-41", "OLL-57", "PLL-21"),
            schedules.map { it.performance.entry.id })
        assertEquals(listOf("F2L-41"), CfopSpacedReview.due(schedules, AlgorithmCategory.F2L, day)
            .map { it.performance.entry.id })
        assertTrue(CfopSpacedReview.due(schedules, AlgorithmCategory.OLL, day).isEmpty())
        assertEquals(1, CfopSpacedReview.due(schedules, AlgorithmCategory.PLL, day).size)
    }

    @Test fun latestErrorHasPriorityThenRecentErrorRateAndCount() {
        val correctedButDifficult = (1..8).map { answer(it, 1, correct = false, at = it.toLong()) } +
            answer(9, 1, at = 9)
        val recentError = (10..18).map { answer(it, 2, at = it.toLong()) } +
            answer(19, 2, correct = false, at = 19)
        val fullyWrong = listOf(answer(20, 3, correct = false, at = 20))
        assertEquals(listOf(3, 2, 1), due(correctedButDifficult + recentError + fullyWrong, day * 2)
            .map { it.performance.entry.number })
    }

    @Test fun equalErrorPriorityPrefersOlderDeadlineThenCaseNumber() {
        val rows = listOf(answer(1, 5, correct = false, at = 100),
            answer(2, 2, correct = false, at = 100),
            answer(3, 3, correct = false, at = 50))
        assertEquals(listOf(3, 2, 5), due(rows, day).map { it.performance.entry.number })
    }

    @Test fun allCorrectCasesStillReturnToReviewByAge() {
        val rows = listOf(answer(1, 2, at = 100), answer(2, 1, at = 50))
        assertTrue(CfopReview.queue(rows, AlgorithmCategory.F2L).isEmpty())
        assertEquals(listOf(1, 2), due(rows, day + 100).map { it.performance.entry.number })
    }

    @Test fun upcomingIsSortedByDeadlineAndFilteredByCategory() {
        val rows = listOf(answer(1, 5, at = 100), answer(2, 2, at = 100),
            answer(3, 3, at = 50), answer(4, 1, category = "OLL"))
        assertEquals(listOf(3, 2, 5),
            CfopSpacedReview.upcoming(agenda(rows), AlgorithmCategory.F2L, 100)
                .map { it.performance.entry.number })
        assertTrue(CfopSpacedReview.upcoming(agenda(rows), AlgorithmCategory.F2L, day + 100).isEmpty())
    }

    @Test fun clockRollbackChangesDueClassificationWithoutRewritingAgenda() {
        val rows = listOf(answer(1, at = 100))
        val schedules = agenda(rows)
        assertEquals(1, CfopSpacedReview.due(schedules, AlgorithmCategory.F2L, day + 100).size)
        assertTrue(CfopSpacedReview.due(schedules, AlgorithmCategory.F2L, 100).isEmpty())
        assertEquals(1, CfopSpacedReview.upcoming(schedules, AlgorithmCategory.F2L, 100).size)
        assertEquals(schedules, agenda(rows))
        assertEquals(100L, rows.single().recordedAtEpochMillis)
    }

    @Test fun dayIsElapsedTwentyFourHoursNotCalendarMidnightOrTimezone() {
        val instant = java.time.Instant.parse("2026-10-07T23:30:00Z").toEpochMilli()
        assertEquals("2026-10-08T23:30:00Z",
            java.time.Instant.ofEpochMilli(agenda(listOf(answer(1, at = instant)))
                .single().dueAtEpochMillis).toString())
    }

    @Test fun deadlineAdditionSaturatesInsteadOfOverflowingIntoPast() {
        val rows = listOf(answer(1, at = Long.MAX_VALUE - 1))
        assertEquals(Long.MAX_VALUE, agenda(rows).single().dueAtEpochMillis)
        assertTrue(due(rows, Long.MAX_VALUE - 1).isEmpty())
        assertEquals(1, due(rows, Long.MAX_VALUE).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeClockRatherThanInventingPastDeadline() {
        due(listOf(answer(1)), -1)
    }
}
