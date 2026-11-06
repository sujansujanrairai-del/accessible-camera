package com.example.data.model

sealed interface CameraCaptureState {
    data object Idle : CameraCaptureState
    data object Initializing : CameraCaptureState
    data object Ready : CameraCaptureState
    data object Focusing : CameraCaptureState
    data class CountingDown(val secondsRemaining: Int) : CameraCaptureState
    data object Capturing : CameraCaptureState
    data object Recording : CameraCaptureState
    data object RecordingPaused : CameraCaptureState
    data object Saving : CameraCaptureState
    data class Error(val message: String, val canRetry: Boolean = true) : CameraCaptureState
}

data class CameraSettingsState(
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val exposureCompensationIndex: Int = 0,
    val timer: CaptureTimer = CaptureTimer.OFF,
    val aspectRatio: String = "4:3",
    val videoResolution: String = "1080p",
    val isAudioEnabled: Boolean = true,
    val isExposureLocked: Boolean = false,
    val isFocusLocked: Boolean = false,
    val whiteBalance: String = "Auto",
    val iso: String = "Auto",
    val shutterSpeed: String = "Auto"
)
