package com.toblad.khwab.core.validator

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

class Validator {

    fun isValid(intent: IntentData): Boolean {
        return intent.intent != IntentType.UNKNOWN
    }
}
