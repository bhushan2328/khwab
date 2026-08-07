package com.toblad.khwab.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            // ── User bubble ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(0.78f),
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = 20.dp, bottomEnd = 6.dp
                ),
                colors = CardDefaults.cardColors(containerColor = userBubbleColor)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column {
                        Text(
                            text = buildString {
                                append(message.text)
                                if (message.state == MessageState.STREAMING) append("▌")
                            },
                            color = userTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeString,
                                color = userTextColor.copy(alpha = 0.55f),
                                fontSize = 10.sp
                            )
                            val (icon, tint) = when (message.status) {
                                MessageStatus.SENDING -> Icons.Default.Schedule to
                                        userTextColor.copy(alpha = 0.55f)
                                MessageStatus.SENT -> Icons.Default.Check to
                                        userTextColor.copy(alpha = 0.80f)
                                MessageStatus.ERROR -> Icons.Default.ErrorOutline to colors.error
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = message.status.name,
                                tint = tint,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // ── Khwab (assistant) bubble — rich GPT-style rendering ───────────
            Column(modifier = Modifier.fillMaxWidth(0.95f)) {
                // Khwab label row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (auraActive)
                                    lerp(colors.primaryContainer,
                                        timeBubbleTint(auraTheme.timePhase), 0.3f)
                                else colors.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "Khwab",
                        color = colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 6.dp, topEnd = 20.dp,
                        bottomStart = 20.dp, bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = assistantBubbleColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (message.state == MessageState.STREAMING) {
                            // Streaming: plain text with cursor
                            Text(
                                text = buildAnnotatedString {
                                    append(message.text)
                                    withStyle(SpanStyle(color = colors.primary)) { append("▌") }
                                },
                                color = assistantTextColor,
                                fontSize = 15.sp,
                                lineHeight = 23.sp
                            )
                        } else {
                            // Render markdown-like content
                            RichAssistantText(
                                text = message.text,
                                textColor = assistantTextColor,
                                accentColor = colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = timeString,
                            color = assistantTextColor.copy(alpha = 0.45f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders assistant text with simple markdown-like formatting:
 * - Lines starting with "- " or "• " → bullet points
 * - Lines starting with "1. " / "2. " etc → numbered list entries
 * - **bold** → bold spans
 * - `code` → monospace spans
 * - Lines starting with "# " → section headers
 * - Blank lines → spacing
 */
@Composable
private fun RichAssistantText(
    text: String,
    textColor: Color,
    accentColor: Color
) {
    val lines = remember(text) { text.lines() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { line ->
            when {
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# "), textColor, accentColor),
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## "), textColor, accentColor),
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("• ") -> {
                    val content = if (line.startsWith("- ")) line.removePrefix("- ")
                                  else line.removePrefix("• ")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "•",
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content, textColor, accentColor),
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Numbered list: "1. ", "2. ", ... "99. "
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val dotIdx = line.indexOf('.')
                    val num = line.substring(0, dotIdx)
                    val content = line.substring(dotIdx + 2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "$num.",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content, textColor, accentColor),
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line, textColor, accentColor),
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }
            }
        }
    }
}

/**
 * Parses a single line for inline markdown: **bold**, *italic*, `code`.
 */
private fun parseInlineMarkdown(
    text: String,
    defaultColor: Color,
    accentColor: Color
) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // **bold**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i]); i++
                }
            }
            // *italic*
            text.startsWith("*", i) && !text.startsWith("**", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            // `code`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            fontSize = 13.sp,
                            background = accentColor.copy(alpha = 0.10f)
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> { append(text[i]); i++ }
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
