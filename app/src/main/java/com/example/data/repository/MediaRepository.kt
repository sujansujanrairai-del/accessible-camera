package com.example.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.database.GalleryDatabase
import com.example.data.database.MediaMetadataEntity
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaRepository(private val context: Context) {
    private val database = GalleryDatabase.getInstance(context)
    private val dao = database.mediaMetadataDao()

    fun getMediaItemsFlow(): Flow<List<MediaItem>> {
        val mediaStoreFlow = flow {
            emit(queryMediaStore())
        }.flowOn(Dispatchers.IO)

        return combine(mediaStoreFlow, dao.getAllMetadata()) { mediaItems, metadataList ->
            val metadataMap = metadataList.associateBy { it.uriString }
            mediaItems.map { item ->
                val meta = metadataMap[item.uri.toString()]
                if (meta != null) {
                    item.copy(
                        isFavorite = meta.isFavorite,
                        isTrashed = meta.isTrashed,
                        trashedTimestampMs = meta.trashedTimestampMs,
                        userNotes = meta.userNotes
                    )
                } else {
                    item
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun queryMediaStore(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver

        // Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(imageUri, imageProjection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(imageUri, id)
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "Image_$id",
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                            sizeBytes = cursor.getLong(sizeCol),
                            dateModifiedMs = cursor.getLong(dateCol) * 1000L,
                            durationSeconds = 0,
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            isVideo = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(videoUri, videoProjection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(videoUri, id)
                    val durMs = cursor.getLong(durCol)
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "Video_$id",
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                            sizeBytes = cursor.getLong(sizeCol),
                            dateModifiedMs = cursor.getLong(dateCol) * 1000L,
                            durationSeconds = durMs / 1000L,
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            isVideo = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        items.sortedByDescending { it.dateModifiedMs }
    }

    suspend fun toggleFavorite(item: MediaItem) = withContext(Dispatchers.IO) {
        val newFav = !item.isFavorite
        val existing = dao.getMetadata(item.uri.toString())
        if (existing == null) {
            dao.insertOrUpdate(
                MediaMetadataEntity(
                    uriString = item.uri.toString(),
                    isFavorite = newFav
                )
            )
        } else {
            dao.setFavorite(item.uri.toString(), newFav)
        }
    }

    suspend fun moveToTrash(item: MediaItem) = withContext(Dispatchers.IO) {
        val existing = dao.getMetadata(item.uri.toString())
        val now = System.currentTimeMillis()
        if (existing == null) {
            dao.insertOrUpdate(
                MediaMetadataEntity(
                    uriString = item.uri.toString(),
                    isTrashed = true,
                    trashedTimestampMs = now
                )
            )
        } else {
            dao.setTrashed(item.uri.toString(), true, now)
        }
    }

    suspend fun restoreFromTrash(item: MediaItem) = withContext(Dispatchers.IO) {
        dao.setTrashed(item.uri.toString(), false, 0L)
    }

    suspend fun deletePermanently(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(item.uri, null, null)
            dao.deleteMetadata(item.uri.toString())
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveEditedImage(
        originalUri: Uri,
        bitmap: Bitmap,
        saveAsCopy: Boolean
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetUri: Uri
            var outputStream: OutputStream?

            if (saveAsCopy) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "EDIT_${timeStamp}.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AccessibleCamera")
                    }
                }
                targetUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                outputStream = context.contentResolver.openOutputStream(targetUri)
            } else {
                targetUri = originalUri
                outputStream = context.contentResolver.openOutputStream(targetUri, "rwt")
            }

            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
            targetUri
        } catch (_: Exception) {
            null
        }
    }

    fun applyColorTransform(
        source: Bitmap,
        brightness: Float, // -100 to +100
        contrast: Float,   // 0.5 to 2.0
        saturation: Float  // 0.0 to 2.0
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Saturation matrix
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }

        // Contrast and brightness matrix
        val scale = contrast
        val translate = brightness + (1.0f - scale) * 128f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combinedMatrix = ColorMatrix().apply {
            postConcat(satMatrix)
            postConcat(contrastMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
