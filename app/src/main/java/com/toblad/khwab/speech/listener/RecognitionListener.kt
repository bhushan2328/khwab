package com.toblad.khwab.speech.listener

import com.toblad.khwab.speech.SpeechResult

fun interface RecognitionListener {
    fun onRecognized(result: SpeechResult)
}
