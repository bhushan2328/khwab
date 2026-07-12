package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.skill.skills.CallSkill
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallSkillTest {

    @Test
    fun supportsCallContactIntent() {

        val skill = CallSkill()

        val intent = IntentData(
            intent = IntentType.CALL_CONTACT
        )

        assertTrue(skill.supports(intent))
    }

    @Test
    fun rejectsOtherIntent() {

        val skill = CallSkill()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        assertFalse(skill.supports(intent))
    }
}
