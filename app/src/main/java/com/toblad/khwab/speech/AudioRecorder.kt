package com.toblad.khwab.speech

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

    private val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    @Volatile
    private var recording = false

    fun startRecording(
        onAudio: (FloatArray) -> Unit
    ) {

        if (recording) return

        recording = true

        recorder.startRecording()

        thread {

            val pcm = ShortArray(bufferSize)

            while (recording) {

                val read = recorder.read(
                    pcm,
                    0,
                    pcm.size
                )

                if (read > 0) {

                    val samples = FloatArray(read)

                    for (i in 0 until read) {
                        samples[i] = pcm[i] / 32768.0f
                    }

                    onAudio(samples)
                }
            }
        }
    }

    fun stopRecording() {

        recording = false

        recorder.stop()
    }

    fun release() {

        recorder.release()
    }
}
