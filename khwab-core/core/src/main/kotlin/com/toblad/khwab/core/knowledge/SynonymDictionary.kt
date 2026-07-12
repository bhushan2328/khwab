package com.toblad.khwab.core.knowledge

object SynonymDictionary {

    private val synonyms = mapOf(
        "launch" to "open",
        "start" to "open",
        "run" to "open",
        "dial" to "call",
        "phone" to "call",
        "text" to "message",
        "sms" to "message",
        "listen" to "play",
        "google" to "search",
        "find" to "search"
    )

    fun normalize(word: String): String =
        synonyms[word.lowercase()] ?: word.lowercase()
}
