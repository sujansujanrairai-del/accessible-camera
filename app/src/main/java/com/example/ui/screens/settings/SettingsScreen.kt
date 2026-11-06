package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AccessibleStepperSlider
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Section
        SettingsSectionHeader(title = "Accessibility and Voice Guidance")

        // Voice Guidance Master Toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_voice_guidance),
            subtitle = stringResource(R.string.settings_voice_guidance_desc),
            checked = prefs.voiceGuidanceEnabled,
            onCheckedChange = {
                scope.launch { viewModel.settingsDataStore.setVoiceGuidance(it) }
            },
            testTag = "voice_guidance_toggle"
        )

        // Face Guidance Toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_face_guidance),
            subtitle = stringResource(R.string.settings_face_guidance_desc),
            checked = prefs.faceGuidanceEnabled,
            onCheckedChange = {
                scope.launch { viewModel.settingsDataStore.setFaceGuidance(it) }
            },
            testTag = "face_guidance_toggle"
        )

        // Scene Description Interval
        Text(
            text = "Scene Description Interval",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                FilterChip(
                    selected = prefs.sceneDescriptionIntervalSeconds == sec,
                    onClick = {
                        scope.launch { viewModel.settingsDataStore.setSceneDescriptionInterval(sec) }
                    },
                    label = { Text(label) }
                )
            }
        }

        // Speech Rate Slider
        AccessibleStepperSlider(
            value = prefs.speechRate,
            range = 0.5f..2.0f,
            step = 0.1f,
            label = stringResource(R.string.settings_speech_rate),
            valueFormatted = String.format(Locale.US, "%.1fx", prefs.speechRate),
            onValueChange = { scope.launch { viewModel.settingsDataStore.setSpeechRate(it) } },
            onIncrement = {
                val next = (prefs.speechRate + 0.1f).coerceAtMost(2.0f)
                scope.launch { viewModel.settingsDataStore.setSpeechRate(next) }
            },
            onDecrement = {
                val prev = (prefs.speechRate - 0.1f).coerceAtLeast(0.5f)
                scope.launch { viewModel.settingsDataStore.setSpeechRate(prev) }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Speech Pitch Slider
        AccessibleStepperSlider(
            value = prefs.speechPitch,
            range = 0.5f..1.5f,
            step = 0.1f,
            label = stringResource(R.string.settings_speech_pitch),
            valueFormatted = String.format(Locale.US, "%.1f", prefs.speechPitch),
            onValueChange = { scope.launch { viewModel.settingsDataStore.setSpeechPitch(it) } },
            onIncrement = {
                val next = (prefs.speechPitch + 0.1f).coerceAtMost(1.5f)
                scope.launch { viewModel.settingsDataStore.setSpeechPitch(next) }
            },
            onDecrement = {
                val prev = (prefs.speechPitch - 0.1f).coerceAtLeast(0.5f)
                scope.launch { viewModel.settingsDataStore.setSpeechPitch(prev) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Section
        SettingsSectionHeader(title = "Haptic and Audio Feedback")

        SettingsToggleRow(
            title = stringResource(R.string.settings_haptic_feedback),
            subtitle = stringResource(R.string.settings_haptic_desc),
            checked = prefs.hapticEnabled,
            onCheckedChange = { scope.launch { viewModel.settingsDataStore.setHaptic(it) } }
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_audio_cues),
            subtitle = stringResource(R.string.settings_audio_cues_desc),
            checked = prefs.audioCuesEnabled,
            onCheckedChange = { scope.launch { viewModel.settingsDataStore.setAudioCues(it) } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual and Theme Section
        SettingsSectionHeader(title = "Theme and Contrast")

        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            listOf(
                "SYSTEM" to "System",
                "HIGH_CONTRAST_DARK" to "Dark High Contrast",
                "HIGH_CONTRAST_LIGHT" to "Light High Contrast",
                "YELLOW_ON_BLACK" to "Yellow on Black"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = prefs.themeMode == mode,
                    onClick = { scope.launch { viewModel.settingsDataStore.setThemeMode(mode) } },
                    label = { Text(label) }
                )
            }
        }

        SettingsToggleRow(
            title = stringResource(R.string.settings_level_guide),
            subtitle = stringResource(R.string.settings_level_guide_desc),
            checked = prefs.horizonLevelEnabled,
            onCheckedChange = { scope.launch { viewModel.settingsDataStore.setHorizonLevel(it) } }
        )

        Text(
            text = "Grid Overlay",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            listOf("NONE" to "None", "RULE_OF_THIRDS" to "Rule of Thirds", "GOLDEN_RATIO" to "Golden Ratio").forEach { (grid, label) ->
                FilterChip(
                    selected = prefs.gridOverlay == grid,
                    onClick = { scope.launch { viewModel.settingsDataStore.setGridOverlay(grid) } },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy and Metadata
        SettingsSectionHeader(title = "Privacy and Metadata")

        SettingsToggleRow(
            title = stringResource(R.string.settings_location),
            subtitle = stringResource(R.string.settings_location_desc),
            checked = prefs.locationTaggingEnabled,
            onCheckedChange = { scope.launch { viewModel.settingsDataStore.setLocationTagging(it) } }
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_watermark),
            subtitle = stringResource(R.string.settings_watermark_desc),
            checked = prefs.watermarkEnabled,
            onCheckedChange = { scope.launch { viewModel.settingsDataStore.setWatermark(it) } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // About & Developer Information
        SettingsSectionHeader(title = stringResource(R.string.settings_about_header))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.developer_credit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Local-first privacy: No analytics or network use without explicit consent. All media and data remain securely on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Settings Button
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_settings_button")
        ) {
            Text(stringResource(R.string.settings_reset_button))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences") },
            text = { Text(stringResource(R.string.settings_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { viewModel.settingsDataStore.resetDefaults() }
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
            )
        }
    }
}
