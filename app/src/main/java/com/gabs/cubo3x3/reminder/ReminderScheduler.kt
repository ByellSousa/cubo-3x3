package com.gabs.cubo3x3.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.domain.reminder.ReminderTimeCalculator
import java.time.ZonedDateTime

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun apply(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        val next = ReminderTimeCalculator.next(now, settings)
        if (next == null || !ReminderNotifier.canPostNotifications(context)) {
            cancel()
            return
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            alarmIntent(),
        )
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderAlarmReceiver::class.java).setAction(ACTION_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val ACTION_REMINDER = "com.gabs.cubo3x3.action.PRACTICE_REMINDER"
        const val REQUEST_CODE = 3301
    }
}
