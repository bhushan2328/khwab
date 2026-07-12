package com.toblad.khwab.core.understanding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnderstandingEngineTest {

    private val engine = UnderstandingEngine()

    @Test
    fun understandsSingleRequest() {
        val result = engine.understand("Open WhatsApp")

        assertEquals(RequestType.SINGLE, result.requestType)
        assertEquals(UnderstandingStatus.SUCCESS, result.status)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun understandsCompoundRequest() {
        val result = engine.understand("Open WhatsApp and search Google")

        assertEquals(RequestType.COMPOUND, result.requestType)
        assertEquals(UnderstandingStatus.SUCCESS, result.status)
        assertTrue(result.confidence > 0f)
    }
}
