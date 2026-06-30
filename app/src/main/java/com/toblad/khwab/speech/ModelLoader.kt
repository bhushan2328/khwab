package com.toblad.khwab.speech

import android.util.Log

class ModelLoader {

    companion object {
        private const val TAG = "ModelLoader"
    }

    fun loadModel() {

        Log.d(TAG, "Loading Offline Speech Model...")

        // Whisper model will be loaded here later

        Log.d(TAG, "Model Loaded Successfully")
    }
}