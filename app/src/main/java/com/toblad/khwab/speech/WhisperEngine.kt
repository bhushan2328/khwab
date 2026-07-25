package com.toblad.khwab.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.speech.listener.RecognitionListener

class WhisperEngine(
    private val context: Context
) {

    private var listener: RecognitionListener? = null

private var recognizer: OfflineRecognizer? = null
private var stream: OfflineStream? = null

    fun setRecognitionListener(listener: RecognitionListener) {
        this.listener = listener
    }

    fun initialize() {

        Logger.info(
            LogModule.SPEECH,
            "Initializing WhisperEngine"
        )

        val model = WhisperModelLoader(context).loadModel()

        Logger.info(
            LogModule.SPEECH,
            "Encoder : ${model.encoder}"
        )

        Logger.info(
            LogModule.SPEECH,
            "Decoder : ${model.decoder}"
        )

        Logger.info(
            LogModule.SPEECH,
            "Tokens  : ${model.tokens}"
        )

        Logger.info(
            LogModule.SPEECH,
            "Whisper model loaded successfully"
        )

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
            whisper = whisperConfig
            tokens = model.tokens
            provider = "cpu"
            numThreads = 2
        }

        Logger.info(
            LogModule.SPEECH,
            "Whisper configuration created"
        )

        val recognizerConfig = OfflineRecognizerConfig().apply {
            featConfig = FeatureConfig()
            modelConfig = offlineModelConfig
            decodingMethod = "greedy_search"
        }

        recognizer = OfflineRecognizer(
            null,
            recognizerConfig
        )

        stream = recognizer!!.createStream()

        Logger.info(
            LogModule.SPEECH,
            "OfflineRecognizer created successfully"
        )
    }

    fun processAudio(
        samples: FloatArray,
        sampleRate: Int = 16000
    ) {

        val currentRecognizer = recognizer ?: return
        val currentStream = stream ?: return

        currentStream.acceptWaveform(samples, sampleRate)

        currentRecognizer.decode(currentStream)

        val result = currentRecognizer.getResult(currentStream)

        if (result.text.isNotBlank()) {
            listener?.onRecognized(
                SpeechResult(
                    text = result.text,
                    isFinal = true
                )
            )
        }
    }

    fun reset() {

        stream = recognizer?.createStream()

        Logger.info(
            LogModule.SPEECH,
            "WhisperEngine reset"
        )
    }

    fun release() {

        stream = null
        recognizer = null
        listener = null

        Logger.info(
            LogModule.SPEECH,
            "WhisperEngine released"
        )
    }
}





