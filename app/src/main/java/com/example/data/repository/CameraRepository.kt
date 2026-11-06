package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.example.data.model.CameraCapabilities
import com.example.data.model.CameraFacing
import com.example.data.model.FlashMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraRepository(private val context: Context) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    fun detectCapabilities(cameraFacing: CameraFacing): CameraCapabilities {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) return CameraCapabilities()

        var hasBack = false
        var hasFront = false
        var hasFlash = false
        var minZoom = 1.0f
        var maxZoom = 5.0f
        var minExp = -4
        var maxExp = 4
        var expStep = 0.5f

        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) hasBack = true
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) hasFront = true

                val targetFacing = if (cameraFacing == CameraFacing.BACK) {
                    CameraCharacteristics.LENS_FACING_BACK
                } else {
                    CameraCharacteristics.LENS_FACING_FRONT
                }

                if (facing == targetFacing) {
                    hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                        if (zoomRange != null) {
                            minZoom = zoomRange.lower
                            maxZoom = zoomRange.upper
                        }
                    }
                    val expRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    if (expRange != null) {
                        minExp = expRange.lower
                        maxExp = expRange.upper
                    }
                    val stepRational = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    if (stepRational != null && stepRational.denominator != 0) {
                        expStep = stepRational.toFloat()
                    }
                }
            }
        } catch (_: Exception) {}

        val caps = CameraCapabilities(
            hasBackCamera = hasBack,
            hasFrontCamera = hasFront,
            hasFlashUnit = hasFlash,
            minZoomRatio = minZoom,
            maxZoomRatio = maxZoom.coerceAtLeast(minZoom + 1f),
            supportedZoomRatios = listOf(1.0f, 2.0f, (maxZoom / 2f).coerceAtLeast(2f), maxZoom),
            minExposureIndex = minExp,
            maxExposureIndex = maxExp,
            exposureStep = expStep,
            supportsTorch = hasFlash,
            supportsVideoPause = true
        )
        _capabilities.value = caps
        return caps
    }

    fun setCameraReferences(
        provider: ProcessCameraProvider?,
        camera: Camera?,
        capture: ImageCapture?,
        video: VideoCapture<Recorder>?
    ) {
        this.cameraProvider = provider
        this.currentCamera = camera
        this.imageCapture = capture
        this.videoCapture = video

        currentCamera?.cameraInfo?.let { updateCapabilitiesFromCameraInfo(it) }
    }

    private fun updateCapabilitiesFromCameraInfo(info: CameraInfo) {
        val zoomState = info.zoomState.value
        val exposureState = info.exposureState
        val hasFlash = info.hasFlashUnit()

        _capabilities.value = _capabilities.value.copy(
            hasFlashUnit = hasFlash,
            supportsTorch = hasFlash,
            minZoomRatio = zoomState?.minZoomRatio ?: 1.0f,
            maxZoomRatio = zoomState?.maxZoomRatio ?: 5.0f,
            minExposureIndex = exposureState.exposureCompensationRange.lower,
            maxExposureIndex = exposureState.exposureCompensationRange.upper,
            exposureStep = exposureState.exposureCompensationStep.toFloat()
        )
    }

    fun setZoomRatio(ratio: Float) {
        currentCamera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setExposureCompensation(index: Int) {
        currentCamera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun enableTorch(enable: Boolean) {
        currentCamera?.cameraControl?.enableTorch(enable)
    }

    fun focusAt(pointFactory: MeteringPointFactory, x: Float, y: Float) {
        val point = pointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        currentCamera?.cameraControl?.startFocusAndMetering(action)
    }

    fun takePhoto(
        flashMode: FlashMode,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera is not ready")
            return
        }

        capture.flashMode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "IMG_${timeStamp}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AccessibleCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(savedUri, contentValues, null, null)
                        }
                        onSuccess(savedUri)
                    } else {
                        onError("Saved file URI is null")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Failed to capture photo")
                }
            }
        )
    }

    fun startRecording(
        audioEnabled: Boolean,
        onEvent: (VideoRecordEvent) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onError("Video recorder is not ready")
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "VID_${timeStamp}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/AccessibleCamera")
            }
        }

        val mediaStoreOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val pendingRecording = capture.output.prepareRecording(context, mediaStoreOptions)
        if (audioEnabled) {
            try {
                pendingRecording.withAudioEnabled()
            } catch (_: SecurityException) {
                // Audio permission missing, continue without audio
            }
        }

        activeRecording = pendingRecording.start(
            ContextCompat.getMainExecutor(context),
            onEvent
        )
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun release() {
        cameraExecutor.shutdown()
        activeRecording?.stop()
        activeRecording = null
    }
}
