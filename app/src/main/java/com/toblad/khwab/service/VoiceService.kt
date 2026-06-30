package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.toblad.khwab.overlay.FloatingWindow
import com.toblad.khwab.speech.SherpaManager
class VoiceService : Service() {

    private lateinit var floatingWindow: FloatingWindow
    private lateinit var sherpaManager: SherpaManager
    override fun onCreate() {
        super.onCreate()

        Log.d("KHWAB", "VoiceService Created")

        floatingWindow = FloatingWindow(this)

        sherpaManager = SherpaManager(this)
        sherpaManager.initialize()

        NotificationHelper.createNotificationChannel(this)

        startForeground(
            1,
            NotificationHelper.createNotification(this)
        )

        floatingWindow.show()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d("KHWAB", "VoiceService Started")

        return START_STICKY
    }

    override fun onDestroy() {

        floatingWindow.hide()

        Log.d("KHWAB", "VoiceService Destroyed")

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}