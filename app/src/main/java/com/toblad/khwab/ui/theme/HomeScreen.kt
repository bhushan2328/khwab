package com.toblad.khwab.ui.theme

import com.toblad.khwab.BuildConfig
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager

// State-specific status messages — contextually meaningful for each assistant state
private fun statusMessage(state: AssistantState) = when (state) {
    AssistantState.STOPPED   -> "Tap the mic or press Start to wake Khwab."
    AssistantState.ERROR     -> "Something went wrong. Tap Start to try again."
    AssistantState.READY     -> "Say \"Hey Khwab\" or tap the microphone to begin."
    AssistantState.RUNNING   -> "Listening for wake word…"
    AssistantState.LISTENING -> "I'm listening — go ahead."
    AssistantState.THINKING  -> "Processing your request…"
    AssistantState.EXECUTING -> "Running your command…"
    AssistantState.SPEAKING  -> "Speaking…"
}

@Composable
fun HomeScreen(
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAuraDebugClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val assistantState by AssistantStateManager.stateFlow.collectAsState()

    val auraTheme = ThemeController.currentAuraTheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val icons = AuraIconProvider.homeIcons(
        auraActive = auraActive,
        weather    = auraTheme.weatherState,
        timePhase  = auraTheme.timePhase
    )

    val statusColor = when (assistantState) {
        AssistantState.STOPPED   -> colors.error
        AssistantState.ERROR     -> colors.error
        AssistantState.READY     -> KhwabBlue
        AssistantState.RUNNING   -> KhwabListening
        AssistantState.LISTENING -> KhwabListening
        AssistantState.THINKING  -> KhwabProcessing
        AssistantState.EXECUTING -> KhwabExecuting
        AssistantState.SPEAKING  -> KhwabSpeaking
    }

    // Fade-in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "home_fade_in"
    )

    // When Aura is off, show a time-aware gradient fallback so the home screen
    // doesn't stay pure dark-navy during daytime. Background only — no particles.
    // When Aura is active, the background is fully transparent so Unity renders through.
    val fallbackBg: Modifier = when {
        auraActive -> Modifier  // transparent — Unity surface shows through
        else -> {
            val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
            val (topC, botC) = when {
                hour in 5..7   -> Color(0xFF0D1433) to Color(0xFF3D2B1F)
                hour in 8..11  -> Color(0xFF0A1628) to Color(0xFF1A3A5C)
                hour in 12..14 -> Color(0xFF0C1A2E) to Color(0xFF163354)
                hour in 15..17 -> Color(0xFF0E1830) to Color(0xFF2A1B3D)
                hour in 18..20 -> Color(0xFF1A0E2E) to Color(0xFF3D1A12)
                hour in 21..22 -> Color(0xFF090D1A) to Color(0xFF131B2E)
                else           -> Color(0xFF040608) to Color(0xFF080D14)
            }
            Modifier.background(Brush.verticalGradient(listOf(topC, botC)))
        }
    }

    // Daytime Aura: light surfaces are visible on bright sky — use dark text/borders
    val isDaytime = auraActive && auraTheme.timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )

    // Bottom panel: translucent surface floating above the Unity world
    val bottomSurfaceColor = when {
        isDaytime  -> Color.White.copy(alpha = 0.18f)
        auraActive -> Color(0xFF0A0C18).copy(alpha = 0.72f)
        else       -> Color(0xFF080A14).copy(alpha = 0.82f)
    }
    val bottomBorderColor = when {
        isDaytime  -> Color.White.copy(alpha = 0.30f)
        auraActive -> colors.outline.copy(alpha = 0.30f)
        else       -> colors.outline.copy(alpha = 0.40f)
    }

    val isStopped = assistantState == AssistantState.STOPPED
                 || assistantState == AssistantState.ERROR

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(fallbackBg)
    ) {
        // ── Aura note: Unity surface is visible through the transparent background above.
        // AuraScene is intentionally not rendered here to avoid covering Unity.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP: App identity + Header ────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            // Compact app identity label
            Text(
                text = "KHWAB",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDaytime) Color.White.copy(alpha = 0.70f)
                        else colors.primary.copy(alpha = 0.60f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Header section (avatar, greeting, clock)
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                HeaderSection()
            }

            // ── MIDDLE: Aura world + Mic — let environment breathe ────────────
            // Weight(1f) ensures mic area expands to fill available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Subtle vertical gradient scrim at top of mic area to anchor text over Unity
                if (auraActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    1f to if (isDaytime) Color.Black.copy(alpha = 0.10f)
                                          else Color.Black.copy(alpha = 0.30f)
                                )
                            )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Primary mic control — existing MicButton with all animations preserved
                    MicButton(onClick = onStartClick)

                    Spacer(modifier = Modifier.height(28.dp))

                    // Compact status pill — replaces the large StatusCard
                    StatusPill(
                        assistantState = assistantState,
                        statusColor    = statusColor,
                        message        = statusMessage(assistantState),
                        isDaytime      = isDaytime,
                        auraActive     = auraActive
                    )
                }
            }

            // ── BOTTOM: Translucent action panel ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = bottomSurfaceColor,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .then(
                        Modifier.padding(
                            start = 24.dp, end = 24.dp,
                            top = 20.dp, bottom = 0.dp
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top handle indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.12f),
                            thickness = 3.dp,
                            color = bottomBorderColor
                        )
                    }

                    // ── Adaptive Start / Stop — cross-fades via AnimatedContent ──
                    AnimatedContent(
                        targetState = isStopped,
                        transitionSpec = {
                            (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.93f)) togetherWith
                            (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.93f))
                        },
                        label = "start_stop_btn"
                    ) { stopped ->
                        if (stopped) {
                            ActionButton(
                                text            = "Start Assistant",
                                icon            = icons.start,
                                backgroundColor = colors.secondary,
                                modifier        = Modifier.fillMaxWidth(),
                                onClick         = onStartClick
                            )
                        } else {
                            ActionButton(
                                text            = "Stop Assistant",
                                icon            = icons.stop,
                                backgroundColor = colors.error,
                                modifier        = Modifier.fillMaxWidth(),
                                onClick         = onStopClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Secondary actions: Chat + Settings ────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            text            = "Chat",
                            icon            = icons.chat,
                            backgroundColor = colors.primary,
                            outlined        = true,
                            modifier        = Modifier.weight(1f),
                            onClick         = onChatClick
                        )
                        ActionButton(
                            text            = "Settings",
                            icon            = icons.settings,
                            backgroundColor = colors.tertiary,
                            outlined        = true,
                            modifier        = Modifier.weight(1f),
                            onClick         = onSettingsClick
                        )
                    }

                    // ── Aura Debug Console — debug builds only ────────────────
                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onAuraDebugClick,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, bottomBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🐛 Aura Debug",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDaytime) Color(0xFF1f2328).copy(alpha = 0.6f)
                                        else colors.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
