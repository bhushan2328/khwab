package com.toblad.khwab.debug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.aura.ui.LightningBus

/**
 * Debug-only Aura Testing Console.
 *
 * Allows manual control of weather, animations, and Aura lifecycle
 * at runtime without rebuilding the APK.
 *
 * This composable must only be reached from debug builds.
 * It is hosted by AuraDebugActivity which lives in src/debug/
 * and is therefore excluded from release APKs automatically.
 *
 * All commands are routed through [AuraBridge] — never directly
 * to WeatherEngine or any internal Aura component.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuraDebugConsole(
    onBackClick: () -> Unit = {}
) {
    // Observe live Aura state — single source of truth, no separate variables.
    val snapshot by AuraBridge.snapshotFlow.collectAsState()
    val theme = snapshot.theme

    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aura Debug", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Weather ───────────────────────────────────────────────────────
            DebugSectionHeader("Weather")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherState.entries.forEach { weather ->
                    val isSelected = theme.weatherState == weather
                    if (isSelected) {
                        Button(
                            onClick = { AuraBridge.updateWeather(weather) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary
                            )
                        ) {
                            Text(weather.name)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { AuraBridge.updateWeather(weather) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.outline)
                        ) {
                            Text(weather.name)
                        }
                    }
                }
            }

            // ── Animations ────────────────────────────────────────────────────
            DebugSectionHeader("Animations")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val animOn = theme.animationsEnabled
                Button(
                    onClick = {
                        AuraBridge.updateConfig(AuraBridge.getConfig().copy(animationsEnabled = true))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (animOn) colors.primary else colors.surfaceVariant,
                        contentColor   = if (animOn) colors.onPrimary else colors.onSurfaceVariant
                    )
                ) { Text("ON") }

                Button(
                    onClick = {
                        AuraBridge.updateConfig(AuraBridge.getConfig().copy(animationsEnabled = false))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!animOn) colors.error else colors.surfaceVariant,
                        contentColor   = if (!animOn) colors.onError else colors.onSurfaceVariant
                    )
                ) { Text("OFF") }
            }

            // ── Aura Lifecycle ────────────────────────────────────────────────
            DebugSectionHeader("Aura")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val isActive = theme.auraState == AuraState.ACTIVE
                Button(
                    onClick = { AuraBridge.activate() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) colors.primary else colors.surfaceVariant,
                        contentColor   = if (isActive) colors.onPrimary else colors.onSurfaceVariant
                    )
                ) { Text("ACTIVATE") }

                Button(
                    onClick = { AuraBridge.deactivate() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isActive) colors.error else colors.surfaceVariant,
                        contentColor   = if (!isActive) colors.onError else colors.onSurfaceVariant
                    )
                ) { Text("DEACTIVATE") }
            }

            // ── Lightning ─────────────────────────────────────────────────────
            DebugSectionHeader("Lightning")

            Button(
                onClick = { LightningBus.triggerNow() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TRIGGER LIGHTNING", color = Color.Black)
            }

            // ── Current State ─────────────────────────────────────────────────
            DebugSectionHeader("Current State")

            Text(
                text = buildString {
                    appendLine("Weather:    ${theme.weatherState}")
                    appendLine("Aura:       ${theme.auraState}")
                    appendLine("Animations: ${if (theme.animationsEnabled) "ON" else "OFF"}")
                    appendLine("Time phase: ${theme.timePhase}")
                    appendLine("Enabled:    ${theme.enabled}")
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DebugSectionHeader(title: String) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 1.dp
        )
    }
}
