package com.toblad.khwab.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
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

        val model = ModelLoader(context).loadModel()

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

        recognizer = OnlineRecognizer(
            context.assets,
            config
        )

        stream = recognizer!!.createStream()
    }

    fun processAudio(samples: FloatArray) {

        val s = stream ?: return
        val r = recognizer ?: return

        s.acceptWaveform(samples, 16000)

        while (r.isReady(s)) {
            r.decode(s)
        }

        val result = r.getResult(s)

        if (result.text.isNotBlank()) {
            listener?.onRecognized(
                SpeechResult(
                    text = result.text,
                    isFinal = true
                )
            )
        }
    }

    fun release() {
        stream?.release()
        recognizer?.release()
        stream = null
        recognizer = null
    }
}
