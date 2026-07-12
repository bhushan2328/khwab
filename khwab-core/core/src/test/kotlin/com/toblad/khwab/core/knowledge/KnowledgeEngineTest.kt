package com.toblad.khwab.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnowledgeEngineTest {

    @Test
    fun completeKnowledgeLifecycle() {

        val engine = KnowledgeEngine()

        engine.remember(
            key = "favorite_app",
            value = "Spotify"
        )

        val record = engine.recall("favorite_app")

        assertNotNull(record)
        assertEquals("Spotify", record.value)

        assertTrue(
            engine.forget("favorite_app")
        )

        assertNull(
            engine.recall("favorite_app")
        )
    }
}
