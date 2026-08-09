package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import com.toblad.khwab.speech.listener.RecognitionListener
import java.io.File

class SpeechManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "SpeechManager"

        /**
         * Load the Sherpa-ONNX native libraries from [filesDir]/sherpa-libs/.
         *
         * When the AAR is bundled inside the APK, Android unpacks the .so files
         * into the app's native lib directory and System.loadLibrary() finds them
         * automatically. After removing the AAR from the APK we download the .so
         * files at runtime instead. System.load() with an absolute path is
         * equivalent — the dynamic linker reuses already-loaded libraries so
         * calling this before OfflineRecognizer() means the JNI bridge finds
         * them even though they live in filesDir rather than the APK lib dir.
         *
         * Loading order: onnxruntime → c-api → cxx-api → jni
         * (each lib depends on the ones before it).
         */
        fun loadNativeLibs(context: Context) {
            val dir = File(context.filesDir, "sherpa-libs")
            for (libName in ModelDownloadManager.NATIVE_LIBS) {
                val file = File(dir, libName)
                if (file.exists()) {
                    try {
                        System.load(file.absolutePath)
                        Log.d(TAG, "Loaded: $libName")
                    } catch (e: UnsatisfiedLinkError) {
                        // Already loaded by a previous call — safe to ignore.
                        Log.d(TAG, "Already loaded (ignored): $libName — ${e.message}")
                    }
                } else {
                    Log.w(TAG, "Native lib not found: ${file.absolutePath}")
                }
            }
        }
    }

    private val engine = WhisperEngine(context)
    private val recorder = AudioRecorder()

    fun setRecognitionListener(listener: RecognitionListener) {
        engine.setRecognitionListener(listener)
    }

    fun initialize() {
        // Pre-load native .so files from filesDir before the Sherpa JNI bridge
        // tries to resolve them via System.loadLibrary().
        loadNativeLibs(context)
        engine.initialize()
    }

    fun startListening() {
        recorder.startRecording { samples ->
            engine.processAudio(samples)
        }
    }

    fun stopListening() {
        recorder.stopRecording()
    }

    fun release() {
        recorder.release()
        engine.release()
    }
}
