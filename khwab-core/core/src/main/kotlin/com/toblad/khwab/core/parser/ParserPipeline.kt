package com.toblad.khwab.core.parser

import com.toblad.khwab.core.model.CompoundIntent

/**
 * Coordinates the parser pipeline.
 */
class ParserPipeline(
    private val segmenter: IntentSegmenter = IntentSegmenter()
) {

    fun parse(text: String): CompoundIntent {
        val intents = segmenter
            .segment(text)
            .map { IntentParser.parse(it) }

        return CompoundIntent(intents)
    }
}
