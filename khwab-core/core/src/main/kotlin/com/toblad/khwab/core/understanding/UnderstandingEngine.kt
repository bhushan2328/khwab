package com.toblad.khwab.core.understanding

import com.toblad.khwab.core.parser.ParserPipeline

/**
 * Entry point for the Understanding subsystem.
 */
class UnderstandingEngine(
    private val normalizer: InputNormalizer = InputNormalizer(),
    private val analyzer: RequestAnalyzer = RequestAnalyzer(),
    private val parser: ParserPipeline = ParserPipeline()
) {

    fun understand(text: String): UnderstandingResult {

        val normalized = normalizer.normalize(text)

        val requestType = analyzer.analyze(normalized)

        val compoundIntent = parser.parse(normalized)

        return UnderstandingResult(
            normalizedText = normalized,
            requestType = requestType,
            compoundIntent = compoundIntent,
            resolvedEntities = emptyList(),
            status = UnderstandingStatus.SUCCESS,
            confidence = if (requestType == RequestType.SINGLE) 1.0f else 0.95f
        )
    }
}
