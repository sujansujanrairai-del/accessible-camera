package com.example.domain.ml

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class SceneDescriptionAnalyzer(
    private val onSceneDescribed: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.60f)
            .build()
    )

    private var isBusy = false
    private var lastAnalysisTimestamp = 0L

    fun shouldProcess(intervalSeconds: Int): Boolean {
        if (intervalSeconds <= 0) return false
        val now = System.currentTimeMillis()
        return (now - lastAnalysisTimestamp) >= (intervalSeconds * 1000L)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isBusy) {
            imageProxy.close()
            return
        }

        isBusy = true
        lastAnalysisTimestamp = System.currentTimeMillis()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                if (labels.isNotEmpty()) {
                    val topLabels = labels.take(4).map { it.text }
                    val summary = "Scene contains " + topLabels.joinToString(", ")
                    onSceneDescribed(summary)
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
            labeler.close()
        } catch (_: Exception) {}
    }
}
