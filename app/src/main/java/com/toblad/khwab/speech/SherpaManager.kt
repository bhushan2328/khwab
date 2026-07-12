package com.toblad.khwab.speech

import android.content.Context
import com.toblad.khwab.speech.listener.RecognitionListener

class SherpaManager(
    private val context: Context
) {

    private val engine = SherpaEngine(context)
    private val recorder = AudioRecorder()

    fun setRecognitionListener(listener: RecognitionListener) {
        engine.setRecognitionListener(listener)
    }

    fun initialize() {
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
