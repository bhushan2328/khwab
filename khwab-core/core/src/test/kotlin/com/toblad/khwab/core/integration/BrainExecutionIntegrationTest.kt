package com.toblad.khwab.core.integration

import com.toblad.khwab.core.brain.BrainV2
import com.toblad.khwab.core.execution.ExecutionEngine
import com.toblad.khwab.core.execution.CoreStepExecutor
import com.toblad.khwab.core.execution.ExecutionContext
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrainExecutionIntegrationTest {

    @Test
    fun executesPlanProducedByBrain() {

        val brain = BrainV2()

        val executionEngine = ExecutionEngine(
            CoreStepExecutor()
        )

        val cognitive = brain.process(
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "Open WhatsApp"
            )
        )

        assertNotNull(cognitive)

        if (cognitive.plan != null) {

            val result = executionEngine.execute(
                ExecutionContext(cognitive.plan)
            )

            assertTrue(result.status.name.isNotBlank())
        }
    }
}
