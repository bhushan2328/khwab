package com.toblad.khwab.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

    // ── Static values: computed once, never need re-updating mid-session ─────
    val displayName = remember { UserProfileStore.getDisplayName(context) }
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

    // ── Time string: recomputed every second from currentTime ─────────────────
    val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(currentTime)

    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // ── Left: greeting + name — animated slide-in on first composition ────
        Column {
            AnimatedContent(
                targetState = greeting,
                transitionSpec = {
                    (fadeIn() + slideInVertically { -it / 2 }) togetherWith fadeOut()
                },
                label = "greeting_anim"
            ) { greetingText ->
                Text(
                    text = greetingText,
                    color = colors.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = displayName,
                color = colors.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Right: date (static) + live clock (monospace, primary tint) ───────
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = date,
                color = colors.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = time,
                color = colors.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
