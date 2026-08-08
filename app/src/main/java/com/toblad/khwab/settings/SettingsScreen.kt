package com.toblad.khwab.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.ui.graphics.Brush as UiBrush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.aura.model.AuraConfig

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface.copy(alpha = 0.95f), // fix #24: consistent semi-transparent
                    titleContentColor = colors.onSurface
                )
            )
        }
    ) { padding ->

        // fix #14: subtle top-to-background gradient for visual depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    UiBrush.verticalGradient(
                        colors = listOf(
                            colors.primaryContainer.copy(alpha = 0.12f),
                            colors.background
                        )
                    )
                )
        ) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Section: Appearance ────────────────────────────────────────────
            SectionHeader(title = "Appearance")

            SettingSwitchRow(
                icon = Icons.Default.Palette,
                title = "Aura Enabled",
                subtitle = "Master switch for the ambient theme",
                checked = config.enabled,
                onCheckedChange = { checked ->
                    if (checked) AuraBridge.activate() else AuraBridge.deactivate()
                    config = AuraBridge.getConfig()
                }
            )

            SettingSwitchRow(
                icon = Icons.Default.PlayCircle,
                title = "Animations",
                subtitle = "Rain, snow, fireflies, leaves and petals",
                checked = config.animationsEnabled,
                onCheckedChange = { applyChange(config.copy(animationsEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Section: Data & Location ───────────────────────────────────────
            SectionHeader(title = "Data & Location")

            SettingSwitchRow(
                icon = Icons.Default.AccessTime,
                title = "Follow Real Time",
                subtitle = "Sky, sun and moon reflect the actual time of day",
                checked = config.autoTime,
                onCheckedChange = { applyChange(config.copy(autoTime = it)) }
            )

            SettingSwitchRow(
                icon = Icons.Default.Cloud,
                title = "Follow Real Weather",
                subtitle = "Theme reflects live weather at your location",
                checked = config.autoWeather,
                onCheckedChange = { applyChange(config.copy(autoWeather = it)) }
            )

            RefreshIntervalRow(
                minutes = config.refreshIntervalMinutes,
                onMinutesChange = { applyChange(config.copy(refreshIntervalMinutes = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Section: Audio ─────────────────────────────────────────────────
            SectionHeader(title = "Audio")

            SettingSwitchRow(
                icon = Icons.Default.MusicNote,
                title = "Ambient Sound",
                subtitle = "Rain, wind, crickets and thunder audio",
                checked = config.ambientSoundEnabled,
                onCheckedChange = { applyChange(config.copy(ambientSoundEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Section header label ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

// ── Toggle row ────────────────────────────────────────────────────────────────

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) colors.primary.copy(alpha = 0.55f)   // fix #15: stronger active border
                    else colors.outline.copy(alpha = 0.25f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) colors.primaryContainer.copy(alpha = 0.32f)  // fix #15: more visible tint
                             else colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon chip on the left
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 0.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedTrackColor   = colors.primary,
                    uncheckedTrackColor = colors.outline,
                    uncheckedBorderColor = colors.outline,
                    uncheckedThumbColor = colors.onSurfaceVariant
                )
            )
        }
    }
}

// ── Refresh interval — Slider replacing the cramped +/- stepper ──────────────

@Composable
private fun RefreshIntervalRow(
    minutes: Int,
    onMinutesChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = colors.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Weather Refresh", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "How often weather is re-checked",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Text(
                    text = "$minutes min",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider: step = 5, range 5..60
            val steps = ((60 - 5) / 5) - 1  // 10 steps between 5 and 60
            Slider(
                value = minutes.toFloat(),
                onValueChange = { onMinutesChange(it.toInt()) },
                valueRange = 5f..60f,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5 min", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("60 min", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
