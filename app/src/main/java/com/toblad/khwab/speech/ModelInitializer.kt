package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import java.io.File

object ModelInitializer {

    private const val TAG = "ModelInitializer"
    private const val OUTPUT_DIR = "whisper-small"

    /**
     * Previously this copied model files from assets/ to filesDir.
     * Models are now downloaded on first launch by [ModelDownloadManager].
     *
     * This method is kept for compatibility but is now a no-op validation —
     * it simply checks that the files exist and logs their sizes.
     * The actual download + file placement is handled by [ModelDownloadManager].
     */
    fun prepare(context: Context) {
        val outputDir = File(context.filesDir, OUTPUT_DIR)
        if (!outputDir.exists()) {
            Log.w(TAG, "Model directory does not exist yet — waiting for download.")
            return
        }

        val files = listOf("encoder.int8.onnx", "decoder.int8.onnx", "tokens.txt")
        files.forEach { name ->
            val f = File(outputDir, name)
            if (f.exists()) {
                Log.d(TAG, "Model file ready: $name (${f.length() / 1_000_000} MB)")
            } else {
                Log.w(TAG, "Model file missing: $name — download may be incomplete.")
            }
        }
    }
}
