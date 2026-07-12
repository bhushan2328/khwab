package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.Plan

interface Skill {

    fun supports(intent: IntentData): Boolean

    fun createPlan(intent: IntentData): Plan
}
