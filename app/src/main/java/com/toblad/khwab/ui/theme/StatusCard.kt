package com.toblad.khwab.ui.theme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

@Composable
fun StatusCard(
    status: String,
    statusColor: Color,
    message: String
) {
    val colors = MaterialTheme.colorScheme
    val assistantState by AssistantStateManager.stateFlow.collectAsState()

    val dotShouldPulse = assistantState !in listOf(
        AssistantState.STOPPED, AssistantState.ERROR, AssistantState.READY
    )

    // ── Animated border + background tint color ───────────────────────────────
    val animatedBorderColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(durationMillis = 400),
        label = "border_color"
    )
    val animatedBgTint by animateColorAsState(
        targetValue = statusColor.copy(alpha = 0.06f),
        animationSpec = tween(durationMillis = 400),
        label = "bg_tint"
    )

    val transition = rememberInfiniteTransition(label = "status_dot")
    val dotScale by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (dotShouldPulse) 1.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (dotShouldPulse) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    // When Aura is active over a bright daytime sky the card must stay readable.
    // Use a semi-transparent frosted surface so the sky shows through subtly,
    // and keep text/border contrast with a white-tinted base.
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val timePhase  = ThemeController.currentAuraTheme.timePhase
    val isDaytime  = auraActive && timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )

    val cardContainer = when {
        isDaytime  -> Color.White.copy(alpha = 0.78f)          // frosted glass over bright sky
        auraActive -> lerp(colors.surfaceVariant, animatedBgTint, 0.5f)
        else       -> lerp(                                    // non-Aura: stronger surface so text
            colors.surface,                                    // stays readable over any gradient bg
            animatedBorderColor.copy(alpha = 0.12f),
            0.5f
        )
    }

    val labelColor   = animatedBorderColor
    // fix #11: softer message text in Aura for visual hierarchy between label and message
    val messageColor = when {
        isDaytime  -> Color(0xFF1f2328)
        auraActive -> colors.onSurface.copy(alpha = 0.75f)
        else       -> colors.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // ── Left accent border — animated to match status color ───────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 0.dp, bottomEnd = 0.dp
                        )
                    )
                    .background(animatedBorderColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated status dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(dotScale)
                        .background(
                            color = statusColor.copy(alpha = dotAlpha),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = status.replace("_", " ").uppercase(),
                    color = labelColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    color = messageColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Compact status pill displayed beneath the microphone button.
 * Shows a pulsing dot + state label + contextual message in a
 * small translucent chip so the Unity environment remains visible.
 */
@Composable
fun StatusPill(
    assistantState: AssistantState,
    statusColor: Color,
    message: String,
    isDaytime: Boolean,
    auraActive: Boolean
) {
    val colors = MaterialTheme.colorScheme

    val dotShouldPulse = assistantState !in listOf(
        AssistantState.STOPPED, AssistantState.ERROR, AssistantState.READY
    )

    val animatedDotColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(durationMillis = 350),
        label = "pill_dot_color"
    )

    val transition = rememberInfiniteTransition(label = "pill_dot_pulse")
    val dotScale by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (dotShouldPulse) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_dot_scale"
    )
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (dotShouldPulse) 0.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_dot_alpha"
    )

    val pillBg = when {
        isDaytime  -> Color.White.copy(alpha = 0.70f)
        auraActive -> Color(0xFF0A0C18).copy(alpha = 0.78f)
        else       -> Color(0xFF080A14).copy(alpha = 0.84f)
    }
    val pillBorder = when {
        isDaytime  -> Color.White.copy(alpha = 0.50f)
        else       -> animatedDotColor.copy(alpha = 0.35f)
    }
    val labelColor = when {
        isDaytime  -> animatedDotColor.copy(alpha = 0.85f).run {
            // Darken for daytime readability
            Color(red * 0.7f, green * 0.7f, blue * 0.7f, 1f)
        }
        else       -> animatedDotColor
    }
    val msgColor = when {
        isDaytime  -> Color(0xFF1f2328).copy(alpha = 0.75f)
        auraActive -> colors.onSurface.copy(alpha = 0.70f)
        else       -> colors.onSurface.copy(alpha = 0.80f)
    }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(pillBg)
            .border(
                width = 1.dp,
                color = pillBorder,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics { contentDescription = "Assistant status: ${assistantState.name}" }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // State label row: dot + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dotScale)
                        .background(
                            color = animatedDotColor.copy(alpha = dotAlpha),
                            shape = CircleShape
                        )
                )
                AnimatedContent(
                    targetState = assistantState.name,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(150)) + slideOutVertically { -it / 3 })
                    },
                    label = "pill_state_label"
                ) { stateName ->
                    Text(
                        text = stateName.replace("_", " ").uppercase(),
                        color = labelColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                }
            }

            // Contextual message
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically { -it / 2 })
                },
                label = "pill_message"
            ) { msg ->
                Text(
                    text = msg,
                    color = msgColor,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
