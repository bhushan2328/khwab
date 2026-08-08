package com.toblad.khwab.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA

    // fix #21: scrim starts at slight alpha so text field always has a base
    val barBackground = if (auraActive) {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.surface.copy(alpha = 0.15f),
                    colors.surface.copy(alpha = 0.88f),
                    colors.surface.copy(alpha = 0.97f)
                )
            )
        )
    } else {
        Modifier.background(colors.surface)
    }

    val canSend = text.isNotBlank()

    // fix #26: distinct disabled state — outline ghost instead of surfaceVariant
    val sendBgColor by animateColorAsState(
        targetValue = if (canSend) colors.primary else colors.outline.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 220),
        label = "send_bg_color"
    )

    // Bounce scale in when text becomes available
    val sendBtnScale by animateFloatAsState(
        targetValue = if (canSend) 1.0f else 0.88f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "send_btn_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(barBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {

        // ── Text field ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message Khwab…", style = MaterialTheme.typography.bodyMedium) },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (canSend) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSendClick()
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.primary,
                unfocusedBorderColor = colors.outline.copy(alpha = 0.85f),  // fix #22: stronger border
                focusedTextColor     = colors.onSurface,
                unfocusedTextColor   = colors.onSurface,
                cursorColor          = colors.primary,
                focusedContainerColor   = colors.surface.copy(alpha = 0.7f),
                unfocusedContainerColor = colors.surface.copy(alpha = 0.5f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        // ── Animated circular Send button ─────────────────────────────────────
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(sendBtnScale)
                .clip(CircleShape)
                .background(sendBgColor),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    if (canSend) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSendClick()
                    }
                },
                enabled = canSend,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.60f), // fix #26
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
