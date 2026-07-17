package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.toblad.khwab.integration.api.KhwabIntegrationProvider
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.speech.SherpaManager

class VoiceService : Service() {

    private lateinit var sherpaManager: SherpaManager
    private val integration = KhwabIntegrationProvider.create()

    override fun onCreate() {
        super.onCreate()

        integration.initialize()

        sherpaManager = SherpaManager(this)

        sherpaManager.setRecognitionListener { result ->

            Log.d("Sherpa", result.text)

            val response = integration.process(
                IntegrationRequest(
                    input = result.text
                )
            )

            Log.d("Khwab", "Response : $response")
        }

        sherpaManager.initialize()
        sherpaManager.startListening()
    }

    override fun onDestroy() {
        sherpaManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
