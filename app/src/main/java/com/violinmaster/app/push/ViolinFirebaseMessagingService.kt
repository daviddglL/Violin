package com.violinmaster.app.push

import android.app.NotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViolinFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token refresh handled by PushTokenManager when user logs in.
        // If already logged in, re-register.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: return
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body ?: ""
        val targetScreen = data["target_screen"]
        val targetId = data["target_id"]

        val channelId = when (type) {
            "assignment_created", "assignment_completed" -> NotificationHelper.CHANNEL_ASSIGNMENTS
            "practice_reminder" -> NotificationHelper.CHANNEL_PRACTICE_REMINDERS
            "chat_message" -> NotificationHelper.CHANNEL_CHAT
            else -> NotificationHelper.CHANNEL_ASSIGNMENTS
        }

        val notification = notificationHelper.buildNotification(
            channelId = channelId,
            title = title,
            body = body,
            targetScreen = targetScreen,
            targetId = targetId
        )

        notificationHelper.notify(System.currentTimeMillis().toInt(), notification)
    }
}
