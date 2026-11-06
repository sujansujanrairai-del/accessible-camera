package com.example.data.model

import android.graphics.Rect
import android.net.Uri

data class FaceAlignmentState(
    val hasFace: Boolean = false,
    val faceCount: Int = 0,
    val primaryFaceBounds: Rect? = null,
    val horizontalOffset: Float = 0f, // -1.0f (left) to +1.0f (right)
    val verticalOffset: Float = 0f,   // -1.0f (too high) to +1.0f (too low)
    val faceScale: Float = 0f,        // face height / preview height
    val guidanceMessage: String = "No face detected",
    val isWellAligned: Boolean = false
)

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedMs: Long,
    val durationSeconds: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedTimestampMs: Long = 0L,
    val userNotes: String = ""
)
