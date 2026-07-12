package com.toblad.khwab.core.extractor

object ContactNameExtractor {

    private val prefixes = listOf(
        "call",
        "dial",
        "message",
        "text",
        "sms"
    )

    fun extract(command: String): String {

        var text = command.trim()

        prefixes.forEach {
            if (text.startsWith(it, ignoreCase = true)) {
                text = text.substring(it.length).trim()
            }
        }

        return text
    }
}
