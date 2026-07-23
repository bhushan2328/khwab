package com.toblad.khwab.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.speech.listener.RecognitionListener

class SherpaEngine(
    private val context: Context
) {

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var listener: RecognitionListener? = null

    fun setRecognitionListener(listener: RecognitionListener) {
        this.listener = listener
    }

    fun initialize() {

        Logger.info(LogModule.SPEECH, "Initializing SherpaEngine")

        try {

            Logger.info(LogModule.SPEECH, "Loading speech model")

            val model = ModelLoader(context).loadModel()

            Logger.info(LogModule.SPEECH, "Creating OnlineRecognizerConfig")

            val config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = model.encoder,
                        decoder = model.decoder,
                        joiner = model.joiner
                    ),
                    tokens = model.tokens,
                    bpeVocab = model.bpe,
                    modelType = "zipformer",
                    provider = "cpu",
                    numThreads = 2
                ),
                decodingMethod = "greedy_search"
            )

            Logger.info(LogModule.SPEECH, "Creating OnlineRecognizer")

            recognizer = OnlineRecognizer(
                null,
                config
            )

            Logger.info(LogModule.SPEECH, "Creating OnlineStream")

            stream = recognizer!!.createStream()

            Logger.info(LogModule.SPEECH, "SherpaEngine initialized successfully")

        } catch (e: Exception) {

            Logger.error(
                LogModule.SPEECH,
                "Failed to initialize SherpaEngine",
                e
            )

            throw e
        }
    }

    fun processAudio(samples: FloatArray) {

        val s = stream ?: return
        val r = recognizer ?: return

        try {

            s.acceptWaveform(samples, 16000)

            while (r.isReady(s)) {
                r.decode(s)
            }

            val result = r.getResult(s)

            if (result.text.isNotBlank()) {

                Logger.info(
                    LogModule.SPEECH,
                    "Recognized: ${result.text}"
                )

                listener?.onRecognized(
                    SpeechResult(
                        text = result.text,
                        isFinal = true
                    )
                )
            }

        } catch (e: Exception) {

            Logger.error(
                LogModule.SPEECH,
                "Speech recognition failed",
                e
            )
        }
    }

    fun release() {

        Logger.info(LogModule.SPEECH, "Releasing SherpaEngine")

        stream?.release()
        recognizer?.release()

        stream = null
        recognizer = null
    }
}