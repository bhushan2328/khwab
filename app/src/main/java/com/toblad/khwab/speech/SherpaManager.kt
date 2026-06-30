package com.toblad.khwab.speech

class SherpaManager {

    private val modelLoader = ModelLoader()
    private val audioRecorder = AudioRecorder()
    private val speechRecognizer = SpeechRecognizer()

    fun initialize() {
        modelLoader.loadModel()
    }

    fun startListening() {
        audioRecorder.startRecording()
    }

    fun stopListening() {
        audioRecorder.stopRecording()
    }
}