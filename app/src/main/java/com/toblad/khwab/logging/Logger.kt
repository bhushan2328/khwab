package com.toblad.khwab.logging

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private var initialized = false

    fun initialize(context: Context) {
        if (!initialized) {
            LogWriter.initialize(context)
            initialized = true
        }
    }

    fun debug(module: LogModule, message: String) {
        log(LogLevel.DEBUG, module, message)
    }

    fun info(module: LogModule, message: String) {
        log(LogLevel.INFO, module, message)
    }

    fun warning(module: LogModule, message: String) {
        log(LogLevel.WARNING, module, message)
    }

    fun error(
        module: LogModule,
        message: String,
        throwable: Throwable? = null
    ) {
        log(LogLevel.ERROR, module, message, throwable)
    }

    private fun log(
        level: LogLevel,
        module: LogModule,
        message: String,
        throwable: Throwable? = null
    ) {

        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val builder = StringBuilder()

        builder.appendLine("========================================")
        builder.appendLine("Time      : $timestamp")
        builder.appendLine("Level     : $level")
        builder.appendLine("Module    : $module")
        builder.appendLine("Message   : $message")

        throwable?.let {
            builder.appendLine()
            builder.appendLine("Exception : ${it::class.java.name}")
            builder.appendLine("Reason    : ${it.message}")
            builder.appendLine()
            builder.appendLine(it.stackTraceToString())
        }

        builder.appendLine("========================================")
        builder.appendLine()

        val fileName = when (module) {
            LogModule.SYSTEM -> "system.txt"
            LogModule.ASSISTANT -> "assistant.txt"
            LogModule.SPEECH -> "speech.txt"
            LogModule.BRAIN -> "brain.txt"
            LogModule.EXECUTION -> "execution.txt"
            LogModule.ACCESSIBILITY -> "accessibility.txt"
            LogModule.OVERLAY -> "overlay.txt"
            LogModule.MEMORY -> "memory.txt"
            LogModule.AI -> "ai.txt"
            LogModule.UI -> "ui.txt"
        }

        LogWriter.write(fileName, builder.toString())

        if (level == LogLevel.ERROR) {
            LogWriter.write("error.txt", builder.toString())
        }
    }
}