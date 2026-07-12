package com.toblad.khwab.core.understanding

import com.toblad.khwab.core.model.Entity

/**
 * Resolves parser entities into canonical entities.
 *
 * This is the foundation for future app,
 * contact, location and knowledge resolution.
 */
class EntityResolver {

    fun resolve(entities: List<Entity>): List<ResolvedEntity> {
        return entities.map {
            ResolvedEntity(
                type = it.type.name,
                value = it.value,
                canonical = it.value.lowercase(),
                confidence = 1.0f
            )
        }
    }
}
