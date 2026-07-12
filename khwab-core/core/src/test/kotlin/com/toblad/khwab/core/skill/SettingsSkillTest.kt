package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.skill.skills.SettingsSkill
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsSkillTest {

    @Test
    fun supportsSettingsIntent() {

        val skill = SettingsSkill()

        val intent = IntentData(
            intent = IntentType.CHANGE_SETTING
        )

        assertTrue(skill.supports(intent))
    }

    @Test
    fun rejectsOtherIntent() {

        val skill = SettingsSkill()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        assertFalse(skill.supports(intent))
    }
}
