package com.toblad.khwab.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

@Composable
fun TypingIndicator() {
    val colors = MaterialTheme.colorScheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA

    val containerColor = if (auraActive) colors.surfaceVariant.copy(alpha = 0.72f)
                         else colors.surfaceVariant

    // Dot colour matches the accent so it feels part of the assistant brand
    val dotColor = colors.primary.copy(alpha = 0.85f)

    val transition = rememberInfiniteTransition(label = "typing")

    // Three dots: staggered 160 ms each, scale + Y with FastOutSlowIn for "breathing" feel
    data class DotAnim(val yOffset: Float, val scale: Float)

    val dotAnims = (0..2).map { index ->
        val yOffset by transition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 480,
                    delayMillis = index * 160,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_y_$index"
        )
        val scale by transition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 480,
                    delayMillis = index * 160,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale_$index"
        )
        DotAnim(yOffset, scale)
    }

    // Slide the whole bubble up from below when it first appears
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + slideInVertically(
            animationSpec = tween(280),
            initialOffsetY = { it / 2 }
        )
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dotAnims.forEach { anim ->
                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer {
                                translationY = anim.yOffset
                                scaleX = anim.scale
                                scaleY = anim.scale
                            },
                        shape = CircleShape,
                        color = dotColor
                    ) {}
                }
            }
        }
    }
}
