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
     * Hosted on: https://github.com/bhushan2328/khwab/releases/tag/v1.0-models
     */
    private val MODEL_FILES = listOf(
        ModelFile(
            name = "encoder.int8.onnx",
            url  = "https://github.com/bhushan2328/khwab/releases/download/v1.0-models/encoder.int8.onnx"
        ),
        ModelFile(
            name = "decoder.int8.onnx",
            url  = "https://github.com/bhushan2328/khwab/releases/download/v1.0-models/decoder.int8.onnx"
        ),
        ModelFile(
            name = "tokens.txt",
            url  = "https://github.com/bhushan2328/khwab/releases/download/v1.0-models/tokens.txt"
        )
    )

    data class ModelFile(val name: String, val url: String)

    /** Returns true when all model files exist and are non-empty. */
    fun modelsReady(context: Context): Boolean {
        val dir = File(context.filesDir, OUTPUT_DIR)
        val ready = MODEL_FILES.all { f ->
            val file = File(dir, f.name)
            file.exists() && file.length() > 0
        }
        // Sanitize tokens.txt BOM on every check — handles files already on disk.
        if (ready) {
            stripBomIfPresent(File(dir, "tokens.txt"))
        }
        return ready
    }

    /**
     * Downloads all model files sequentially, resuming partial downloads.
     *
     * If a previous attempt left a `.tmp` file on disk, a `Range` request
     * continues from where it left off so the user never re-downloads bytes
     * they already have — critical for large files on slow/mobile connections.
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

            // Skip files already fully and successfully downloaded.
            if (dest.exists() && dest.length() > 0) {
                Log.d(TAG, "Already downloaded: ${modelFile.name}")
                onProgress(((fileIndex + 1) * 100) / totalFiles, modelFile.name)
                return@forEachIndexed
            }

            val tmp = File(dir, "${modelFile.name}.tmp")
            val resumeFrom = if (tmp.exists()) tmp.length() else 0L

            Log.d(TAG, "Downloading: ${modelFile.name} " +
                    "(resume from $resumeFrom bytes) from ${modelFile.url}")

            try {
                val connection = (URL(modelFile.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    // Large model files on slow connections need a generous read
                    // timeout — 5 minutes is safe without risking true hangs.
                    readTimeout    = 300_000
                    instanceFollowRedirects = true
                    if (resumeFrom > 0) {
                        setRequestProperty("Range", "bytes=$resumeFrom-")
                    }
                    connect()
                }

                val code = connection.responseCode
                val resuming = code == HttpURLConnection.HTTP_PARTIAL  // 206
                val fresh    = code == HttpURLConnection.HTTP_OK       // 200

                check(resuming || fresh) {
                    "HTTP $code for ${modelFile.url}"
                }

                // If the server ignored our Range header and sent 200, start fresh.
                val startOffset = if (resuming) resumeFrom else 0L
                if (!resuming && resumeFrom > 0) {
                    Log.d(TAG, "Server does not support resume — restarting ${modelFile.name}")
                    tmp.delete()
                }

                val contentLength = connection.contentLengthLong
                // Total file size: for a 206 response, Content-Length is the remaining
                // bytes, so we add the already-downloaded offset to get the full size.
                val totalBytes = (if (resuming) startOffset + contentLength
                                  else contentLength).coerceAtLeast(1L)
                var bytesRead = startOffset

                connection.inputStream.use { input ->
                    // append=true resumes from the end; append=false overwrites for a fresh start.
                    java.io.FileOutputStream(tmp, resuming).use { output ->
                        val buf = ByteArray(64 * 1024)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            output.write(buf, 0, n)
                            bytesRead += n

                            val filePercent    = (bytesRead * 100L / totalBytes).toInt()
                            val overallPercent = (fileIndex * 100 + filePercent) / totalFiles
                            onProgress(overallPercent, modelFile.name)
                        }
                    }
                }

                // Atomic rename — only promote tmp to dest when fully written.
                tmp.renameTo(dest)
                Log.d(TAG, "Downloaded: ${modelFile.name} (${dest.length() / 1_000_000} MB)")

                // Strip UTF-8 BOM (EF BB BF) from tokens.txt — sherpa-onnx's
                // native base64 decoder crashes (abort) on character 239 (0xEF).
                if (modelFile.name == "tokens.txt") {
                    stripBomIfPresent(dest)
                }

            } catch (e: Exception) {
                // Leave the .tmp file intact so the next attempt can resume.
                Log.e(TAG, "Download interrupted for ${modelFile.name}: ${e.message}")
                throw e
            }
        }

        onProgress(100, "")
        Log.d(TAG, "All model files downloaded successfully.")
    }

    /**
     * Strips a UTF-8 BOM (0xEF 0xBB 0xBF) from the start of [file] if present.
     * Rewrites the file in-place without the BOM bytes.
     */
    private fun stripBomIfPresent(file: File) {
        val bytes = file.readBytes()
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            Log.d(TAG, "Stripping UTF-8 BOM from ${file.name}")
            file.writeBytes(bytes.copyOfRange(3, bytes.size))
        }
    }
}
