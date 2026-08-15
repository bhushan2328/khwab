package com.toblad.khwab.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // When true renders an outlined style — use for secondary actions (Chat, Settings)
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // In daytime Aura the outlined buttons render over the translucent bottom panel.
    // When Aura is active at night/storm/etc, boost contrast against the dark panel.
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val isDaytimeAura = auraActive &&
            ThemeController.currentAuraTheme.timePhase in listOf(
                com.toblad.khwab.aura.model.TimePhase.SUNRISE,
                com.toblad.khwab.aura.model.TimePhase.MORNING,
                com.toblad.khwab.aura.model.TimePhase.NOON,
                com.toblad.khwab.aura.model.TimePhase.AFTERNOON
            )
    // Daytime: use a dark variant for readability on the frosted light panel
    // Night Aura: use the normal color — it reads fine on the dark panel
    // Default: unchanged
    val outlinedColor = when {
        outlined && isDaytimeAura -> Color(
            red   = (backgroundColor.red   * 0.72f).coerceAtMost(1f),
            green = (backgroundColor.green * 0.72f).coerceAtMost(1f),
            blue  = (backgroundColor.blue  * 0.72f).coerceAtMost(1f),
            alpha = 1f
        )
        else -> backgroundColor
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "button_scale"
    )

    val shape = RoundedCornerShape(18.dp)

    val wrappedClick: () -> Unit = {
        val feedbackType = if (outlined) HapticFeedbackType.TextHandleMove
                           else HapticFeedbackType.LongPress
        haptic.performHapticFeedback(feedbackType)
        onClick()
    }

    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
        }
    }

    if (outlined) {
        OutlinedButton(
            onClick = wrappedClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(60.dp).scale(scale),
            shape = shape,
            border = BorderStroke(1.5.dp, if (enabled) outlinedColor else colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = outlinedColor,
                disabledContentColor = colors.onSurfaceVariant
            )
        ) { content() }
    } else {
        Button(
            onClick = wrappedClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(60.dp).scale(scale),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceVariant,
                disabledContentColor = colors.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation  = 6.dp,
                pressedElevation  = 2.dp,
                disabledElevation = 0.dp
            )
        ) { content() }
    }
}
