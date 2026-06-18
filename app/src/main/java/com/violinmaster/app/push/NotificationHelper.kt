package com.violinmaster.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.violinmaster.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ASSIGNMENTS = "assignments"
        const val CHANNEL_PRACTICE_REMINDERS = "practice_reminders"
        const val CHANNEL_CHAT = "chat_messages"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_ASSIGNMENTS,
                "Assignments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "New assignments from your teacher" },
            NotificationChannel(
                CHANNEL_PRACTICE_REMINDERS,
                "Practice Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily practice reminders" },
            NotificationChannel(
                CHANNEL_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Messages from your teacher or student" }
        )
        manager.createNotificationChannels(channels)
    }

    fun buildNotification(
        channelId: String,
        title: String,
        body: String,
        targetScreen: String? = null,
        targetId: String? = null
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            targetScreen?.let { putExtra("notification_target_screen", it) }
            targetId?.let { putExtra("notification_target_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun notify(id: Int, notification: android.app.Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(id, notification)
    }
}
