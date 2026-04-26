package com.group2.movi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.group2.movi.MainActivity
import com.group2.movi.R

class MoviFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Movi"
        val body = message.notification?.body ?: message.data["body"] ?: return
        if (!hasNotificationPermission(this)) return

        val channelId = channelIdFor(message.data["type"])
        createChannel(channelId, channelNameFor(channelId), channelDescriptionFor(channelId))

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notificationType", message.data["type"])
            putExtra("taskId", message.data["taskId"])
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(
                if (channelId == CHANNEL_TASK_UPDATES || channelId == CHANNEL_CHAT) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .build()

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannel(channelId: String, name: String, description: String) {
        val channel = NotificationChannel(
            channelId,
            name,
            if (channelId == CHANNEL_TASK_UPDATES || channelId == CHANNEL_CHAT) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
        ).apply { this.description = description }
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(channel)
    }

    private fun channelIdFor(type: String?): String = when (type) {
        "TASK_STATUS_CHANGE" -> CHANNEL_TASK_UPDATES
        "CHAT_MESSAGE" -> CHANNEL_CHAT
        else -> CHANNEL_GENERAL
    }

    private fun channelNameFor(channelId: String): String = when (channelId) {
        CHANNEL_TASK_UPDATES -> "Movi task updates"
        CHANNEL_CHAT -> "Movi chat"
        else -> "Movi notifications"
    }

    private fun channelDescriptionFor(channelId: String): String = when (channelId) {
        CHANNEL_TASK_UPDATES -> "Accepted, picked up, delivered, and confirmed task updates"
        CHANNEL_CHAT -> "New chat messages from task conversations"
        else -> "General Movi notifications"
    }

    companion object {
        private const val CHANNEL_GENERAL = "movi_default"
        private const val CHANNEL_TASK_UPDATES = "movi_task_updates"
        private const val CHANNEL_CHAT = "movi_chat"
    }
}
