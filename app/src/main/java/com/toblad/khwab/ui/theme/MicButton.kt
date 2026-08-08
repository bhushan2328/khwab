package com.toblad.khwab.ui.theme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val state by AssistantStateManager.stateFlow.collectAsState()
    val haptic = LocalHapticFeedback.current

    val auraTheme = ThemeController.currentAuraTheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA

    val auraIcon = AuraIconProvider.micIconFor(
        weather = auraTheme.weatherState,
        timePhase = auraTheme.timePhase
    )

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

    val isListening = state == AssistantState.LISTENING

    val transition = rememberInfiniteTransition(label = "mic_pulse")

    // ── Inner ring / button scale ─────────────────────────────────────────────
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (pulseConfig.active) pulseConfig.targetScale else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseConfig.durationMs,
                easing = if (isListening) LinearEasing else FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    // ── Inner ring alpha ──────────────────────────────────────────────────────
    val innerRingAlpha by transition.animateFloat(
        initialValue = if (pulseConfig.active) 0.18f else 0f,
        targetValue  = if (pulseConfig.active) 0.42f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseConfig.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_ring_alpha"
    )

    // ── Outer ring: opposite phase, half speed, only when LISTENING ───────────
    val outerRingScale by transition.animateFloat(
        initialValue = if (isListening) 1.05f else 1f,
        targetValue  = if (isListening) 1.22f  else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseConfig.durationMs * 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_ring_scale"
    )
    val outerRingAlpha by transition.animateFloat(
        initialValue = if (isListening) 0.18f else 0f,
        targetValue  = if (isListening) 0f    else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseConfig.durationMs * 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_ring_alpha"
    )

    val buttonSize: Dp = 160.dp
    val ringSize: Dp   = 196.dp
    val outerRingSize: Dp = 224.dp

    // Ring colour strategy:
    //   • Daytime Aura sky → white rings (they'd disappear on a bright background)
    //   • Dark Aura sky (night/storm/pre-dawn etc.) → brighten the primary colour
    //     so rings are clearly visible against the very dark canvas
    //   • Non-Aura → standard primary as before
    val isDaytimeAura = auraActive && ThemeController.currentAuraTheme.timePhase in listOf(
        com.toblad.khwab.aura.model.TimePhase.SUNRISE,
        com.toblad.khwab.aura.model.TimePhase.MORNING,
        com.toblad.khwab.aura.model.TimePhase.NOON,
        com.toblad.khwab.aura.model.TimePhase.AFTERNOON
    )
    val ringColor = when {
        isDaytimeAura -> Color.White
        auraActive    -> Color(
            red   = (colors.primary.red   * 1.35f).coerceAtMost(1f),
            green = (colors.primary.green * 1.35f).coerceAtMost(1f),
            blue  = (colors.primary.blue  * 1.35f).coerceAtMost(1f),
            alpha = 1f
        )
        else          -> colors.primary
    }

    Box(
        modifier = modifier.size(outerRingSize),
        contentAlignment = Alignment.Center
    ) {
        // ── Outer ripple ring — only LISTENING ────────────────────────────────
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(outerRingSize)
                    .scale(outerRingScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = outerRingAlpha))
            )
        }

        // ── Inner glow ring ───────────────────────────────────────────────────
        if (pulseConfig.active) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = innerRingAlpha))
            )
        }

        // ── Mic card ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .size(buttonSize)
                .scale(pulseScale),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = colors.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        ) {
            // In daytime Aura mode the mic edge fades to transparent instead of
            // dark navy so the sky shows through naturally behind the button.
            val auraActive2 = ThemeController.currentTheme == ThemeMode.AURA
            val isDaytimeMic = auraActive2 && ThemeController.currentAuraTheme.timePhase in listOf(
                com.toblad.khwab.aura.model.TimePhase.SUNRISE,
                com.toblad.khwab.aura.model.TimePhase.MORNING,
                com.toblad.khwab.aura.model.TimePhase.NOON,
                com.toblad.khwab.aura.model.TimePhase.AFTERNOON
            )
            val gradientEdge = if (isDaytimeMic) androidx.compose.ui.graphics.Color.Transparent
                               else colors.background

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.primaryContainer,
                                colors.primary,
                                gradientEdge
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // AnimatedContent swaps the icon when Aura mode toggled (scale+fade)
                AnimatedContent(
                    targetState = auraActive,
                    transitionSpec = {
                        (scaleIn(tween(200)) + fadeIn(tween(200))) togetherWith
                                (scaleOut(tween(150)) + fadeOut(tween(150)))
                    },
                    label = "mic_icon_swap"
                ) { isAura ->
                    Icon(
                        imageVector = if (isAura) auraIcon else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }
    }
}
