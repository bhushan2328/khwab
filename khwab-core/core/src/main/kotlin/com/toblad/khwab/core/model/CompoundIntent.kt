package com.toblad.khwab.core.model

/**
 * Represents a compound user request consisting of
 * multiple independent intents.
 *
 * Example:
 * "Open WhatsApp and message John"
 *
 * becomes:
 *  - OPEN_APP
 *  - SEND_MESSAGE
 */
data class CompoundIntent(
    val intents: List<IntentData>
) {
    fun isEmpty(): Boolean = intents.isEmpty()

    fun size(): Int = intents.size

    operator fun iterator(): Iterator<IntentData> = intents.iterator()
}
