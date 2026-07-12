package com.toblad.khwab.core.planner

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

class RuleEngine {

    fun isSupported(intent: IntentData): Boolean {

        return intent.intent != IntentType.UNKNOWN

    }

}
