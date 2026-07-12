package com.toblad.khwab.core.parser

object Tokenizer {

    fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }
}
