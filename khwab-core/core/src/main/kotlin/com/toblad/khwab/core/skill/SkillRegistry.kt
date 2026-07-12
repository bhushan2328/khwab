package com.toblad.khwab.core.skill

import com.toblad.khwab.core.skill.skills.*

class SkillRegistry {

    val skills: List<Skill> = listOf(
        AppSkill(),
        CallSkill(),
        MessageSkill(),
        MediaSkill(),
        SearchSkill(),
        SettingsSkill(),
        MemorySkill()
    )
}
