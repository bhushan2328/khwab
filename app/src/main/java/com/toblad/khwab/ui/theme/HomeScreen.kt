package com.toblad.khwab.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.ui.AuraScene
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

// State-specific status messages — contextually meaningful for each assistant state
private fun statusMessage(state: AssistantState) = when (state) {
    AssistantState.STOPPED   -> "Tap the mic or press Start to wake Khwab."
    AssistantState.ERROR     -> "Something went wrong. Tap Start to try again."
    AssistantState.READY     -> "Say \"Hey Khwab\" or tap the microphone to begin."
    AssistantState.RUNNING   -> "Listening for wake word…"
    AssistantState.LISTENING -> "I'm listening — go ahead."
    AssistantState.THINKING  -> "Processing your request…"
    AssistantState.EXECUTING -> "Running your command…"
    AssistantState.SPEAKING  -> "Speaking…"
}

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
        weather    = auraTheme.weatherState,
        timePhase  = auraTheme.timePhase
    )

    val statusColor = when (assistantState) {
        AssistantState.STOPPED   -> colors.error
        AssistantState.READY     -> colors.primary
        AssistantState.RUNNING   -> colors.secondary
        AssistantState.LISTENING -> colors.primary
        AssistantState.THINKING  -> colors.tertiary
        AssistantState.EXECUTING -> colors.secondary
        AssistantState.SPEAKING  -> colors.primary
        AssistantState.ERROR     -> colors.error
    }

    // Fade-in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "home_fade_in"
    )

    // When Aura is off, show a time-aware gradient fallback so the home screen
    // doesn't stay pure dark-navy during daytime. Background only — no particles.
    val fallbackBg: Modifier = if (!auraActive) {
        val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
        val (topC, botC) = when {
            hour in 5..7   -> Color(0xFF1A1A3E) to Color(0xFFFF9E80)  // dawn
            hour in 8..11  -> Color(0xFF4FC3F7) to Color(0xFFFFF8E1)  // morning
            hour in 12..14 -> Color(0xFF1565C0) to Color(0xFFE3F2FD)  // noon
            hour in 15..17 -> Color(0xFF0D47A1) to Color(0xFFFFF9C4)  // afternoon
            hour in 18..20 -> Color(0xFFFF7043) to Color(0xFF5E35B1)  // sunset
            hour in 21..22 -> Color(0xFF3949AB) to Color(0xFF7986CB)  // evening
            else           -> Color(0xFF0D1B2A) to Color(0xFF000814)  // night
        }
        Modifier.background(
            Brush.verticalGradient(listOf(topC, botC))
        )
    } else {
        Modifier.background(colors.background)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(fallbackBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (auraActive) {
            AuraScene(theme = auraTheme, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            HeaderSection()

            Spacer(modifier = Modifier.height(40.dp))

            MicButton(onClick = onStartClick)

            Spacer(modifier = Modifier.height(32.dp))

            // Status message cross-fades when the assistant state changes
            StatusCard(
                status      = assistantState.name,
                statusColor = statusColor,
                message     = statusMessage(assistantState)
            )

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 4.dp),
                color     = colors.outline.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Adaptive Start / Stop — cross-fades via AnimatedContent ──────
            val isStopped = assistantState == AssistantState.STOPPED
                         || assistantState == AssistantState.ERROR

            AnimatedContent(
                targetState = isStopped,
                transitionSpec = {
                    (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f))
                },
                label = "start_stop_btn"
            ) { stopped ->
                if (stopped) {
                    ActionButton(
                        text            = "Start Assistant",
                        icon            = icons.start,
                        backgroundColor = colors.secondary,
                        modifier        = Modifier.fillMaxWidth(0.88f),
                        onClick         = onStartClick
                    )
                } else {
                    ActionButton(
                        text            = "Stop Assistant",
                        icon            = icons.stop,
                        backgroundColor = colors.error,
                        modifier        = Modifier.fillMaxWidth(0.88f),
                        onClick         = onStopClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Secondary actions: outlined for visual hierarchy ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    text            = "Chat",
                    icon            = icons.chat,
                    backgroundColor = colors.primary,
                    outlined        = true,
                    modifier        = Modifier.weight(1f),
                    onClick         = onChatClick
                )
                ActionButton(
                    text            = "Settings",
                    icon            = icons.settings,
                    backgroundColor = colors.tertiary,
                    outlined        = true,
                    modifier        = Modifier.weight(1f),
                    onClick         = onSettingsClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
