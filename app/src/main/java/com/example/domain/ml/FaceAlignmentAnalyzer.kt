package com.example.domain.ml

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.model.FaceAlignmentState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAlignmentAnalyzer(
    private val isFrontCamera: Boolean,
    private val onStateUpdated: (FaceAlignmentState) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.12f)
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isBusy) {
            imageProxy.close()
            return
        }

        isBusy = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
                val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

                if (faces.isEmpty()) {
                    onStateUpdated(
                        FaceAlignmentState(
                            hasFace = false,
                            faceCount = 0,
                            guidanceMessage = "No face detected in frame",
                            isWellAligned = false
                        )
                    )
                } else {
                    val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces.first()
                    val box = primaryFace.boundingBox
                    val centerX = box.centerX().toFloat()
                    val centerY = box.centerY().toFloat()

                    val halfW = imageWidth / 2f
                    val halfH = imageHeight / 2f

                    // Calculate offsets normalized from -1.0 to +1.0
                    var normX = (centerX - halfW) / halfW
                    if (isFrontCamera) {
                        normX = -normX // mirror horizontal direction for front camera intuitive guidance
                    }
                    val normY = (centerY - halfH) / halfH

                    val faceFraction = box.height().toFloat() / imageHeight

                    val (message, aligned) = when {
                        faceFraction < 0.16f -> "Face is too far. Move camera closer." to false
                        faceFraction > 0.65f -> "Face is too close. Move camera further away." to false
                        normX < -0.22f -> "Move camera slightly to the right" to false
                        normX > 0.22f -> "Move camera slightly to the left" to false
                        normY < -0.22f -> "Tilt camera slightly up" to false
                        normY > 0.22f -> "Tilt camera slightly down" to false
                        else -> {
                            val countDesc = if (faces.size > 1) "${faces.size} faces detected. Main face" else "Face"
                            "$countDesc is centered and well aligned" to true
                        }
                    }

                    onStateUpdated(
                        FaceAlignmentState(
                            hasFace = true,
                            faceCount = faces.size,
                            primaryFaceBounds = Rect(box),
                            horizontalOffset = normX,
                            verticalOffset = normY,
                            faceScale = faceFraction,
                            guidanceMessage = message,
                            isWellAligned = aligned
                        )
                    )
                }
            }
            .addOnFailureListener {
                // Ignore transient frame analysis failures
            }
            .addOnCompleteListener {
                isBusy = false
                imageProxy.close()
            }
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {}
    }
}
