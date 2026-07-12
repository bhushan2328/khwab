package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.toblad.khwab.speech.SherpaManager

class VoiceService : Service() {

    private lateinit var sherpaManager: SherpaManager

    override fun onCreate() {
        super.onCreate()

        sherpaManager = SherpaManager(this)

        sherpaManager.setRecognitionListener { result ->
            Log.d("Sherpa", result.text)

            // TODO:
            // val response = KhwabEngine.process(result.text)
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
