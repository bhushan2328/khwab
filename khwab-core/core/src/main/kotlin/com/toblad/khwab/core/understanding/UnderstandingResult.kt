package com.toblad.khwab.core.understanding

import com.toblad.khwab.core.model.CompoundIntent

/**
 * Complete semantic output of the Understanding Engine.
 */
data class UnderstandingResult(

    val normalizedText: String,

    val requestType: RequestType,

    val compoundIntent: CompoundIntent,

    val resolvedEntities: List<ResolvedEntity> = emptyList(),

    val status: UnderstandingStatus = UnderstandingStatus.SUCCESS,

    val confidence: Float = 1.0f
)
