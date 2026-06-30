package com.toblad.khwab.speech

import android.content.Context
import android.util.Log

class ModelLoader(
    private val context: Context
) {

    companion object {
        private const val TAG = "ModelLoader"
    }

    fun loadModel() {
        try {

            val files = context.assets.list("models/whisper")

            if (files.isNullOrEmpty()) {
                Log.d(TAG, "No model files found.")
                return
            }

            Log.d(TAG, "===== Whisper Model Files =====")

            files.forEach {
                Log.d(TAG, it)
            }

            Log.d(TAG, "===============================")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
        }
    }
}