package com.gabs.cubo3x3.domain.training

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DailyGoalsTest {
    private val brazil = ZoneId.of("America/Sao_Paulo")
    private val now = epoch("2026-10-08T12:00:00Z")
    private fun epoch(value: String) = Instant.parse(value).toEpochMilli()
    private fun answer(id: String = "a", at: Long = now, category: String = "F2L",
        target: Int = 1, selected: Int = 1) = CfopAttempt(id, category, target, selected, at)
    private fun solve(id: Long = 1, at: Long = now, category: String? = "F2L",
        case: Int? = 1, penalty: SolvePenalty = SolvePenalty.NONE, session: Long = 1,
        duration: Long = 1_000) = SolveTime(id, duration, at, "R U2' R'", "synthetic",
            penalty, session, category, case)
    private fun progress(answers: List<CfopAttempt> = emptyList(), times: List<SolveTime> = emptyList(),
        at: Long = now, zone: ZoneId = brazil) = DailyGoals.progress(answers, times, at, zone)

    @Test fun defaultsAreOptionalAndDisablingPreservesTargets() {
        assertEquals(DailyGoalSettings(false, 5, 5), DailyGoalSettings())
        val enabled = DailyGoalSettings(true, 2, 100)
        assertEquals(DailyGoalSettings(false, 2, 100), enabled.copy(enabled = false))
        assertFalse(progress().reached(DailyGoalSettings()))
    }

    @Test fun targetLimitsAndEnabledZeroValidationAreStrict() {
        listOf(-1 to 5, 5 to -1, 101 to 5, 5 to 101).forEach { (r, e) ->
            assertThrows(IllegalArgumentException::class.java) { DailyGoalSettings(false, r, e) }
        }
        assertThrows(IllegalArgumentException::class.java) { DailyGoalSettings(true, 0, 0) }
        assertEquals(DailyGoalSettings(false, 0, 0), DailyGoalSettingsCodec.decode("1|0|0|0"))
        assertEquals(100, DailyGoalSettings(true, 100, 0).recognitionTarget)
    }

    @Test fun canonicalCodecRoundTripsEveryAllowedTargetPair() {
        (0..100).forEach { r -> (0..100).forEach { e ->
            listOf(false, true).filter { !it || r + e > 0 }.forEach { enabled ->
                val settings = DailyGoalSettings(enabled, r, e)
                assertEquals(settings, DailyGoalSettingsCodec.decode(DailyGoalSettingsCodec.encode(settings)))
            }
        } }
    }

    @Test fun codecRejectsMissingFutureNonCanonicalAndOutOfRangeValues() {
        listOf("", "2|1|5|5", "1|true|5|5", "1|2|5|5", "1|1|0|0", "1|0|-1|5",
            "1|1|101|5", "1|0|5|", "1|0|5|5|", "1|0|05|5", "1|0|+5|5",
            " 1|0|5|5", "1|0|5|5 ", "1|0|999999999999999999999999999999999999999999|5"
        ).forEach { value ->
            assertThrows(value, IllegalArgumentException::class.java) { DailyGoalSettingsCodec.decode(value) }
        }
    }

    @Test fun emptyHistoryHasZeroProgressForTheActualLocalDate() {
        assertEquals(DailyGoalProgress(LocalDate.of(2026, 10, 8), 0, 0), progress())
        assertFalse(progress().reached(DailyGoalSettings(true, 1, 1)))
    }

    @Test fun correctAndWrongAnswersAcrossStagesCountOncePerSavedId() {
        val rows = listOf(answer(), answer(), answer("wrong", selected = 2),
            answer("oll", category = "OLL", target = 57, selected = 1),
            answer("pll", category = "PLL", target = 21, selected = 21))
        assertEquals(4, progress(rows).recognitionCount)
        assertEquals(5, rows.size)
    }

    @Test fun savedAttemptsAcrossSessionsIncludePlusTwoDnfAndZeroDuration() {
        val rows = listOf(solve(), solve(), solve(2, category = "OLL", case = 57,
            penalty = SolvePenalty.PLUS_TWO, session = 3),
            solve(3, category = "PLL", case = 21, penalty = SolvePenalty.DNF, session = 4),
            solve(4, duration = 0))
        assertEquals(4, progress(times = rows).executionCount)
    }

    @Test fun freeImportedAndInvalidOriginRowsDoNotBecomeCfopPractice() {
        val rows = listOf(solve(category = null, case = null), solve(2, category = "WCA"),
            solve(3, case = null), solve(4, case = 0), solve(5, case = 42),
            solve(6, category = "OLL", case = 58), solve(7, category = "PLL", case = 22),
            solve(8, duration = -1), solve(0), solve(9, category = "f2l"),
            solve(10, category = null, case = 1), solve(11, at = -1))
        assertEquals(0, progress(times = rows).executionCount)
    }

    @Test fun todayStartsAtLocalMidnightAndIncludesNowButNeverFuture() {
        val midnight = epoch("2026-10-08T03:00:00Z")
        val stamps = listOf(midnight - 1, midnight, now, now + 1)
        assertEquals(2, progress(stamps.mapIndexed { i, at -> answer("a-$i", at) },
            stamps.mapIndexed { i, at -> solve(i + 1L, at) }).recognitionCount)
        assertEquals(2, progress(times = stamps.mapIndexed { i, at -> solve(i + 1L, at) }).executionCount)
    }

    @Test fun oldAndFutureRecordsAreNotIncludedByARollingTwentyFourHourWindow() {
        val at = epoch("2026-10-08T03:01:00Z")
        val rows = listOf(answer(at = at - 120_000), answer("future", at + 1),
            answer("much-later", Long.MAX_VALUE))
        assertEquals(0, progress(rows, at = at).recognitionCount)
    }

    @Test fun midnightResetsDerivedCountersWithoutMutatingSavedHistory() {
        val midnight = epoch("2026-10-09T03:00:00Z")
        val rows = listOf(answer(at = midnight - 1))
        val times = listOf(solve(at = midnight - 1))
        assertEquals(1, progress(rows, times, midnight - 1).recognitionCount)
        assertEquals(DailyGoalProgress(LocalDate.of(2026, 10, 9), 0, 0), progress(rows, times, midnight))
        assertEquals(midnight - 1, rows.single().recordedAtEpochMillis)
    }

    @Test fun changingZoneRecalculatesLocalDateWithoutRewritingTimestamps() {
        val at = epoch("2026-10-09T01:00:00Z")
        val rows = listOf(answer(at = epoch("2026-10-08T22:00:00Z")))
        assertEquals(1, progress(rows, at = at).recognitionCount)
        assertEquals(0, progress(rows, at = at, zone = ZoneId.of("UTC")).recognitionCount)
        assertEquals(LocalDate.of(2026, 10, 8), progress(rows, at = at).date)
    }

    @Test fun springDstDayIsTwentyThreeHoursAndFallDayTwentyFiveHours() {
        val zone = ZoneId.of("America/New_York")
        listOf(LocalDate.of(2026, 3, 8) to 23, LocalDate.of(2026, 11, 1) to 25).forEach { (day, hours) ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            assertEquals(hours * 3_600_000L, end - start)
            val rows = listOf(answer("before", start - 1), answer("start", start),
                answer("end", end - 1), answer("tomorrow", end))
            assertEquals(2, progress(rows, at = end - 1, zone = zone).recognitionCount)
        }
    }

    @Test fun activeCountersAreIndependentAndRawCountsCanExceedTargets() {
        val value = progress((1..3).map { answer("a-$it") }, listOf(solve()))
        assertTrue(value.reached(DailyGoalSettings(true, 2, 1)))
        assertTrue(value.reached(DailyGoalSettings(true, 0, 1)))
        assertTrue(value.reached(DailyGoalSettings(true, 3, 0)))
        assertFalse(value.reached(DailyGoalSettings(true, 4, 1)))
        assertFalse(value.reached(DailyGoalSettings(false, 0, 0)))
        assertEquals(3, value.recognitionCount)
    }

    @Test fun deletingOrRestoringHistoryRecalculatesInsteadOfUsingStaleCounters() {
        assertTrue(progress(listOf(answer()), listOf(solve())).reached(DailyGoalSettings(true, 1, 1)))
        assertFalse(progress(listOf(answer())).reached(DailyGoalSettings(true, 1, 1)))
        assertEquals(0, progress().recognitionCount)
        assertEquals(1, progress(listOf(answer())).recognitionCount)
    }

    @Test fun invalidCfopHistoryAndNegativeClockAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { progress(listOf(answer(category = "OTHER"))) }
        assertThrows(IllegalArgumentException::class.java) { progress(at = -1) }
    }
}
