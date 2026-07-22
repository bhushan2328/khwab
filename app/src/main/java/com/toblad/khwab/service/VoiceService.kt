package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegrationProvider
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.overlay.FloatingWindow
import com.toblad.khwab.speech.SherpaManager

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var sherpaManager: SherpaManager
    private lateinit var executionEngine: AndroidExecutionEngine
    private lateinit var floatingWindow: FloatingWindow

    private val integration = KhwabIntegrationProvider.create()

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "VoiceService created")

        executionEngine = AndroidExecutionEngine(this)
        floatingWindow = FloatingWindow(this)
        sherpaManager = SherpaManager(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        try {

            Log.d(TAG, "Starting foreground service")

            NotificationHelper.createNotificationChannel(this)

            startForeground(
                NOTIFICATION_ID,
                NotificationHelper.createNotification(this)
            )

            Log.d(TAG, "Showing floating window")

            floatingWindow.show()

            Log.d(TAG, "Initializing integration")

            integration.initialize()

            sherpaManager.setRecognitionListener { result ->

                Log.d("Sherpa", result.text)

                val response = integration.process(
                    IntegrationRequest(
                        input = result.text
                    )
                )

                response.executionPlan?.let { plan ->

                    Log.d("Khwab", "Executing: ${plan.action}")

                    val success = executionEngine.execute(plan)

                    Log.d("Khwab", "Execution Success: $success")
                }
            }

            Log.d(TAG, "Initializing Sherpa")

            sherpaManager.initialize()

            Log.d(TAG, "Starting listening")

            sherpaManager.startListening()

            Log.d(TAG, "VoiceService started successfully")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to start VoiceService", e)

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {

        Log.d(TAG, "Stopping VoiceService")

        try {
            sherpaManager.release()
        } catch (_: Exception) {
        }

        try {
            floatingWindow.hide()
        } catch (_: Exception) {
        }

        stopForeground(STOP_FOREGROUND_REMOVE)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}