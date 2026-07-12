package com.toblad.khwab.core.parser

import com.toblad.khwab.core.knowledge.KnowledgeBase
import com.toblad.khwab.core.model.Entity
import com.toblad.khwab.core.model.EntityType
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

object IntentParser {

    fun parse(text: String): IntentData {

        val tokens = Tokenizer.tokenize(text)

        if (tokens.isEmpty()) {
            return IntentData(
                intent = IntentType.UNKNOWN,
                originalText = text,
                confidence = 0f
            )
        }

        val intent = KnowledgeBase.findIntent(tokens.first())
            ?: IntentType.UNKNOWN

        val entities =
            if (tokens.size > 1)
                listOf(
                    Entity(
                        type = EntityType.UNKNOWN,
                        value = tokens.drop(1).joinToString(" ")
                    )
                )
            else
                emptyList()

        return IntentData(
            intent = intent,
            entities = entities,
            originalText = text,
            confidence = if (intent == IntentType.UNKNOWN) 0f else 0.95f
        )
    }
}
