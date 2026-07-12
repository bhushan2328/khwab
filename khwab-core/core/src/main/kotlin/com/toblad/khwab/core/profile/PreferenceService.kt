package com.toblad.khwab.core.profile

import com.toblad.khwab.core.knowledge.KnowledgeEngine
import com.toblad.khwab.core.knowledge.KnowledgeType

/**
 * High-level API for user preferences.
 */
class PreferenceService(

    private val knowledge: KnowledgeEngine = KnowledgeEngine()

) {

    fun setFavoriteApp(app: String) {
        knowledge.remember(
            key = "favorite_app",
            value = app,
            type = KnowledgeType.USER_PREFERENCE
        )
    }

    fun favoriteApp(): String? {
        return knowledge.recall("favorite_app")?.value
    }

    fun setFavoriteFood(food: String) {
        knowledge.remember(
            key = "favorite_food",
            value = food,
            type = KnowledgeType.USER_PREFERENCE
        )
    }

    fun favoriteFood(): String? {
        return knowledge.recall("favorite_food")?.value
    }
}
