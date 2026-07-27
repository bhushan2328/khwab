package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import java.io.File

object ModelInitializer {

    private const val TAG = "ModelInitializer"

    private const val ASSET_DIR = "models/whisper-small"
    private const val OUTPUT_DIR = "whisper-small"

    fun prepare(context: Context) {

        val outputDir = File(context.filesDir, OUTPUT_DIR)

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val requiredFiles = listOf(
            "encoder.int8.onnx",
            "decoder.int8.onnx",
            "tokens.txt"
        )

        val alreadyPrepared = requiredFiles.all {
            File(outputDir, it).exists()
        }

        if (alreadyPrepared) {

            Log.d(TAG, "Whisper model already prepared.")

            return
        }

        val assetManager = context.assets

        val files = assetManager.list(ASSET_DIR)
            ?: throw IllegalStateException(
                "Whisper assets not found."
            )

        files.forEach { fileName ->

            val destination = File(outputDir, fileName)

            assetManager.open("$ASSET_DIR/$fileName").use { input ->

                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Copied: $fileName")
        }

        Log.d(TAG, "Whisper model preparation completed.")
    }
}