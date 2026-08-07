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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toblad.khwab.aura.ui.AuraScene
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

@Composable
fun HomeScreen(
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val assistantState = AssistantStateManager.state

    val auraTheme = ThemeController.currentAuraTheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val icons = AuraIconProvider.homeIcons(
        auraActive = auraActive,
        weather = auraTheme.weatherState,
        timePhase = auraTheme.timePhase
    )

    val statusColor = when (assistantState) {
        AssistantState.STOPPED  -> colors.error
        AssistantState.READY    -> colors.primary
        AssistantState.RUNNING  -> colors.secondary
        AssistantState.LISTENING -> colors.primary
        AssistantState.THINKING  -> colors.tertiary
        AssistantState.EXECUTING -> colors.secondary
        AssistantState.SPEAKING  -> colors.primary
        AssistantState.ERROR     -> colors.error
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (auraActive) {
            AuraScene(
                theme = auraTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

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

            MicButton(onClick = onStartClick)

            Spacer(modifier = Modifier.height(36.dp))

            StatusCard(
                status = assistantState.name,
                statusColor = statusColor,
                message = "Say \"Hey Khwab\" or tap the microphone to begin."
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Adaptive Start / Stop ─────────────────────────────────────────
            // Show only one contextually relevant action at a time.
            // If the assistant is stopped/in error → Start; otherwise → Stop.
            if (assistantState == AssistantState.STOPPED || assistantState == AssistantState.ERROR) {
                ActionButton(
                    text = "Start Assistant",
                    icon = icons.start,
                    backgroundColor = colors.secondary,
                    modifier = Modifier.fillMaxWidth(0.88f),
                    onClick = onStartClick
                )
            } else {
                ActionButton(
                    text = "Stop Assistant",
                    icon = icons.stop,
                    backgroundColor = colors.error,
                    modifier = Modifier.fillMaxWidth(0.88f),
                    onClick = onStopClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Secondary actions: Chat + Settings side by side ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    text = "Chat",
                    icon = icons.chat,
                    backgroundColor = colors.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onChatClick
                )
                ActionButton(
                    text = "Settings",
                    icon = icons.settings,
                    backgroundColor = colors.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = onSettingsClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
