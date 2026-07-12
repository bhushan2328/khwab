package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData

class SkillManager(
    private val registry: SkillRegistry = SkillRegistry()
) {

    /**
     * Returns the first matching skill.
     * Backward-compatible API.
     */
    fun findSkill(intent: IntentData): Skill? {
        return registry.skills.firstOrNull { it.supports(intent) }
    }

    /**
     * Returns all skills that support the intent.
     * Used for future multi-skill planning.
     */
    fun findMatchingSkills(intent: IntentData): List<Skill> {
        return registry.skills.filter { it.supports(intent) }
    }
}
