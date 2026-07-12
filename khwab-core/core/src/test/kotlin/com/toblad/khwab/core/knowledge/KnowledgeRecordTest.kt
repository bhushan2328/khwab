package com.toblad.khwab.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals

class KnowledgeRecordTest {

    @Test
    fun createsKnowledgeRecord() {

        val record = KnowledgeRecord(
            key = "favorite_app",
            value = "Spotify",
            type = KnowledgeType.USER_PREFERENCE
        )

        assertEquals("favorite_app", record.key)
        assertEquals("Spotify", record.value)
        assertEquals(KnowledgeType.USER_PREFERENCE, record.type)
        assertEquals(KnowledgeSource.USER, record.source)
        assertEquals(1.0f, record.confidence)
    }
}
