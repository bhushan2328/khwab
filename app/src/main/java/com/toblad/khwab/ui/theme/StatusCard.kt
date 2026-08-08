package com.toblad.khwab.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
    val assistantState = AssistantStateManager.state

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

    val cardContainer = if (isDaytime)
        Color.White.copy(alpha = 0.78f)   // frosted glass over bright sky
    else
        lerp(colors.surfaceVariant, animatedBgTint, 0.5f)

    val labelColor   = if (isDaytime) animatedBorderColor else animatedBorderColor
    val messageColor = if (isDaytime) Color(0xFF1f2328) else colors.onSurfaceVariant

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
