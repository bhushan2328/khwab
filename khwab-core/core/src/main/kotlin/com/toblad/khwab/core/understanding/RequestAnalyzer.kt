package com.toblad.khwab.core.understanding

/**
 * Determines the structure of a user's request.
 *
 * This implementation is intentionally simple and will evolve
 * into a richer analysis engine in later milestones.
 */
class RequestAnalyzer {

    private val connectors = listOf(
        " and ",
        " then ",
        " after that "
    )

    fun analyze(text: String): RequestType {
        val input = text.lowercase()

        return if (connectors.any { input.contains(it) }) {
            RequestType.COMPOUND
        } else {
            RequestType.SINGLE
        }
    }
}
