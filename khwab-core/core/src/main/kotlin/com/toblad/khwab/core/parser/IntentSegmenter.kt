package com.toblad.khwab.core.parser

/**
 * Splits a raw user request into independent command segments.
 *
 * NOTE:
 * Initial implementation supports simple conjunctions.
 * More advanced NLP-based segmentation will be added later.
 */
class IntentSegmenter {

    private val connectors = listOf(
        " and ",
        " then ",
        " after that "
    )

    fun segment(text: String): List<String> {
        var segments = listOf(text.trim())

        for (connector in connectors) {
            segments = segments.flatMap { part ->
                part.split(connector)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
            }
        }

        return segments
    }
}
