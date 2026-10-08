package com.gabs.cubo3x3.domain.reminder

import java.time.ZonedDateTime

object ReminderTimeCalculator {
    fun next(
        now: ZonedDateTime,
        settings: ReminderSettings,
    ): ZonedDateTime? {
        if (!settings.enabled || settings.days.isEmpty()) return null
        val selectedDays = settings.days.map(ReminderDay::dayOfWeek).toSet()
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in selectedDays) continue
            val candidate = date
                .atTime(settings.hour, settings.minute)
                .atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
