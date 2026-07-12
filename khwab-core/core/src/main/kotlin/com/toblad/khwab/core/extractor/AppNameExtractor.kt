package com.toblad.khwab.core.extractor

object AppNameExtractor {

    private val prefixes = listOf(
        "open",
        "launch",
        "start",
        "run"
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
