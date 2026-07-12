package com.toblad.khwab.core.knowledge

/**
 * Removes knowledge from the KnowledgeStore.
 */
class ForgetSkill(

    private val store: KnowledgeStore

) {

    fun forget(key: String): Boolean {
        return store.remove(key)
    }
}
