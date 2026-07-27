package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import java.io.File

data class WhisperModel(
    val encoder: String,
    val decoder: String,
    val tokens: String,
    val language: String = "",
    val task: String = "transcribe"
)

class WhisperModelLoader(
    private val context: Context
) {

    companion object {
        private const val TAG = "WhisperModelLoader"
        private const val OUTPUT_DIR = "whisper-small"
    }

    fun loadModel(): WhisperModel {

        val outputDir = File(context.filesDir, OUTPUT_DIR)

        require(outputDir.exists()) {
            "Whisper model directory does not exist. Run ModelInitializer.prepare() first."
        }

        fun requireFile(name: String): File {

            val file = File(outputDir, name)

            require(file.exists()) {
                "Missing Whisper model file: $name"
            }

            return file
        }

        val encoder = requireFile("encoder.int8.onnx")
        val decoder = requireFile("decoder.int8.onnx")
        val tokens = requireFile("tokens.txt")

        Log.d(TAG, "Using existing Whisper model files.")

        return WhisperModel(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            tokens = tokens.absolutePath,
            language = "",
            task = "transcribe"
        )
    }
}