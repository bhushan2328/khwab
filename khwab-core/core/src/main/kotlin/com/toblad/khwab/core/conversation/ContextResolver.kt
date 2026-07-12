package com.toblad.khwab.core.conversation

class ContextResolver(
    private val memory: ConversationMemory
) {

    fun resolve(text: String): String {

        val words = text.split(" ").toMutableList()

        for (i in words.indices) {

            val resolved = ReferenceResolver.resolve(
                words[i],
                memory
            )

            if (resolved != null)
                words[i] = resolved
        }

        return words.joinToString(" ")
    }
}
