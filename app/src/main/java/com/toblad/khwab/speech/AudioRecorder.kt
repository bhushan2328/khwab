package com.toblad.khwab.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread

class AudioRecorder {

    private val sampleRate = 16000

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var recorder: AudioRecord? = null

    @Volatile
    private var recording = false

    @SuppressLint("MissingPermission")
    fun startRecording(onAudio: (FloatArray) -> Unit) {

        if (recording) return

        // Always create a fresh AudioRecord — reusing a stopped instance throws
        // on some devices and produces glitched audio on others.
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        recording = true
        recorder!!.startRecording()

        thread(name = "khwab-audio-recorder") {
            val pcm = ShortArray(bufferSize)
            while (recording) {
                val read = recorder!!.read(pcm, 0, pcm.size)
                if (read > 0) {
                    val samples = FloatArray(read) { pcm[it] / 32768.0f }
                    onAudio(samples)
                }
            }
        }
    }

    fun stopRecording() {
        recording = false
        recorder?.stop()
        // Release immediately so the next startRecording() gets a clean instance.
        recorder?.release()
        recorder = null
    }

    fun release() {
        recording = false
        recorder?.release()
        recorder = null
    }
}
