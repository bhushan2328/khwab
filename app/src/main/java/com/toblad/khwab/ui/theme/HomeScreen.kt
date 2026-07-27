package com.toblad.khwab.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

@Composable
fun HomeScreen(
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
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
            .background(KhwabBackground)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            HeaderSection()

            Spacer(modifier = Modifier.height(40.dp))

            MicButton(
                onClick = {}
            )

            Spacer(modifier = Modifier.height(36.dp))

            StatusCard(
                status = assistantState.name,
                statusColor = statusColor,
                message = "Say \"Hey Khwab\" or tap the microphone to begin."
            )

            Spacer(modifier = Modifier.height(36.dp))

            ActionButton(
                text = "Start Assistant",
                icon = Icons.Default.Mic,
                backgroundColor = KhwabGreen,
                onClick = onStartClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                text = "Stop Assistant",
                icon = Icons.Default.StopCircle,
                backgroundColor = KhwabRed,
                onClick = onStopClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                text = "Chat With Khwab",
                icon = Icons.Default.Chat,
                backgroundColor = KhwabBlue,
                onClick = onChatClick
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}