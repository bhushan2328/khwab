package com.toblad.khwab.logging

import android.content.Context
import java.io.File

object LogWriter {

    private lateinit var logDirectory: File

    fun initialize(context: Context) {
        logDirectory = File(context.filesDir, "logs")

        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    fun write(fileName: String, text: String) {

        if (!::logDirectory.isInitialized) {
            return
        }

        val file = File(logDirectory, fileName)

        file.appendText(text + "\n")
    }
}