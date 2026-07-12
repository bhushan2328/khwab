package com.toblad.khwab.core.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextEngineTest {

    @Test
    fun resolvesContactThroughEngine() {

        val engine = ContextEngine()

        engine.setActiveContact("John")

        val result = engine.resolve("him")

        assertTrue(result.resolved)
        assertEquals("John", result.resolvedReference)
    }

    @Test
    fun resolvesAppThroughEngine() {

        val engine = ContextEngine()

        engine.setActiveApp("WhatsApp")

        val result = engine.resolve("it")

        assertTrue(result.resolved)
        assertEquals("WhatsApp", result.resolvedReference)
    }
}
