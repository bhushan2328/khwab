package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.skill.skills.MediaSkill
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaSkillTest {

    @Test
    fun supportsPlayMediaIntent() {

        val skill = MediaSkill()

        val intent = IntentData(
            intent = IntentType.PLAY_MEDIA
        )

        assertTrue(skill.supports(intent))
    }

    @Test
    fun rejectsOtherIntent() {

        val skill = MediaSkill()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        assertFalse(skill.supports(intent))
    }
}
