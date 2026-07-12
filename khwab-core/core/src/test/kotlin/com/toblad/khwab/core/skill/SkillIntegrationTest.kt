package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.skill.skills.*
import kotlin.test.Test
import kotlin.test.assertTrue

class SkillIntegrationTest {

    private val manager = SkillManager()

    @Test
    fun openAppMapsToAppSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.OPEN_APP))
        assertTrue(skill is AppSkill)
    }

    @Test
    fun callMapsToCallSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.CALL_CONTACT))
        assertTrue(skill is CallSkill)
    }

    @Test
    fun messageMapsToMessageSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.SEND_MESSAGE))
        assertTrue(skill is MessageSkill)
    }

    @Test
    fun mediaMapsToMediaSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.PLAY_MEDIA))
        assertTrue(skill is MediaSkill)
    }

    @Test
    fun searchMapsToSearchSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.SEARCH_WEB))
        assertTrue(skill is SearchSkill)
    }

    @Test
    fun settingsMapsToSettingsSkill() {
        val skill = manager.findSkill(IntentData(intent = IntentType.CHANGE_SETTING))
        assertTrue(skill is SettingsSkill)
    }
}
