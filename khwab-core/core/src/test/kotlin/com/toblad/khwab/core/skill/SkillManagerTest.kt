package com.toblad.khwab.core.skill

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

class SkillManagerTest {

    @Test
    fun findsAppSkillForOpenApp() {

        val manager = SkillManager()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        val skill = manager.findSkill(intent)

        assertNotNull(skill)
        assertTrue(skill.supports(intent))
    }

    @Test
    fun returnsNullForUnknownIntent() {

        val manager = SkillManager()

        val intent = IntentData(
            intent = IntentType.UNKNOWN
        )

        val skill = manager.findSkill(intent)

        assertNull(skill)
    }
}
