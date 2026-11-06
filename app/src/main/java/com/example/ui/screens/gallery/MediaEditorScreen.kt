package com.example.ui.screens.gallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.components.AccessibleStepperSlider
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun MediaEditorScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val item by viewModel.activeMediaItem.collectAsState()
    val scope = rememberCoroutineScope()

    BackHandler {
        viewModel.closeSubScreen()
    }

    if (item == null) {
        LaunchedEffect(Unit) { viewModel.closeSubScreen() }
        return
    }

    val currentItem = item!!
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var selectedFilter by remember { mutableStateOf("Original") }

    var showSaveDialog by remember { mutableStateOf(false) }

    // Load bitmap on entry
    LaunchedEffect(currentItem.uri) {
        isLoading = true
        val loaded = viewModel.mediaRepository.loadBitmap(currentItem.uri)
        originalBitmap = loaded
        displayedBitmap = loaded
        isLoading = false
    }

    fun applyAdjustments() {
        val base = originalBitmap ?: return
        scope.launch {
            val adjusted = withContext(Dispatchers.Default) {
                viewModel.mediaRepository.applyColorTransform(
                    source = base,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = when (selectedFilter) {
                        "Grayscale" -> 0f
                        "High Contrast" -> 1.5f
                        "Sepia" -> 0.8f
                        else -> saturation
                    }
                )
            }
            displayedBitmap = adjusted
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.closeSubScreen() },
                    modifier = Modifier.semantics {
                        this.contentDescription = "Cancel and close editor"
                        this.role = Role.Button
                    }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }

                Text(
                    text = "Edit Photo",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )

                IconButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.semantics {
                        this.contentDescription = "Save photo changes"
                        this.role = Role.Button
                    }
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Photo Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (displayedBitmap != null) {
                Image(
                    bitmap = displayedBitmap!!.asImageBitmap(),
                    contentDescription = "Photo editing preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Editing Controls Area
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Quick Transform Buttons: Rotate & Flip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val cur = originalBitmap ?: return@Button
                            val rotated = viewModel.mediaRepository.rotateBitmap(cur, 90f)
                            originalBitmap = rotated
                            applyAdjustments()
                            viewModel.voiceGuidanceManager.announce("Rotated 90 degrees clockwise")
                        }
                    ) {
                        Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate 90 deg")
                    }

                    Button(
                        onClick = {
                            val cur = originalBitmap ?: return@Button
                            val flipped = viewModel.mediaRepository.flipBitmap(cur, horizontal = true)
                            originalBitmap = flipped
                            applyAdjustments()
                            viewModel.voiceGuidanceManager.announce("Flipped horizontally")
                        }
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flip")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Preset Chips
                Text(
                    text = "Color Filters",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    listOf("Original", "Grayscale", "High Contrast", "Sepia").forEach { filterName ->
                        FilterChip(
                            selected = selectedFilter == filterName,
                            onClick = {
                                selectedFilter = filterName
                                applyAdjustments()
                                viewModel.voiceGuidanceManager.announce("Applied $filterName filter")
                            },
                            label = { Text(filterName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Brightness Slider
                AccessibleStepperSlider(
                    value = brightness,
                    range = -80f..80f,
                    step = 5f,
                    label = "Brightness",
                    valueFormatted = String.format(Locale.US, "%.0f", brightness),
                    onValueChange = {
                        brightness = it
                        applyAdjustments()
                    },
                    onIncrement = {
                        brightness = (brightness + 10f).coerceAtMost(80f)
                        applyAdjustments()
                    },
                    onDecrement = {
                        brightness = (brightness - 10f).coerceAtLeast(-80f)
                        applyAdjustments()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Contrast Slider
                AccessibleStepperSlider(
                    value = contrast,
                    range = 0.5f..2.0f,
                    step = 0.1f,
                    label = "Contrast",
                    valueFormatted = String.format(Locale.US, "%.1fx", contrast),
                    onValueChange = {
                        contrast = it
                        applyAdjustments()
                    },
                    onIncrement = {
                        contrast = (contrast + 0.1f).coerceAtMost(2f)
                        applyAdjustments()
                    },
                    onDecrement = {
                        contrast = (contrast - 0.1f).coerceAtLeast(0.5f)
                        applyAdjustments()
                    }
                )
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Photo Changes") },
            text = { Text("Would you like to save changes as a new copy or overwrite the original photo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        displayedBitmap?.let { bmp ->
                            scope.launch {
                                val savedUri = viewModel.mediaRepository.saveEditedImage(
                                    originalUri = currentItem.uri,
                                    bitmap = bmp,
                                    saveAsCopy = true
                                )
                                if (savedUri != null) {
                                    viewModel.voiceGuidanceManager.announce("Photo saved as a new copy")
                                    viewModel.closeSubScreen()
                                }
                            }
                        }
                    }
                ) {
                    Text("Save as copy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        displayedBitmap?.let { bmp ->
                            scope.launch {
                                val savedUri = viewModel.mediaRepository.saveEditedImage(
                                    originalUri = currentItem.uri,
                                    bitmap = bmp,
                                    saveAsCopy = false
                                )
                                if (savedUri != null) {
                                    viewModel.voiceGuidanceManager.announce("Photo changes overwritten")
                                    viewModel.closeSubScreen()
                                }
                            }
                        }
                    }
                ) {
                    Text("Overwrite original")
                }
            }
        )
    }
}
