package com.example.ui.screens.camera

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CameraCaptureState
import com.example.data.model.CameraFacing
import com.example.ui.components.AccessibleCircleButton
import com.example.ui.components.GridOverlay
import com.example.ui.components.HorizonLevelOverlay
import com.example.ui.viewmodel.AppViewModel
import java.util.Locale

@Composable
fun VideoScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSettings by viewModel.cameraSettings.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraCaps by viewModel.cameraCapabilities.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val tiltAngle by viewModel.deviceTiltAngle.collectAsState()
    val isLevel by viewModel.isDeviceLevel.collectAsState()

    var showAdvancedSheet by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val isRecording = cameraState is CameraCaptureState.Recording
    val isPaused = cameraState is CameraCaptureState.RecordingPaused

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bind CameraX with VideoCapture
        LaunchedEffect(cameraSettings.cameraFacing, previewViewRef) {
            val pView = previewViewRef ?: return@LaunchedEffect
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val selector = if (cameraSettings.cameraFacing == CameraFacing.FRONT) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pView.surfaceProvider
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.FHD, androidx.camera.video.FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)

                try {
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        videoCapture
                    )
                    viewModel.cameraRepository.setCameraReferences(provider, camera, null, videoCapture)
                } catch (e: Exception) {
                    viewModel.voiceGuidanceManager.announce("Failed to bind video camera: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Grid lines overlay
        GridOverlay(gridType = prefs.gridOverlay)

        // Horizon level guide
        if (prefs.horizonLevelEnabled) {
            HorizonLevelOverlay(tiltAngle = tiltAngle, isLevel = isLevel)
        }

        // Top Status Bar (Recording Timer & Toggles)
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Toggle
                AccessibleCircleButton(
                    icon = if (cameraSettings.isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (cameraSettings.isAudioEnabled) "Audio recording on" else "Audio recording muted",
                    onClick = { viewModel.toggleAudioRecording() },
                    size = 48.dp,
                    testTag = "audio_toggle_button"
                )

                // Recording Timer Badge
                if (isRecording || isPaused) {
                    val mins = recordingDuration / 60
                    val secs = recordingDuration % 60
                    val timeFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPaused) Color(0xFFE65100) else Color(0xFFB71C1C),
                        modifier = Modifier.semantics {
                            this.contentDescription = if (isPaused) "Recording paused at $timeFormatted" else "Recording $timeFormatted"
                            this.liveRegion = LiveRegionMode.Polite
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeFormatted,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Video Ready",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Torch Light Toggle
                AccessibleCircleButton(
                    icon = if (cameraSettings.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = if (cameraSettings.isTorchOn) "Torch on" else "Torch off",
                    onClick = { viewModel.toggleTorch() },
                    size = 48.dp,
                    enabled = cameraCaps.supportsTorch,
                    testTag = "torch_toggle_button"
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(bottom = 24.dp, top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom in / out controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    onClick = { viewModel.setZoom(cameraSettings.zoomRatio - 0.5f) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val formattedZoom = String.format(Locale.US, "%.1fx", cameraSettings.zoomRatio)
                    Text(
                        text = formattedZoom,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    onClick = { viewModel.setZoom(cameraSettings.zoomRatio + 0.5f) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                    }
                }
            }

            // Primary Record / Pause / Stop controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary action: Pause or Switch camera
                if (isRecording || isPaused) {
                    AccessibleCircleButton(
                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume recording" else "Pause recording",
                        onClick = {
                            if (isPaused) viewModel.resumeVideoRecording() else viewModel.pauseVideoRecording()
                        },
                        size = 56.dp,
                        containerColor = Color.White.copy(alpha = 0.25f),
                        contentColor = Color.White,
                        testTag = "pause_resume_button"
                    )
                } else {
                    AccessibleCircleButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "Advanced controls",
                        onClick = { showAdvancedSheet = true },
                        size = 56.dp,
                        containerColor = Color.White.copy(alpha = 0.25f),
                        contentColor = Color.White,
                        testTag = "video_advanced_button"
                    )
                }

                // Primary Record Button (Large 80dp)
                Surface(
                    onClick = {
                        if (isRecording || isPaused) {
                            viewModel.stopVideoRecording()
                        } else {
                            viewModel.startVideoRecording()
                        }
                    },
                    shape = CircleShape,
                    color = if (isRecording || isPaused) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .semantics {
                            this.contentDescription = if (isRecording || isPaused) "Stop recording" else "Start recording"
                        }
                        .testTag("record_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording || isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F))
                            )
                        }
                    }
                }

                // Switch Camera Button
                AccessibleCircleButton(
                    icon = Icons.Default.Cameraswitch,
                    contentDescription = if (cameraSettings.cameraFacing == CameraFacing.BACK) "Switch to front camera" else "Switch to back camera",
                    onClick = { if (!isRecording && !isPaused) viewModel.switchCamera() },
                    size = 56.dp,
                    enabled = !isRecording && !isPaused,
                    containerColor = Color.White.copy(alpha = 0.25f),
                    contentColor = Color.White,
                    testTag = "video_switch_camera_button"
                )
            }
        }

        if (showAdvancedSheet) {
            AdvancedControlsSheet(
                viewModel = viewModel,
                onDismiss = { showAdvancedSheet = false }
            )
        }
    }
}
