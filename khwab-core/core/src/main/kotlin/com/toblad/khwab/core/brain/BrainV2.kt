package com.toblad.khwab.core.brain

import com.toblad.khwab.core.conversation.ConversationEngine
import com.toblad.khwab.core.decision.Decision
import com.toblad.khwab.core.decision.DecisionEngine
import com.toblad.khwab.core.knowledge.KnowledgeEngine
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.reasoning.ReasoningEngine
import com.toblad.khwab.core.skill.SkillManager

class BrainV2(

    private val conversationEngine: ConversationEngine = ConversationEngine(),
    private val knowledgeEngine: KnowledgeEngine = KnowledgeEngine(),
    private val reasoningEngine: ReasoningEngine = ReasoningEngine(),
    private val decisionEngine: DecisionEngine = DecisionEngine(),
    private val skillManager: SkillManager = SkillManager()

) {

    fun process(intent: IntentData): CognitiveResult {

        conversationEngine.process(intent)

        val reasoning = reasoningEngine.analyze(intent)

        val decision = decisionEngine.decide(reasoning)

        val plan =
            if (decision.decision == Decision.EXECUTE) {
                skillManager.findSkill(intent)?.createPlan(intent)
            } else {
                null
            }

        return CognitiveResult(
            intent = intent,
            reasoning = reasoning,
            decision = decision,
            plan = plan
        )
    }

    fun conversation() = conversationEngine

    fun knowledge() = knowledgeEngine

    fun rememberPreference(
        key: String,
        value: String
    ) {
        knowledgeEngine.remember(
            key = key,
            value = value
        )
    }

    fun recallPreference(
        key: String
    ): String? {
        return knowledgeEngine
            .recall(key)
            ?.value
    }

    fun forgetPreference(
        key: String
    ): Boolean {
        return knowledgeEngine.forget(key)
    }
}
