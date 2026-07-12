package com.toblad.khwab.core.conversation

object ReferenceResolver {

    fun resolve(
        word: String,
        memory: ConversationMemory
    ): String? {

        return when (word.lowercase()) {
            "it",
            "him",
            "her",
            "again",
            "that" -> memory.recall()?.value

            else -> word
        }
    }
}
