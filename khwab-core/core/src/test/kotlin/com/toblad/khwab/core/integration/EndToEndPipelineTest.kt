package com.toblad.khwab.core.integration

import com.toblad.khwab.core.brain.BrainV2
import com.toblad.khwab.core.execution.ExecutionContext
import com.toblad.khwab.core.execution.ExecutionEngine
import com.toblad.khwab.core.execution.ExecutionStatus
import com.toblad.khwab.core.execution.MockStepExecutor
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndToEndPipelineTest {

    @Test
    fun endToEndPipeline() {

        // Simulated parser output
        val intent = IntentData(
            intent = IntentType.OPEN_APP,
            originalText = "Open WhatsApp"
        )

        val brain = BrainV2()

        val cognitive = brain.process(intent)

        assertNotNull(cognitive)

        if (cognitive.plan != null) {

            val engine = ExecutionEngine(
                MockStepExecutor()
            )

            val result = engine.execute(
                ExecutionContext(cognitive.plan)
            )

            assertTrue(
                result.status == ExecutionStatus.SUCCESS ||
                result.status == ExecutionStatus.PARTIAL_SUCCESS
            )
        }
    }
}
