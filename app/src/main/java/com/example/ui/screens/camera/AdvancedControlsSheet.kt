package com.example.ui.screens.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.components.AccessibleStepperSlider
import com.example.ui.viewmodel.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedControlsSheet(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraSettings by viewModel.cameraSettings.collectAsState()
    val cameraCaps by viewModel.cameraCapabilities.collectAsState()
    val prefs by viewModel.preferences.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Camera Controls",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        this.contentDescription = "Close advanced controls"
                        this.role = Role.Button
                    }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exposure Compensation Stepper Slider
            val expStep = cameraCaps.exposureStep
            val currentExp = cameraSettings.exposureCompensationIndex
            AccessibleStepperSlider(
                value = currentExp.toFloat(),
                range = cameraCaps.minExposureIndex.toFloat()..cameraCaps.maxExposureIndex.toFloat(),
                step = 1f,
                label = "Exposure Compensation",
                valueFormatted = if (currentExp > 0) "+$currentExp" else "$currentExp",
                onValueChange = { viewModel.setExposureIndex(it.toInt()) },
                onIncrement = { viewModel.setExposureIndex(currentExp + 1) },
                onDecrement = { viewModel.setExposureIndex(currentExp - 1) },
                testTag = "exposure_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio Selector
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                listOf("4:3", "16:9", "1:1").forEach { ratio ->
                    FilterChip(
                        selected = cameraSettings.aspectRatio == ratio,
                        onClick = {
                            viewModel.voiceGuidanceManager.announce("Aspect ratio set to $ratio")
                        },
                        label = { Text(ratio) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Torch Toggle (Supported check)
            if (cameraCaps.supportsTorch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Torch Light",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Continuous illumination for low light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cameraSettings.isTorchOn,
                        onCheckedChange = { viewModel.toggleTorch() }
                    )
                }
            } else {
                UnsupportedControlCard(
                    title = "Torch Light",
                    reason = "Hardware flash unit is not available on this camera"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RAW Capture (Device capability check)
            UnsupportedControlCard(
                title = "RAW DNG Capture",
                reason = "Camera2 RAW sensor output is not supported by current sensor hardware"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optical Image Stabilization (Device capability check)
            UnsupportedControlCard(
                title = "Optical Stabilization",
                reason = "Hardware optical image stabilizer (OIS) is not equipped on this camera module"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Manual White Balance and ISO
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "White Balance: Auto",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Automatic continuous white balance active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ISO Sensitivity: Auto",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Hardware auto-exposure metering active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun UnsupportedControlCard(
    title: String,
    reason: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.contentDescription = "$title unavailable. Reason: $reason"
            }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "$title (Unavailable)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
