package com.toblad.khwab.core.conversation

/**
 * Stores recent conversation events.
 */
class ConversationHistory(

    private val maxSize: Int = 20

) {

    private val entries = mutableListOf<String>()

    fun add(entry: String) {

        entries += entry

        if (entries.size > maxSize) {
            entries.removeAt(0)
        }
    }

    fun latest(): String? = entries.lastOrNull()

    fun previous(): String? =
        if (entries.size >= 2)
            entries[entries.size - 2]
        else
            null

    fun all(): List<String> = entries.toList()

    fun clear() {
        entries.clear()
    }
}
