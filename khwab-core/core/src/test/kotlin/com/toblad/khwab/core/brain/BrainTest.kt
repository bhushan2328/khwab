package com.toblad.khwab.core.brain

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BrainTest {

    @Test
    fun brainProcessesIntent() {

        val brain = BrainV2()

        val result = brain.process(
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "Open WhatsApp"
            )
        )

        assertNotNull(result)

        assertEquals(
            IntentType.OPEN_APP,
            result.intent.intent
        )

        assertNotNull(result.reasoning)
        assertNotNull(result.decision)

        println("===================================")
        println("Decision = ${result.decision.decision}")
        println("Plan = ${result.plan}")
        println("===================================")
    }
}
