package com.toblad.khwab.core.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextServiceTest {

    @Test
    fun engineImplementsContextService() {

        val service: ContextService = ContextEngine()

        service.setActiveContact("John")

        val result = service.resolve("him")

        assertTrue(result.resolved)
        assertEquals("John", result.resolvedReference)
    }
}
