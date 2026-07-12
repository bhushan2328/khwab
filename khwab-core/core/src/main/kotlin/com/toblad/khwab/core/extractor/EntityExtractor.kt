package com.toblad.khwab.core.extractor

import com.toblad.khwab.core.model.Entity
import com.toblad.khwab.core.model.EntityType

object EntityExtractor {

    fun extract(text: String): List<Entity> {

        return listOf(
            Entity(
                type = EntityType.UNKNOWN,
                value = text.trim(),
                confidence = 1.0f
            )
        )
    }
}
