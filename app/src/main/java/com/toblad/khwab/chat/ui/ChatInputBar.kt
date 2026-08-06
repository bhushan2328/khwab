package com.toblad.khwab.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toblad.khwab.ui.theme.AuraIconProvider
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

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    // Mic icon follows Aura weather/time when active
    val micIcon = AuraIconProvider.micIconFor(
        weather = auraTheme.weatherState,
        timePhase = auraTheme.timePhase
    )

    // Semi-transparent background behind the input bar so the Aura scene shows through
    val rowBg = if (auraActive) colors.surface.copy(alpha = 0.70f)
                else colors.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Type a message...")
            },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                cursorColor = colors.primary
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        IconButton(onClick = onMicClick) {
            Icon(
                imageVector = if (auraActive) micIcon else Icons.Default.Mic,
                contentDescription = "Voice",
                tint = colors.primary
            )
        }

        IconButton(onClick = onSendClick) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = colors.primary
            )
        }
    }
}
