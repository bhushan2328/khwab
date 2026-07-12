package com.toblad.khwab.core.brain

import kotlin.test.Test
import kotlin.test.assertNotNull

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

class BrainV2Test {

    @Test
    fun brainV2CanProcessIntent() {

        val brain = BrainV2()

        val intent = IntentData(
            intent = IntentType.OPEN_APP
        )

        val result = brain.process(intent)

        assertNotNull(result)
        assertNotNull(result.reasoning)
        assertNotNull(result.decision)
    }
}
