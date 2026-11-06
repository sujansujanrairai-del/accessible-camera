package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_metadata")
data class MediaMetadataEntity(
    @PrimaryKey
    val uriString: String,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedTimestampMs: Long = 0L,
    val userNotes: String = "",
    val orientationAngle: Int = 0,
    val albumName: String = "Camera"
)
