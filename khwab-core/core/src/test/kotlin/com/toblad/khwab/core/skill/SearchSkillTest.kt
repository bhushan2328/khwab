package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.skill.skills.SearchSkill
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchSkillTest {

    @Test
    fun supportsSearchIntent() {

        val skill = SearchSkill()

        val intent = IntentData(
            intent = IntentType.SEARCH_WEB
        )

        assertTrue(skill.supports(intent))
    }

    @Test
    fun rejectsOtherIntent() {

        val skill = SearchSkill()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        assertFalse(skill.supports(intent))
    }
}
