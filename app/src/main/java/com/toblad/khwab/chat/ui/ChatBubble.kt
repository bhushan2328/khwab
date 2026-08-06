package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

@Composable
fun ChatBubble(
    message: ChatMessage
) {
    val colors = MaterialTheme.colorScheme
    val isUser = message.sender == Sender.USER

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    // Derive chat bubble colors from Aura's live weather + time when active.
    // User bubble: shifted toward the ambient accent (primary) tinted by weather.
    // Assistant bubble: shifted toward the sky surface variant tinted by time.
    val userBubbleColor: Color
    val assistantBubbleColor: Color
    val userTextColor: Color
    val assistantTextColor: Color

    if (auraActive) {
        val weatherTint = weatherBubbleTint(auraTheme.weatherState)
        val timeTint = timeBubbleTint(auraTheme.timePhase)

        // User bubble = primary accent blended with weather tint, semi-transparent so Aura scene shows
        userBubbleColor = lerp(colors.primary, weatherTint, 0.25f).copy(alpha = 0.82f)
        // Assistant bubble = surfaceVariant blended with time tint, semi-transparent
        assistantBubbleColor = lerp(colors.surfaceVariant, timeTint, 0.20f).copy(alpha = 0.75f)
        userTextColor = colors.onPrimary
        assistantTextColor = colors.onSurfaceVariant
    } else {
        userBubbleColor = colors.primary
        assistantBubbleColor = colors.surfaceVariant
        userTextColor = colors.onPrimary
        assistantTextColor = colors.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.80f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) userBubbleColor else assistantBubbleColor
            )
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = buildString {
                        append(message.text)
                        if (message.state == MessageState.STREAMING) append("▌")
                    },
                    color = if (isUser) userTextColor else assistantTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * A tint color derived from the current weather, used to subtly
 * shift chat bubble backgrounds when Aura is active.
 */
private fun weatherBubbleTint(weather: WeatherState): Color = when (weather) {
    WeatherState.CLEAR  -> Color(0xFF6FCBFF)   // clear blue
    WeatherState.CLOUDY -> Color(0xFF9AA4B2)   // muted grey-blue
    WeatherState.RAIN   -> Color(0xFF4A8FCC)   // rain blue
    WeatherState.SNOW   -> Color(0xFFB8D8F0)   // icy pale blue
    WeatherState.FOG    -> Color(0xFFA9B0B4)   // foggy grey
    WeatherState.STORM  -> Color(0xFF3A4A5E)   // storm deep blue-grey
}

/**
 * A tint color derived from the time of day, used to subtly
 * shift assistant bubble backgrounds when Aura is active.
 */
private fun timeBubbleTint(phase: TimePhase): Color = when (phase) {
    TimePhase.PRE_DAWN  -> Color(0xFF10122B)   // deep indigo
    TimePhase.SUNRISE   -> Color(0xFFFF9A6C)   // warm orange
    TimePhase.MORNING   -> Color(0xFF5FC7FF)   // bright sky blue
    TimePhase.NOON      -> Color(0xFF3AD1FF)   // midday cyan
    TimePhase.AFTERNOON -> Color(0xFFFFC978)   // golden afternoon
    TimePhase.SUNSET    -> Color(0xFFFF7A5C)   // sunset red-orange
    TimePhase.EVENING   -> Color(0xFF8E7CE0)   // dusky purple
    TimePhase.NIGHT     -> Color(0xFF6E7BD6)   // night blue-indigo
    TimePhase.MIDNIGHT  -> Color(0xFF4F5AB8)   // deep midnight blue
}
