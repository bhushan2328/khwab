package com.toblad.khwab.logging

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val module: LogModule,
    val message: String,
    val throwable: Throwable? = null
)