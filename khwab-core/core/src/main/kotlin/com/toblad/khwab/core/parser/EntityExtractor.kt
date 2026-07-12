package com.toblad.khwab.core.parser

import com.toblad.khwab.core.model.Entity
import com.toblad.khwab.core.model.EntityType

class EntityExtractor {

    fun extractApp(name: String): Entity {
        return Entity(
            type = EntityType.APP,
            value = name.trim()
        )
    }

    fun extractContact(name: String): Entity {
        return Entity(
            type = EntityType.CONTACT,
            value = name.trim()
        )
    }

    fun extractMedia(name: String): Entity {
        return Entity(
            type = EntityType.MEDIA,
            value = name.trim()
        )
    }
}
