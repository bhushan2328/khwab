package com.toblad.khwab.core.parser

class CommandTokenizer {

    fun tokenize(text: String): List<String> {
        return text
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }
}
