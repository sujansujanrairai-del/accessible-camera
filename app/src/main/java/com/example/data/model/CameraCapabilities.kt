package com.example.data.model

data class CameraCapabilities(
    val hasBackCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val hasFlashUnit: Boolean = false,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,
    val supportedZoomRatios: List<Float> = listOf(1.0f, 2.0f, 5.0f),
    val hasOpticalStabilization: Boolean = false,
    val hasElectronicStabilization: Boolean = false,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val exposureStep: Float = 0.5f,
    val supportsVideoPause: Boolean = true,
    val supportsTorch: Boolean = false,
    val supportedAspectRatios: List<String> = listOf("4:3", "16:9", "1:1"),
    val supportedVideoResolutions: List<String> = listOf("1080p", "720p"),
    val isAutoExposureLockSupported: Boolean = true,
    val isAutoFocusLockSupported: Boolean = true
)

enum class CameraFacing {
    BACK,
    FRONT
}

enum class FlashMode {
    OFF,
    ON,
    AUTO
}

enum class CaptureTimer {
    OFF,
    SECONDS_3,
    SECONDS_5,
    SECONDS_10;

    val seconds: Int
        get() = when (this) {
            OFF -> 0
            SECONDS_3 -> 3
            SECONDS_5 -> 5
            SECONDS_10 -> 10
        }
}
