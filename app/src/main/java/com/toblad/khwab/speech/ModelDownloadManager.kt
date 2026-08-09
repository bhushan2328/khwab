package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

/**
 * Downloads the Whisper Small int8 ONNX model files AND the Sherpa-ONNX
 * native libraries from GitHub Releases on first launch.
 *
 * Why download the native libs?
 *   The sherpa-onnx AAR (54 MB) contains libonnxruntime.so and friends.
 *   Bundling it in the APK made the install ~57 MB heavier. Instead we
 *   ship only the Java API stub (classes.jar, 229 KB) at compile time and
 *   download the ARM-only AAR (~25 MB) at runtime, then extract the .so
 *   files into filesDir so System.load() can reach them.
 *
 * Download sequence (shown in the UI as one combined progress bar):
 *   Phase 1 — Native libs AAR  (~25 MB, 0–30 % of overall progress)
 *   Phase 2 — Whisper models   (~357 MB, 30–100 % of overall progress)
 *
 * After a successful first run [nativeLibsReady] and [modelsReady] both
 * return true and no further network access is needed.
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownloadManager"
    private const val MODEL_DIR  = "whisper-small"
    private const val NATIVE_DIR = "sherpa-libs"

    // ── Native libs download ──────────────────────────────────────────────────

    private const val NATIVE_BASE_URL =
        "https://github.com/bhushan2328/khwab/releases/download/v1.0-models"

    /**
     * Native .so files present in each ABI zip.
     * Load order matters: onnxruntime first, then the sherpa chain.
     */
    val NATIVE_LIBS = listOf(
        "libonnxruntime.so",
        "libsherpa-onnx-c-api.so",
        "libsherpa-onnx-cxx-api.so",
        "libsherpa-onnx-jni.so"
    )

    // ── Model files ───────────────────────────────────────────────────────────

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

    // ── Readiness checks ──────────────────────────────────────────────────────

    /** True when all Whisper model files exist and are non-empty. */
    fun modelsReady(context: Context): Boolean {
        val dir = File(context.filesDir, MODEL_DIR)
        val ready = MODEL_FILES.all { f ->
            val file = File(dir, f.name)
            file.exists() && file.length() > 0
        }
        if (ready) stripBomIfPresent(File(dir, "tokens.txt"))
        return ready
    }

    /**
     * True when all required .so files exist in [NATIVE_DIR].
     * Called before SpeechManager.initialize() to decide whether to
     * attempt System.load().
     */
    fun nativeLibsReady(context: Context): Boolean {
        val dir = File(context.filesDir, NATIVE_DIR)
        return NATIVE_LIBS.all { name ->
            val f = File(dir, name)
            f.exists() && f.length() > 0
        }
    }

    /**
     * Both phases complete — the app can start the speech engine.
     */
    fun allReady(context: Context) = nativeLibsReady(context) && modelsReady(context)

    // ── Main download entry point ─────────────────────────────────────────────

    /**
     * Phase 1: download the ARM-only AAR and extract .so files.
     * Phase 2: download the Whisper model files.
     *
     * [onProgress] receives (0..100, currentFileName) for the overall
     * combined progress. Phase 1 occupies 0–30, Phase 2 occupies 30–100.
     */
    suspend fun downloadAll(
        context: Context,
        onProgress: (percent: Int, fileName: String) -> Unit
    ) = withContext(Dispatchers.IO) {

        // ── Phase 1: native libs (0–30 %) ────────────────────────────────────
        if (!nativeLibsReady(context)) {
            downloadAndExtractNativeLibs(context) { raw, name ->
                // raw 0..100 → overall 0..30
                onProgress((raw * 30) / 100, name)
            }
        } else {
            onProgress(30, "")
        }

        // ── Phase 2: Whisper models (30–100 %) ───────────────────────────────
        downloadModelFiles(context) { raw, name ->
            // raw 0..100 → overall 30..100
            onProgress(30 + (raw * 70) / 100, name)
        }

        onProgress(100, "")
        Log.d(TAG, "All downloads complete.")
    }

    // ── Phase 1 implementation ────────────────────────────────────────────────

    private fun downloadAndExtractNativeLibs(
        context: Context,
        onProgress: (Int, String) -> Unit
    ) {
        val nativeDir = File(context.filesDir, NATIVE_DIR).also { it.mkdirs() }

        // Pick the zip that matches this device — only download what's needed.
        val abi     = preferredAbi()
        val zipName = "sherpa-onnx-$abi.zip"
        val zipUrl  = "$NATIVE_BASE_URL/$zipName"
        val zipDest = File(context.filesDir, zipName)

        Log.d(TAG, "Downloading native libs for ABI: $abi")

        // Download the ABI-specific zip (arm64-v8a ≈12.7 MB, armeabi-v7a ≈11.7 MB)
        downloadFile(
            url        = zipUrl,
            dest       = zipDest,
            onProgress = { pct -> onProgress((pct * 80) / 100, zipName) }
        )

        onProgress(82, "Extracting native libraries…")

        // The zip contains .so files flat (no subdirectory) — extract directly
        ZipFile(zipDest).use { zip ->
            for (libName in NATIVE_LIBS) {
                val entry = zip.getEntry(libName) ?: run {
                    Log.w(TAG, "Entry not found in zip: $libName")
                    continue
                }
                val outFile = File(nativeDir, libName)
                zip.getInputStream(entry).use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Extracted: $libName (${outFile.length() / 1024} KB)")
            }
        }

        // Delete the zip — .so files are now in nativeDir
        zipDest.delete()
        onProgress(100, "")
        Log.d(TAG, "Native libs ready in $nativeDir")
    }

    /**
     * Returns the preferred ABI directory name for the running device.
     * Falls back to armeabi-v7a if arm64-v8a is not reported.
     */
    private fun preferredAbi(): String {
        val supported = android.os.Build.SUPPORTED_ABIS
        return when {
            "arm64-v8a"   in supported -> "arm64-v8a"
            "armeabi-v7a" in supported -> "armeabi-v7a"
            else -> "arm64-v8a" // best guess
        }
    }

    // ── Phase 2 implementation ────────────────────────────────────────────────

    private fun downloadModelFiles(
        context: Context,
        onProgress: (Int, String) -> Unit
    ) {
        val dir        = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }
        val totalFiles = MODEL_FILES.size

        MODEL_FILES.forEachIndexed { fileIndex, modelFile ->
            val dest = File(dir, modelFile.name)
            if (dest.exists() && dest.length() > 0) {
                Log.d(TAG, "Already downloaded: ${modelFile.name}")
                onProgress(((fileIndex + 1) * 100) / totalFiles, modelFile.name)
                return@forEachIndexed
            }

            downloadFile(
                url  = modelFile.url,
                dest = dest,
                onProgress = { filePct ->
                    val overall = (fileIndex * 100 + filePct) / totalFiles
                    onProgress(overall, modelFile.name)
                }
            )

            if (modelFile.name == "tokens.txt") stripBomIfPresent(dest)
        }

        onProgress(100, "")
    }

    // ── Generic resumable file downloader ─────────────────────────────────────

    /**
     * Downloads [url] to [dest], supporting HTTP 206 resume from a `.tmp`
     * file left by a previous interrupted attempt.
     *
     * [onProgress] is called with 0..100 for this individual file.
     * Throws on network/IO error.
     */
    private fun downloadFile(
        url: String,
        dest: File,
        onProgress: (Int) -> Unit
    ) {
        val tmp        = File(dest.parent, "${dest.name}.tmp")
        val resumeFrom = if (tmp.exists()) tmp.length() else 0L

        Log.d(TAG, "Downloading ${dest.name} (resume=$resumeFrom) from $url")

        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod          = "GET"
                connectTimeout         = 15_000
                readTimeout            = 300_000
                instanceFollowRedirects = true
                if (resumeFrom > 0) setRequestProperty("Range", "bytes=$resumeFrom-")
                connect()
            }

            val code      = connection.responseCode
            val resuming  = code == HttpURLConnection.HTTP_PARTIAL
            val fresh     = code == HttpURLConnection.HTTP_OK
            check(resuming || fresh) { "HTTP $code for $url" }

            val startOffset = if (resuming) resumeFrom else 0L
            if (!resuming && resumeFrom > 0) { tmp.delete() }

            val contentLength = connection.contentLengthLong
            val totalBytes    = (if (resuming) startOffset + contentLength else contentLength).coerceAtLeast(1L)
            var bytesRead     = startOffset

            connection.inputStream.use { input ->
                java.io.FileOutputStream(tmp, resuming).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        bytesRead += n
                        onProgress((bytesRead * 100L / totalBytes).toInt())
                    }
                }
            }

            tmp.renameTo(dest)
            Log.d(TAG, "Downloaded: ${dest.name} (${dest.length() / 1_000_000} MB)")

        } catch (e: Exception) {
            Log.e(TAG, "Download interrupted for ${dest.name}: ${e.message}")
            throw e
        }
    }

    // ── BOM stripper ──────────────────────────────────────────────────────────

    private fun stripBomIfPresent(file: File) {
        if (!file.exists()) return
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
