package com.toblad.khwab.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.di.UserProfileStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HeaderSection() {
    val context = LocalContext.current

    // ── Clock: updates every second ──────────────────────────────────────────
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    // ── Static values ─────────────────────────────────────────────────────────
    val rawName = remember { UserProfileStore.getDisplayName(context) }
    val displayName = remember { rawName.ifBlank { "Friend" } }
    val avatarInitial = remember { displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "K" }

    val initialCalendar = remember { Calendar.getInstance() }
    val greeting = remember {
        when (initialCalendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Good Night"
        }
    }
    val date = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(initialCalendar.time)
    }

    val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(currentTime)

    val colors = MaterialTheme.colorScheme

    // ── Aura daytime awareness ────────────────────────────────────────────────
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val timePhase  = ThemeController.currentAuraTheme.timePhase

    val isDaytime  = auraActive && timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )

    // When rendering over a bright daytime sky, swap to white text so the
    // header stays readable regardless of sky brightness.
    val greetingColor = if (isDaytime) Color.White else colors.onBackground
    val nameColor     = if (isDaytime) Color.White.copy(alpha = 0.88f) else colors.onSurfaceVariant
    val dateColor     = if (isDaytime) Color.White.copy(alpha = 0.78f) else colors.onSurfaceVariant
    val clockColor    = if (isDaytime) Color.White else colors.primary

    // Avatar badge: frosted semi-transparent in daytime, solid otherwise
    val avatarBg = if (isDaytime)
        Color.White.copy(alpha = 0.28f) else colors.primaryContainer
    val avatarTextColor = if (isDaytime) Color.White else colors.onPrimaryContainer

    // Subtle dark scrim behind text so white text always has contrast,
    // even on very bright skies (the scrim is invisible in dark mode)
    val textScrim = if (isDaytime) Color.Black.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // ── Left: avatar badge + greeting + name ─────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular avatar badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = avatarBg, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    color = avatarTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                AnimatedContent(
                    targetState = greeting,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { -it / 2 }) togetherWith fadeOut()
                    },
                    label = "greeting_anim"
                ) { greetingText ->
                    // Scrim pill behind greeting text for contrast over bright skies
                    Box(
                        modifier = Modifier
                            .background(textScrim, RoundedCornerShape(6.dp))
                            .padding(horizontal = if (isDaytime) 4.dp else 0.dp)
                    ) {
                        Text(
                            text = greetingText,
                            color = greetingColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = displayName,
                    color = nameColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Right: date + live clock ──────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = date,
                color = dateColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = time,
                color = clockColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
