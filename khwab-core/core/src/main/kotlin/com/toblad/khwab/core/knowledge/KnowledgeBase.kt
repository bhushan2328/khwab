package com.toblad.khwab.core.knowledge

import com.toblad.khwab.core.model.IntentType

object KnowledgeBase {

    fun findIntent(word: String): IntentType? =
        CommandAliasRegistry.findIntent(
            SynonymDictionary.normalize(word)
        )

    fun aliases(intent: IntentType): Set<String> =
        CommandAliasRegistry.aliases(intent)

    fun supports(intent: IntentType): Boolean =
        CapabilityRegistry.supports(intent)
}
