package com.gabs.cubo3x3.domain.reminder

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeCalculatorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun returnsSameSelectedDayWhenTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 10, 5, 18, 30, 0, 0, zone)
        val settings = ReminderSettings(
            enabled = true,
            hour = 19,
            minute = 0,
            days = setOf(ReminderDay.MONDAY),
        )

        assertEquals(
            ZonedDateTime.of(2026, 10, 5, 19, 0, 0, 0, zone),
            ReminderTimeCalculator.next(now, settings),
        )
    }

    @Test
    fun movesToNextWeekWhenSelectedTimeAlreadyPassed() {
        val now = ZonedDateTime.of(2026, 10, 5, 19, 1, 0, 0, zone)
        val settings = ReminderSettings(
            enabled = true,
            hour = 19,
            minute = 0,
            days = setOf(ReminderDay.MONDAY),
        )

        assertEquals(
            ZonedDateTime.of(2026, 10, 12, 19, 0, 0, 0, zone),
            ReminderTimeCalculator.next(now, settings),
        )
    }

    @Test
    fun selectsNearestConfiguredWeekday() {
        val now = ZonedDateTime.of(2026, 10, 5, 20, 0, 0, 0, zone)
        val settings = ReminderSettings(
            enabled = true,
            hour = 8,
            minute = 15,
            days = setOf(ReminderDay.WEDNESDAY, ReminderDay.FRIDAY),
        )

        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 8, 15, 0, 0, zone),
            ReminderTimeCalculator.next(now, settings),
        )
    }

    @Test
    fun returnsNullWhenReminderIsDisabled() {
        assertNull(
            ReminderTimeCalculator.next(
                ZonedDateTime.of(2026, 10, 5, 18, 30, 0, 0, zone),
                ReminderSettings(),
            ),
        )
    }
}
