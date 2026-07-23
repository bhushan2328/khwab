package com.toblad.khwab.logging

import android.content.Context
import java.io.File

object LogReader {

    private fun logDirectory(context: Context): File {
        return File(context.filesDir, "logs")
    }

    fun read(context: Context, fileName: String): String {

        val file = File(logDirectory(context), fileName)

        return if (file.exists()) {
            file.readText()
        } else {
            "No logs available."
        }
    }

    fun readError(context: Context): String {
        return read(context, "error.txt")
    }

    fun readSpeech(context: Context): String {
        return read(context, "speech.txt")
    }

    fun readAssistant(context: Context): String {
        return read(context, "assistant.txt")
    }

    fun readSystem(context: Context): String {
        return read(context, "system.txt")
    }
}