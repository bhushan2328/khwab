package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the Whisper Small int8 ONNX model files from the Sherpa-ONNX
 * GitHub release into [Context.filesDir]/whisper-small/ on first launch.
 *
 * Files are ~357 MB total. After a successful download, [modelsReady]
 * returns true and no further network access is needed for speech.
 *
 * Host: https://github.com/k2-fsa/sherpa-onnx/releases/download/
 *       asr-models/sherpa-onnx-whisper-small.int8.tar.bz2  (tarball)
 *
 * We use individual file URLs so we can stream each file directly
 * to disk with progress reporting without needing a bz2 decompressor.
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownloadManager"
    private const val OUTPUT_DIR = "whisper-small"

    /**
     * Individual file download URLs.
     * Source: https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models
     */
    private val MODEL_FILES = listOf(
        ModelFile(
            name = "encoder.int8.onnx",
            url  = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small.int8-encoder.onnx"
        ),
        ModelFile(
            name = "decoder.int8.onnx",
            url  = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small.int8-decoder.onnx"
        ),
        ModelFile(
            name = "tokens.txt",
            url  = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small-tokens.txt"
        )
    )

    data class ModelFile(val name: String, val url: String)

    /** Returns true when all model files exist and are non-empty. */
    fun modelsReady(context: Context): Boolean {
        val dir = File(context.filesDir, OUTPUT_DIR)
        return MODEL_FILES.all { f ->
            val file = File(dir, f.name)
            file.exists() && file.length() > 0
        }
    }

    /**
     * Downloads all model files sequentially.
     *
     * [onProgress] is called with values 0..100 representing overall
     * percentage across all files combined.
     *
     * Throws on network / IO error — caller should handle and retry.
     */
    suspend fun downloadAll(
        context: Context,
        onProgress: (percent: Int, fileName: String) -> Unit
    ) = withContext(Dispatchers.IO) {

        val dir = File(context.filesDir, OUTPUT_DIR).also { it.mkdirs() }

        val totalFiles = MODEL_FILES.size

        MODEL_FILES.forEachIndexed { fileIndex, modelFile ->
            val dest = File(dir, modelFile.name)

            // Skip files already fully downloaded
            if (dest.exists() && dest.length() > 0) {
                Log.d(TAG, "Already downloaded: ${modelFile.name}")
                onProgress(((fileIndex + 1) * 100) / totalFiles, modelFile.name)
                return@forEachIndexed
            }

            Log.d(TAG, "Downloading: ${modelFile.name} from ${modelFile.url}")

            val tmp = File(dir, "${modelFile.name}.tmp")
            try {
                val connection = (URL(modelFile.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout    = 60_000
                    instanceFollowRedirects = true
                    connect()
                }

                check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                    "HTTP ${connection.responseCode} for ${modelFile.url}"
                }

                val totalBytes = connection.contentLengthLong.coerceAtLeast(1L)
                var bytesRead = 0L

                connection.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            output.write(buf, 0, n)
                            bytesRead += n

                            // Overall progress: each file is (1/totalFiles) of 100%
                            val filePercent   = (bytesRead * 100L / totalBytes).toInt()
                            val overallPercent =
                                (fileIndex * 100 + filePercent) / totalFiles
                            onProgress(overallPercent, modelFile.name)
                        }
                    }
                }

                // Atomic rename — never leave a partial file as the real file
                tmp.renameTo(dest)
                Log.d(TAG, "Downloaded: ${modelFile.name} (${dest.length() / 1_000_000} MB)")

            } catch (e: Exception) {
                tmp.delete()
                throw e
            }
        }

        onProgress(100, "")
        Log.d(TAG, "All model files downloaded successfully.")
    }
}
