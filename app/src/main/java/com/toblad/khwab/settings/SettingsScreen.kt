package com.toblad.khwab.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.BuildConfig
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

/**
 * Lets the user control Aura's behavior: whether it's on,
 * whether it follows real time/weather, whether animations
 * and ambient sound play, and how often weather refreshes.
 *
 * All changes go through AuraBridge, which both applies them
 * immediately and persists them for next launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    var config by remember { mutableStateOf(AuraBridge.getConfig()) }

    fun applyChange(newConfig: AuraConfig) {
        config = newConfig
        AuraBridge.updateConfig(newConfig)
    }

    val colors = MaterialTheme.colorScheme
    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    val isDaytimeAura = auraActive && auraTheme.timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )

    // ── Top bar colors ────────────────────────────────────────────────────────
    val barContainerColor = when {
        isDaytimeAura -> Color.White.copy(alpha = 0.72f)
        auraActive    -> colors.surface.copy(alpha = 0.78f)
        else          -> colors.surface
    }
    val barContentColor = if (isDaytimeAura) Color(0xFF1A1A2E) else colors.onSurface
    val barSubtitleColor = if (isDaytimeAura) Color(0xFF3D3D5C).copy(alpha = 0.72f)
                           else colors.onSurfaceVariant

    // ── Section card surface ──────────────────────────────────────────────────
    val cardSurface = when {
        isDaytimeAura -> Color.White.copy(alpha = 0.62f)
        auraActive    -> colors.surface.copy(alpha = 0.72f)
        else          -> colors.surface
    }
    val cardBorder = when {
        isDaytimeAura -> Color.White.copy(alpha = 0.55f)
        auraActive    -> colors.outline.copy(alpha = 0.22f)
        else          -> colors.outline.copy(alpha = 0.18f)
    }

    // ── Text colors on cards ──────────────────────────────────────────────────
    val titleOnCard  = if (isDaytimeAura) Color(0xFF1A1A2E) else colors.onSurface
    val bodyOnCard   = if (isDaytimeAura) Color(0xFF3D3D5C).copy(alpha = 0.80f) else colors.onSurfaceVariant

    Scaffold(
        containerColor = if (auraActive) Color.Transparent else colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barContainerColor,
                    titleContentColor = barContentColor,
                    navigationIconContentColor = barContentColor,
                    actionIconContentColor = barContentColor
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = barContentColor,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Aura & environment",
                            style = MaterialTheme.typography.labelSmall,
                            color = barSubtitleColor,
                            lineHeight = 14.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            )
        }
    ) { padding ->

        // Non-Aura: subtle vertical gradient for visual depth
        if (!auraActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.primaryContainer.copy(alpha = 0.10f),
                                colors.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Section: Aura ─────────────────────────────────────────────────
            SettingsSection(
                title = "Aura",
                description = "Unity ambient environment",
                cardSurface = cardSurface,
                cardBorder  = cardBorder
            ) {
                // Primary Aura switch — special visual treatment
                AuraMasterSwitch(
                    config      = config,
                    isDaytime   = isDaytimeAura,
                    auraActive  = auraActive,
                    titleColor  = titleOnCard,
                    bodyColor   = bodyOnCard,
                    onCheckedChange = { checked ->
                        if (checked) AuraBridge.activate() else AuraBridge.deactivate()
                        config = AuraBridge.getConfig()
                    }
                )

                SettingsDivider(cardBorder)

                SettingSwitchRow(
                    icon       = Icons.Default.PlayCircle,
                    title      = "Animations",
                    subtitle   = "Rain, snow, fireflies, leaves and petals",
                    checked    = config.animationsEnabled,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard,
                    onCheckedChange = { applyChange(config.copy(animationsEnabled = it)) }
                )
            }

            // ── Section: Data & Location ──────────────────────────────────────
            SettingsSection(
                title = "Environment",
                description = "Time, weather and location data",
                cardSurface = cardSurface,
                cardBorder  = cardBorder
            ) {
                SettingSwitchRow(
                    icon       = Icons.Default.AccessTime,
                    title      = "Follow Real Time",
                    subtitle   = "Sky, sun and moon reflect the actual time of day",
                    checked    = config.autoTime,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard,
                    onCheckedChange = { applyChange(config.copy(autoTime = it)) }
                )

                SettingsDivider(cardBorder)

                SettingSwitchRow(
                    icon       = Icons.Default.Cloud,
                    title      = "Follow Real Weather",
                    subtitle   = "Theme reflects live weather at your location",
                    checked    = config.autoWeather,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard,
                    onCheckedChange = { applyChange(config.copy(autoWeather = it)) }
                )

                SettingsDivider(cardBorder)

                RefreshIntervalRow(
                    minutes    = config.refreshIntervalMinutes,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard,
                    onMinutesChange = { applyChange(config.copy(refreshIntervalMinutes = it)) }
                )
            }

            // ── Section: Audio ────────────────────────────────────────────────
            SettingsSection(
                title = "Audio",
                description = "Ambient sound for the Aura world",
                cardSurface = cardSurface,
                cardBorder  = cardBorder
            ) {
                SettingSwitchRow(
                    icon       = Icons.Default.MusicNote,
                    title      = "Ambient Sound",
                    subtitle   = "Rain, wind, crickets and thunder audio",
                    checked    = config.ambientSoundEnabled,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard,
                    onCheckedChange = { applyChange(config.copy(ambientSoundEnabled = it)) }
                )
            }

            // ── Section: About ────────────────────────────────────────────────
            SettingsSection(
                title = "About",
                description = null,
                cardSurface = cardSurface,
                cardBorder  = cardBorder
            ) {
                AboutRow(
                    label = "Version",
                    value = BuildConfig.VERSION_NAME,
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard
                )
                SettingsDivider(cardBorder)
                AboutRow(
                    label = "Build",
                    value = if (BuildConfig.DEBUG) "Debug" else "Release",
                    titleColor = titleOnCard,
                    bodyColor  = bodyOnCard
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Section container ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    description: String?,
    cardSurface: Color,
    cardBorder: Color,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val isDaytimeAura = auraActive && ThemeController.currentAuraTheme.timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )
    val headerColor = when {
        isDaytimeAura -> Color(0xFF1A1A2E).copy(alpha = 0.65f)
        else          -> colors.primary
    }
    val descColor = when {
        isDaytimeAura -> Color(0xFF3D3D5C).copy(alpha = 0.60f)
        else          -> colors.onSurfaceVariant.copy(alpha = 0.70f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section label row
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = headerColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            if (description != null) {
                Text(
                    text = "·  $description",
                    style = MaterialTheme.typography.labelSmall,
                    color = descColor,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Section card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = cardSurface,
            border = BorderStroke(1.dp, cardBorder),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// ── Thin in-card divider ──────────────────────────────────────────────────────

@Composable
private fun SettingsDivider(borderColor: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = borderColor
    )
}

// ── Aura master switch — special hero treatment ───────────────────────────────

@Composable
private fun AuraMasterSwitch(
    config: AuraConfig,
    isDaytime: Boolean,
    auraActive: Boolean,
    titleColor: Color,
    bodyColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val checked = config.enabled

    // Read from the Compose-observable ThemeController so the dot/label updates
    // reactively as the state transitions OFF→STARTING→ACTIVE and ACTIVE→STOPPING→OFF.
    val auraState = ThemeController.currentAuraTheme.auraState
    val stateLabel = when (auraState) {
        AuraState.ACTIVE   -> "Active"
        AuraState.STARTING -> "Starting…"
        AuraState.STOPPING -> "Stopping…"
        AuraState.ERROR    -> "Error"
        AuraState.OFF      -> "Off"
    }
    val stateDotColor by animateColorAsState(
        targetValue = when (auraState) {
            AuraState.ACTIVE   -> colors.primary
            AuraState.STARTING -> colors.tertiary
            AuraState.STOPPING -> colors.tertiary.copy(alpha = 0.60f)
            AuraState.ERROR    -> colors.error
            AuraState.OFF      -> colors.onSurfaceVariant.copy(alpha = 0.40f)
        },
        animationSpec = tween(400),
        label = "aura_state_dot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "Aura enabled: $checked" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (checked) colors.primary.copy(alpha = if (isDaytime) 0.18f else 0.22f)
                        else colors.surfaceVariant.copy(alpha = 0.50f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (checked) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.60f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Aura Environment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live state dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(stateDotColor)
                    )
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor    = colors.primary,
                uncheckedTrackColor  = colors.outline.copy(alpha = 0.50f),
                uncheckedBorderColor = colors.outline.copy(alpha = 0.50f),
                uncheckedThumbColor  = colors.onSurfaceVariant
            )
        )
    }
}

// ── Standard switch row ───────────────────────────────────────────────────────

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    titleColor: Color,
    bodyColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "$title: ${if (checked) "on" else "off"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (checked) colors.primary.copy(alpha = 0.16f)
                        else colors.surfaceVariant.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) colors.primary
                           else colors.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor    = colors.primary,
                uncheckedTrackColor  = colors.outline.copy(alpha = 0.50f),
                uncheckedBorderColor = colors.outline.copy(alpha = 0.50f),
                uncheckedThumbColor  = colors.onSurfaceVariant
            )
        )
    }
}

// ── Refresh interval slider ───────────────────────────────────────────────────

@Composable
private fun RefreshIntervalRow(
    minutes: Int,
    titleColor: Color,
    bodyColor: Color,
    onMinutesChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Weather Refresh",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )
                    Text(
                        text = "How often weather is re-checked",
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor,
                        lineHeight = 16.sp
                    )
                }
            }

            Text(
                text = "$minutes min",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val steps = ((60 - 5) / 5) - 1  // 10 steps between 5 and 60
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onMinutesChange(it.toInt()) },
            valueRange = 5f..60f,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor          = colors.primary,
                activeTrackColor    = colors.primary,
                inactiveTrackColor  = colors.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Weather refresh interval: $minutes minutes" }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "5 min",
                style = MaterialTheme.typography.labelSmall,
                color = bodyColor.copy(alpha = 0.70f)
            )
            Text(
                text = "60 min",
                style = MaterialTheme.typography.labelSmall,
                color = bodyColor.copy(alpha = 0.70f)
            )
        }
    }
}

// ── About row ─────────────────────────────────────────────────────────────────

@Composable
private fun AboutRow(
    label: String,
    value: String,
    titleColor: Color,
    bodyColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = titleColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = bodyColor
        )
    }
}
