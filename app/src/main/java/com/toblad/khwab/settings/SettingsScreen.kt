package com.toblad.khwab.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.di.KhwabProvider

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
    val context = LocalContext.current
    var config by remember { mutableStateOf(AuraBridge.getConfig()) }

    fun applyChange(newConfig: AuraConfig) {
        config = newConfig
        AuraBridge.updateConfig(newConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aura Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SettingSwitchRow(
                title = "Aura Enabled",
                subtitle = "Master switch for the ambient theme",
                checked = config.enabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        AuraBridge.activate()
                    } else {
                        AuraBridge.deactivate()
                    }
                    config = AuraBridge.getConfig()
                }
            )

            SettingSwitchRow(
                title = "Follow Real Time",
                subtitle = "Sky, sun and moon reflect the actual time of day",
                checked = config.autoTime,
                onCheckedChange = { applyChange(config.copy(autoTime = it)) }
            )

            SettingSwitchRow(
                title = "Follow Real Weather",
                subtitle = "Theme reflects live weather at your location",
                checked = config.autoWeather,
                onCheckedChange = { applyChange(config.copy(autoWeather = it)) }
            )

            SettingSwitchRow(
                title = "Animations",
                subtitle = "Rain, snow, fireflies, leaves and petals",
                checked = config.animationsEnabled,
                onCheckedChange = { applyChange(config.copy(animationsEnabled = it)) }
            )

            SettingSwitchRow(
                title = "Ambient Sound",
                subtitle = "Rain, wind, crickets and thunder audio",
                checked = config.ambientSoundEnabled,
                onCheckedChange = { applyChange(config.copy(ambientSoundEnabled = it)) }
            )

            RefreshIntervalRow(
                minutes = config.refreshIntervalMinutes,
                onMinutesChange = { applyChange(config.copy(refreshIntervalMinutes = it)) }
            )

            ApiKeyRow(
                initialKey = KhwabProvider.apiKeyStore.getApiKey().orEmpty(),
                onSave = { key ->
                    KhwabProvider.apiKeyStore.saveApiKey(key)
                    KhwabProvider.reinitialize(context)
                }
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun RefreshIntervalRow(
    minutes: Int,
    onMinutesChange: (Int) -> Unit
) {

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Refresh Interval", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "How often weather is re-checked (minutes)",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = { if (minutes > 1) onMinutesChange(minutes - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease interval")
            }

            Text(text = "$minutes", style = MaterialTheme.typography.titleMedium)

            IconButton(onClick = { if (minutes < 60) onMinutesChange(minutes + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase interval")
            }
        }
    }
}

@Composable
private fun ApiKeyRow(
    initialKey: String,
    onSave: (String) -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }
    var showKey by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "OpenAI API Key", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Required for background knowledge acquisition",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...") },
                singleLine = true,
                visualTransformation = if (showKey)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                }
            )
            Button(
                onClick = { onSave(key) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API Key")
            }
        }
    }
}