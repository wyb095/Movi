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

        createChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Movi notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Task updates, chat messages, and delivery alerts"
        }
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "movi_default"
    }
}
