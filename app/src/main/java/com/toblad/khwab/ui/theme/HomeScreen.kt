package com.toblad.khwab.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

@Composable
fun HomeScreen(
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {}
) {

    val assistantState = AssistantStateManager.state

    val statusColor = when (assistantState) {
        AssistantState.STOPPED -> KhwabRed
        AssistantState.READY -> KhwabBlue
        AssistantState.RUNNING -> KhwabGreen
        AssistantState.LISTENING -> KhwabBlue
        AssistantState.THINKING -> KhwabYellow
        AssistantState.EXECUTING -> KhwabGreen
        AssistantState.SPEAKING -> KhwabBlue
        AssistantState.ERROR -> KhwabRed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KhwabBackground,
                        KhwabSurface
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            HeaderSection()

            Spacer(modifier = Modifier.height(30.dp))

            StatusCard(
                status = assistantState.name,
                statusColor = statusColor
            )

            Spacer(modifier = Modifier.height(30.dp))

            MicButton(
                onClick = {}
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(modifier = Modifier.weight(1f)) {
                    ActionButton(
                        text = "START",
                        backgroundColor = KhwabGreen,
                        onClick = onStartClick
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ActionButton(
                        text = "STOP",
                        backgroundColor = KhwabRed,
                        onClick = onStopClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            InfoCard(
                onChatClick = {}
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}