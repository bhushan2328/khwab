package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegrationProvider
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.speech.SherpaManager

class VoiceService : Service() {

    private lateinit var sherpaManager: SherpaManager
    private lateinit var executionEngine: AndroidExecutionEngine

    private val integration = KhwabIntegrationProvider.create()

    override fun onCreate() {
        super.onCreate()

        integration.initialize()
        executionEngine = AndroidExecutionEngine(this)

        sherpaManager = SherpaManager(this)

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

        sherpaManager.initialize()
        sherpaManager.startListening()
    }

    override fun onDestroy() {
        sherpaManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
