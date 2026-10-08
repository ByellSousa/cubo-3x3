package com.gabs.cubo3x3.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gabs.cubo3x3.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.gabs.cubo3x3.action.PRACTICE_REMINDER") return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = AppPreferences(context).reminderSettings.first()
                if (settings.enabled) ReminderNotifier.show(context)
                ReminderScheduler(context).apply(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in SupportedActions) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = AppPreferences(context).reminderSettings.first()
                ReminderScheduler(context).apply(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val SupportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
