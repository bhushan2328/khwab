package com.toblad.khwab.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals

class KnowledgeEntryTest {

    @Test
    fun createsKnowledgeEntry() {

        val entry = KnowledgeEntry(
            key = "favorite_app",
            value = "Spotify"
        )

        assertEquals("favorite_app", entry.key)
        assertEquals("Spotify", entry.value)
        assertEquals("user", entry.source)
    }
}
