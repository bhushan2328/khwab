package com.toblad.khwab.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.speech.listener.RecognitionListener

class WhisperEngine(
    private val context: Context
) {

    companion object {
        private const val SAMPLE_RATE = 16000
        // Accumulate 1.5 seconds of audio before decoding. Whisper needs a
        // meaningful chunk of speech to produce accurate transcriptions —
        // decoding on every tiny PCM buffer (a few ms each) only produces noise.
        private const val CHUNK_SAMPLES = SAMPLE_RATE * 3 / 2  // 1.5 s × 16 000 Hz
        // Silence threshold: RMS below this level is treated as silence.
        private const val SILENCE_RMS = 0.01f
        // How many consecutive silent chunks trigger a decode + emit.
        private const val SILENCE_CHUNKS_THRESHOLD = 3
    }

    private var listener: RecognitionListener? = null
    private var recognizer: OfflineRecognizer? = null

    // Accumulates raw samples until we have enough to decode.
    // AudioRecorder calls processAudio() from its own background thread, so
    // all mutable state here is confined to that single thread — no two threads
    // ever call processAudio() concurrently, so a plain list is safe as long as
    // release() (called from onDestroy on the main thread) only nulls the
    // recognizer/listener and does not touch the accumulator mid-decode.
    private val accumulator = ArrayList<Float>(CHUNK_SAMPLES * 2)
    private var silentChunkCount = 0

    fun setRecognitionListener(listener: RecognitionListener) {
        this.listener = listener
    }

    fun initialize() {

        Logger.info(LogModule.SPEECH, "Initializing WhisperEngine")

        val model = WhisperModelLoader(context).loadModel()

        Logger.info(LogModule.SPEECH, "Encoder : ${model.encoder}")
        Logger.info(LogModule.SPEECH, "Decoder : ${model.decoder}")
        Logger.info(LogModule.SPEECH, "Tokens  : ${model.tokens}")
        Logger.info(LogModule.SPEECH, "Whisper model loaded successfully")

        val whisperConfig = OfflineWhisperModelConfig(
            model.encoder,
            model.decoder,
            model.language,
            model.task,
            0,
            false,
            false
        )

        val offlineModelConfig = OfflineModelConfig().apply {
            whisper    = whisperConfig
            tokens     = model.tokens
            provider   = "cpu"
            numThreads = 2
        }

        Logger.info(LogModule.SPEECH, "Whisper configuration created")

        val recognizerConfig = OfflineRecognizerConfig().apply {
            featConfig    = FeatureConfig()
            modelConfig   = offlineModelConfig
            decodingMethod = "greedy_search"
        }

        recognizer = OfflineRecognizer(null, recognizerConfig)

        Logger.info(LogModule.SPEECH, "OfflineRecognizer created successfully")
    }

    fun processAudio(
        samples: FloatArray,
        sampleRate: Int = SAMPLE_RATE
    ) {
        val currentRecognizer = recognizer ?: return

        accumulator.addAll(samples.asList())

        // Track silence so we can decode early when speech ends rather than
        // always waiting for the fixed chunk size.
        val rms = Math.sqrt(samples.map { it * it }.average()).toFloat()
        if (rms < SILENCE_RMS) {
            silentChunkCount++
        } else {
            silentChunkCount = 0
        }

        val shouldDecode = accumulator.size >= CHUNK_SAMPLES ||
                (silentChunkCount >= SILENCE_CHUNKS_THRESHOLD && accumulator.size > SAMPLE_RATE / 4)

        if (!shouldDecode) return

        val toDecodeArray = accumulator.toFloatArray()
        accumulator.clear()
        silentChunkCount = 0

        // Each decode needs a fresh stream — reusing an already-decoded stream
        // produces empty or repeated results.
        val stream = currentRecognizer.createStream()
        stream.acceptWaveform(toDecodeArray, sampleRate)
        currentRecognizer.decode(stream)
        val result = currentRecognizer.getResult(stream)

        if (result.text.isNotBlank()) {
            listener?.onRecognized(
                SpeechResult(
                    text     = result.text,
                    isFinal  = true
                )
            )
        }
    }

    fun reset() {
        accumulator.clear()
        silentChunkCount = 0
        Logger.info(LogModule.SPEECH, "WhisperEngine reset")
    }

    fun release() {
        accumulator.clear()
        recognizer = null
        listener   = null
        Logger.info(LogModule.SPEECH, "WhisperEngine released")
    }
}





