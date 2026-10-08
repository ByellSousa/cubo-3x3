package com.gabs.cubo3x3.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gabs.cubo3x3.MainActivity
import com.gabs.cubo3x3.R

object ReminderNotifier {
    private const val CHANNEL_ID = "practice_reminders"
    private const val NOTIFICATION_ID = 3301

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context) {
        if (!canPostNotifications(context)) return
        createChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_cube)
            .setContentTitle("Hora de treinar o cubo")
            .setContentText("Abra o 3x3 e pratique alguns algoritmos hoje.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // A permissão pode ser revogada entre a verificação e o envio.
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Lembretes de treino",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Lembretes locais para praticar o cubo 3x3"
            },
        )
    }
}
