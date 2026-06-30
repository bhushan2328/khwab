package com.toblad.khwab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.toblad.khwab.R

object NotificationHelper {

    const val CHANNEL_ID = "khwab_voice_service"
    private const val CHANNEL_NAME = "Khwab Assistant"

    fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for Khwab"
            }

            val notificationManager =
                context.getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(context: Context): Notification {

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 Khwab")
            .setContentText("Assistant is running")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}