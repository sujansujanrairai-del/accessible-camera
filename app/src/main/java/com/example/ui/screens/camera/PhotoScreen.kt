package com.example.ui.screens.camera

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.example.data.model.CameraCaptureState
import com.example.data.model.CameraFacing
import com.example.data.model.CaptureTimer
import com.example.data.model.FlashMode
import com.example.domain.ml.FaceAlignmentAnalyzer
import com.example.domain.ml.SceneDescriptionAnalyzer
import com.example.ui.components.AccessibleCircleButton
import com.example.ui.components.AccessibleShutterButton
import com.example.ui.components.FaceAlignmentOverlay
import com.example.ui.components.GridOverlay
import com.example.ui.components.HorizonLevelOverlay
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.MainTab
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSettings by viewModel.cameraSettings.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraCaps by viewModel.cameraCapabilities.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val faceState by viewModel.faceAlignmentState.collectAsState()
    val tiltAngle by viewModel.deviceTiltAngle.collectAsState()
    val isLevel by viewModel.isDeviceLevel.collectAsState()
    val allMedia by viewModel.allMediaItems.collectAsState()
    val isListeningVoice by viewModel.voiceCommandManager.isListening.collectAsState()

    var showAdvancedSheet by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val latestMedia = allMedia.firstOrNull { !it.isTrashed }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceAnalyzer = remember(cameraSettings.cameraFacing) {
        FaceAlignmentAnalyzer(
            isFrontCamera = cameraSettings.cameraFacing == CameraFacing.FRONT,
            onStateUpdated = { state -> viewModel.updateFaceAlignment(state) }
        )
    }
    val sceneAnalyzer = remember {
        SceneDescriptionAnalyzer(
            onSceneDescribed = { desc -> viewModel.updateSceneDescription(desc) }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            faceAnalyzer.close()
            sceneAnalyzer.close()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // CameraX Preview
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
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        previewViewRef?.let { pView ->
                            val factory = pView.meteringPointFactory
                            viewModel.cameraRepository.focusAt(factory, offset.x, offset.y)
                            viewModel.feedbackManager.playFocusLocked(
                                audioEnabled = prefs.audioCuesEnabled,
                                hapticEnabled = prefs.hapticEnabled
                            )
                            viewModel.voiceGuidanceManager.announce(
                                "Focusing",
                                com.example.domain.accessibility.VoiceGuidanceManager.Priority.NORMAL
                            )
                        }
                    }
                }
        )

        // Bind camera use cases whenever cameraFacing changes
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

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (sceneAnalyzer.shouldProcess(prefs.sceneDescriptionIntervalSeconds)) {
                        sceneAnalyzer.analyze(imageProxy)
                    } else if (prefs.faceGuidanceEnabled) {
                        faceAnalyzer.analyze(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture,
                        analysis
                    )
                    viewModel.cameraRepository.setCameraReferences(provider, camera, capture, null)
                } catch (e: Exception) {
                    viewModel.voiceGuidanceManager.announce(
                        "Camera initialization failed: ${e.message}",
                        com.example.domain.accessibility.VoiceGuidanceManager.Priority.CRITICAL
                    )
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Grid lines overlay
        GridOverlay(gridType = prefs.gridOverlay)

        // Horizon Level overlay
        if (prefs.horizonLevelEnabled) {
            HorizonLevelOverlay(tiltAngle = tiltAngle, isLevel = isLevel)
        }

        // Face Alignment Guidance overlay
        FaceAlignmentOverlay(state = faceState, enabled = prefs.faceGuidanceEnabled)

        // Voice Command Listening Indicator
        if (isListeningVoice) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .semantics {
                        this.contentDescription = "Listening for voice command. Speak now."
                        this.liveRegion = LiveRegionMode.Assertive
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Listening for command...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Countdown Timer Overlay
        if (cameraState is CameraCaptureState.CountingDown) {
            val sec = (cameraState as CameraCaptureState.CountingDown).secondsRemaining
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$sec",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    modifier = Modifier.semantics {
                        this.contentDescription = "Countdown: $sec seconds"
                        this.liveRegion = LiveRegionMode.Assertive
                    }
                )
            }
        }

        // Top Quick Controls Bar
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
                // Flash button
                AccessibleCircleButton(
                    icon = when (cameraSettings.flashMode) {
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                        FlashMode.OFF -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash mode: ${cameraSettings.flashMode.name.lowercase()}",
                    onClick = { viewModel.cycleFlashMode() },
                    size = 48.dp,
                    enabled = cameraCaps.hasFlashUnit,
                    testTag = "flash_button"
                )

                // Timer button
                AccessibleCircleButton(
                    icon = Icons.Default.Timer,
                    contentDescription = if (cameraSettings.timer == CaptureTimer.OFF) "Timer off" else "Timer: ${cameraSettings.timer.seconds} seconds",
                    onClick = { viewModel.cycleTimer() },
                    size = 48.dp,
                    testTag = "timer_button"
                )

                // Voice Command button
                AccessibleCircleButton(
                    icon = Icons.Default.Mic,
                    contentDescription = "Activate voice commands",
                    onClick = {
                        viewModel.voiceCommandManager.startListening()
                        viewModel.feedbackManager.performClickHaptic(hapticEnabled = prefs.hapticEnabled)
                    },
                    size = 48.dp,
                    containerColor = if (isListeningVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isListeningVoice) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "voice_command_button"
                )

                // Advanced Controls button
                AccessibleCircleButton(
                    icon = Icons.Default.Tune,
                    contentDescription = "Open advanced camera controls",
                    onClick = { showAdvancedSheet = true },
                    size = 48.dp,
                    testTag = "advanced_controls_button"
                )
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(bottom = 24.dp, top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Zoom step buttons
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
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom out",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
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
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom in",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Primary Shutter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latest capture thumbnail
                Surface(
                    onClick = {
                        if (latestMedia != null) {
                            viewModel.openMediaViewer(latestMedia)
                        } else {
                            viewModel.selectTab(MainTab.GALLERY)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.DarkGray,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            this.contentDescription = if (latestMedia != null) "Latest capture: ${latestMedia.displayName}" else "Open gallery"
                        }
                        .testTag("gallery_thumbnail_button")
                ) {
                    if (latestMedia != null) {
                        AsyncImage(
                            model = latestMedia.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Shutter Button
                AccessibleShutterButton(
                    onClick = { viewModel.takePhoto() },
                    isCapturing = cameraState is CameraCaptureState.Capturing || cameraState is CameraCaptureState.Saving,
                    contentDescription = "Shutter button. Take photo.",
                    testTag = "shutter_button"
                )

                // Switch Camera Button
                AccessibleCircleButton(
                    icon = Icons.Default.Cameraswitch,
                    contentDescription = if (cameraSettings.cameraFacing == CameraFacing.BACK) "Switch to front camera" else "Switch to back camera",
                    onClick = { viewModel.switchCamera() },
                    size = 56.dp,
                    containerColor = Color.White.copy(alpha = 0.25f),
                    contentColor = Color.White,
                    testTag = "switch_camera_button"
                )
            }
        }

        // Advanced Settings Modal Sheet
        if (showAdvancedSheet) {
            AdvancedControlsSheet(
                viewModel = viewModel,
                onDismiss = { showAdvancedSheet = false }
            )
        }
    }
}
