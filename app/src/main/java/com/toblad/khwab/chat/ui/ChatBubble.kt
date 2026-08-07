package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.toblad.khwab.chat.model.MessageStatus
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(message: ChatMessage) {
    val colors = MaterialTheme.colorScheme
    val isUser = message.sender == Sender.USER

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    val userBubbleColor: Color
    val assistantBubbleColor: Color
    val userTextColor: Color
    val assistantTextColor: Color

    if (auraActive) {
        val weatherTint = weatherBubbleTint(auraTheme.weatherState)
        val timeTint = timeBubbleTint(auraTheme.timePhase)
        userBubbleColor = lerp(colors.primary, weatherTint, 0.25f).copy(alpha = 0.82f)
        assistantBubbleColor = lerp(colors.surfaceVariant, timeTint, 0.20f).copy(alpha = 0.75f)
        userTextColor = colors.onPrimary
        assistantTextColor = colors.onSurfaceVariant
    } else {
        userBubbleColor = colors.primary
        assistantBubbleColor = colors.surfaceVariant
        userTextColor = colors.onPrimary
        assistantTextColor = colors.onSurfaceVariant
    }

    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(message.timestamp))

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
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Column {
                    Text(
                        text = buildString {
                            append(message.text)
                            if (message.state == MessageState.STREAMING) append("▌")
                        },
                        color = if (isUser) userTextColor else assistantTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeString,
                            color = (if (isUser) userTextColor else assistantTextColor)
                                .copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )

                        // Status tick only shown on user messages
                        if (isUser) {
                            val (icon, tint) = when (message.status) {
                                MessageStatus.SENDING -> Icons.Default.Schedule to
                                        userTextColor.copy(alpha = 0.55f)
                                MessageStatus.SENT    -> Icons.Default.Check to
                                        userTextColor.copy(alpha = 0.80f)
                                MessageStatus.ERROR   -> Icons.Default.ErrorOutline to
                                        colors.error
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = message.status.name,
                                tint = tint,
                                modifier = Modifier
                                    .height(12.dp)
                                    .padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun weatherBubbleTint(weather: WeatherState): Color = when (weather) {
    WeatherState.CLEAR  -> Color(0xFF6FCBFF)
    WeatherState.CLOUDY -> Color(0xFF9AA4B2)
    WeatherState.RAIN   -> Color(0xFF4A8FCC)
    WeatherState.SNOW   -> Color(0xFFB8D8F0)
    WeatherState.FOG    -> Color(0xFFA9B0B4)
    WeatherState.STORM  -> Color(0xFF3A4A5E)
}

private fun timeBubbleTint(phase: TimePhase): Color = when (phase) {
    TimePhase.PRE_DAWN  -> Color(0xFF10122B)
    TimePhase.SUNRISE   -> Color(0xFFFF9A6C)
    TimePhase.MORNING   -> Color(0xFF5FC7FF)
    TimePhase.NOON      -> Color(0xFF3AD1FF)
    TimePhase.AFTERNOON -> Color(0xFFFFC978)
    TimePhase.SUNSET    -> Color(0xFFFF7A5C)
    TimePhase.EVENING   -> Color(0xFF8E7CE0)
    TimePhase.NIGHT     -> Color(0xFF6E7BD6)
    TimePhase.MIDNIGHT  -> Color(0xFF4F5AB8)
}
