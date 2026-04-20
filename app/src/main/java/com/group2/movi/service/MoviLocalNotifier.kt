package com.group2.movi.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.group2.movi.R

@Singleton
class MoviLocalNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @SuppressLint("MissingPermission")
    fun showMatchReady(taskTitle: String, count: Int) {
        if (!hasNotificationPermission(context)) return
        createChannel()
        val body = if (count == 1) {
            "A carrier now has strong route overlap for \"$taskTitle\"."
        } else {
            "$count carriers now have strong route overlap for \"$taskTitle\"."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Route overlap found")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(taskTitle.hashCode(), notification)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Movi route overlap",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a posted task now has strongly overlapping carrier routes."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "movi_matching"
    }
}
