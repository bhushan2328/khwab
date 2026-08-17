package com.toblad.khwab.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

/**
 * Animated typing/dreaming indicator shown while Khwab is processing.
 *
 * Shows a floating AutoAwesome icon with a gentle breathing + float animation,
 * paired with a "Dreaming…" label. Designed to be lightweight and calm.
 */
@Composable
fun TypingIndicator() {
    val colors = MaterialTheme.colorScheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA

    val isDaytimeAura = auraActive && ThemeController.currentAuraTheme.timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )
    val containerColor = when {
        isDaytimeAura -> Color.White.copy(alpha = 0.78f)
        auraActive    -> colors.surfaceVariant.copy(alpha = 0.80f)
        else          -> colors.surfaceVariant
    }
    val iconTint    = colors.primary
    val labelColor  = if (isDaytimeAura) Color(0xFF1A1A2E) else colors.onSurfaceVariant

    val transition = rememberInfiniteTransition(label = "dream_anim")

    // Gentle vertical float
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue  = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dream_float"
    )

    // Slow breathing scale
    val breathScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dream_scale"
    )

    // Slow star rotation
    val starRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dream_rotation"
    )

    // Ellipsis dot count
    val dotPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_phase"
    )
    val dots = ".".repeat((dotPhase.toInt() % 3) + 1)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 2 }
        )
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Floating dream icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { translationY = floatY }
                        .scale(breathScale)
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .graphicsLayer { rotationZ = starRotation }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = iconTint.copy(alpha = 0.16f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    // Main icon
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Khwab",
                        color = colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "Dreaming$dots",
                        color = labelColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
