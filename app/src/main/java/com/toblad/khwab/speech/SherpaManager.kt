package com.toblad.khwab.speech

import android.content.Context
import android.util.Log

class SherpaManager(
    private val context: Context
) {

    private val modelLoader = ModelLoader(context)
    private val audioRecorder = AudioRecorder()
    private val speechRecognizer = SpeechRecognizer()

    companion object {
        private const val TAG = "SherpaManager"
    }

    fun initialize() {
        Log.d(TAG, "Initializing Speech Engine...")

        modelLoader.loadModel()

        Log.d(TAG, "Speech Engine Ready")
    }

    fun startListening() {
        Log.d(TAG, "Start Listening")

        audioRecorder.startRecording()
    }

    fun stopListening() {
        Log.d(TAG, "Stop Listening")

        audioRecorder.stopRecording()
    }
}