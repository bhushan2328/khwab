package com.toblad.khwab.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val state = AssistantStateManager.state

    val auraTheme = ThemeController.currentAuraTheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA

    val icon = AuraIconProvider.micIconFor(
        weather = auraTheme.weatherState,
        timePhase = auraTheme.timePhase
    )

    // ── Pulse animation config per state ─────────────────────────────────────
    data class PulseConfig(
        val targetScale: Float,
        val durationMs: Int,
        val active: Boolean
    )

    val pulseConfig = when (state) {
        AssistantState.STOPPED, AssistantState.ERROR ->
            PulseConfig(targetScale = 1.0f, durationMs = 1800, active = false)
        AssistantState.READY ->
            PulseConfig(targetScale = 1.06f, durationMs = 1800, active = true)
        AssistantState.LISTENING ->
            PulseConfig(targetScale = 1.13f, durationMs = 550, active = true)
        else ->
            PulseConfig(targetScale = 1.08f, durationMs = 900, active = true)
    }

    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulseConfig.active) pulseConfig.targetScale else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseConfig.durationMs,
                easing = if (state == AssistantState.LISTENING) LinearEasing
                         else FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    // Ring alpha pulses independently — visible only in active states
    val ringAlpha by transition.animateFloat(
        initialValue = if (pulseConfig.active) 0.18f else 0f,
        targetValue  = if (pulseConfig.active) 0.42f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseConfig.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_ring_alpha"
    )

    val buttonSize: Dp = 160.dp
    val ringSize: Dp   = 196.dp

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        // ── Pulsing glow ring (behind the button) ─────────────────────────────
        if (pulseConfig.active) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = ringAlpha))
            )
        }

        // ── Mic card ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .size(buttonSize)
                .scale(pulseScale)
                .clickable(onClick = onClick),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = colors.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        // Fixed: use primaryContainer → primary → background
                        // (old code used tertiary = red in center)
                        Brush.radialGradient(
                            colors = listOf(
                                colors.primaryContainer,
                                colors.primary,
                                colors.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (auraActive) icon else Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}
