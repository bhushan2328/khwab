package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import java.io.File

data class WhisperModel(
    val encoder: String,
    val decoder: String,
    val tokens: String,
    val language: String = "auto",
    val task: String = "transcribe"
)

class WhisperModelLoader(
    private val context: Context
) {

    companion object {
        private const val TAG = "WhisperModelLoader"

        private const val ASSET_DIR = "models/whisper-small"
        private const val OUTPUT_DIR = "whisper-small"
    }

    fun loadModel(): WhisperModel {

        val outputDir = File(context.filesDir, OUTPUT_DIR)

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val assetManager = context.assets

        val files = assetManager.list(ASSET_DIR)
            ?: throw IllegalStateException("Whisper assets not found.")

        files.forEach { fileName ->

            val outFile = File(outputDir, fileName)

            if (!outFile.exists()) {

                assetManager.open("$ASSET_DIR/$fileName").use { input ->

                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Copied: $fileName")
            }
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

        Log.d(TAG, "Whisper Small INT8 prepared successfully.")

        return WhisperModel(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            tokens = tokens.absolutePath,
            language = "",
            task = "transcribe"
        )
    }
}