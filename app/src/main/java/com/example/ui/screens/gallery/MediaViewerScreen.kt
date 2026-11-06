package com.example.ui.screens.gallery

import android.content.Intent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.components.AccessibleCircleButton
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun MediaViewerScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item by viewModel.activeMediaItem.collectAsState()
    val allMedia by viewModel.allMediaItems.collectAsState()
    val isSlideshow by viewModel.slideshowActive.collectAsState()

    BackHandler {
        viewModel.closeSubScreen()
    }

    if (item == null) {
        LaunchedEffect(Unit) { viewModel.closeSubScreen() }
        return
    }

    val currentItem = item!!
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Zoom and pan state for photos
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom when item changes
    LaunchedEffect(currentItem.uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentItem.isVideo) {
            // Media3 ExoPlayer for video
            val exoPlayer = remember(currentItem.uri) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(ExoMediaItem.fromUri(currentItem.uri))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(currentItem.uri) {
                onDispose { exoPlayer.release() }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Photo View with pinch zoom & pan
            AsyncImage(
                model = currentItem.uri,
                contentDescription = "${currentItem.displayName}. Photo capture.",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )
        }

        // Top App Bar
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.closeSubScreen() },
                    modifier = Modifier.semantics {
                        this.contentDescription = "Back to gallery"
                        this.role = Role.Button
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Text(
                    text = currentItem.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                Row {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Item details", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.toggleSlideshow(allMedia) }) {
                        Icon(
                            Icons.Default.Slideshow,
                            contentDescription = if (isSlideshow) "Stop slideshow" else "Start slideshow",
                            tint = if (isSlideshow) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Navigation & Actions Bar
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Item
                    IconButton(
                        onClick = { viewModel.previousMediaItem(allMedia) },
                        modifier = Modifier.semantics {
                            this.contentDescription = "Previous item"
                            this.role = Role.Button
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    // Favorite Toggle
                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentItem) }
                    ) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (currentItem.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (currentItem.isFavorite) Color.Red else Color.White
                        )
                    }

                    // Edit button (Photos only)
                    if (!currentItem.isVideo) {
                        IconButton(onClick = { viewModel.openMediaEditor(currentItem) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit photo", tint = Color.White)
                        }
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = currentItem.mimeType
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    // Delete button
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                    }

                    // Next Item
                    IconButton(
                        onClick = { viewModel.nextMediaItem(allMedia) },
                        modifier = Modifier.semantics {
                            this.contentDescription = "Next item"
                            this.role = Role.Button
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Item Details Dialog
    if (showInfoDialog) {
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(currentItem.dateModifiedMs))
        val sizeFormatted = String.format(Locale.US, "%.2f MB", currentItem.sizeBytes / (1024f * 1024f))
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Media Details") },
            text = {
                Column {
                    Text("Name: ${currentItem.displayName}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Date: $dateFormatted")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Size: $sizeFormatted")
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentItem.width > 0 && currentItem.height > 0) {
                        Text("Dimensions: ${currentItem.width} x ${currentItem.height}")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (currentItem.isVideo) {
                        Text("Duration: ${currentItem.durationSeconds} seconds")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text("Type: ${currentItem.mimeType}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("URI: ${currentItem.uri}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Move to Trash") },
            text = { Text("Are you sure you want to move this item to trash?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.moveToTrash(listOf(currentItem))
                        showDeleteConfirm = false
                        viewModel.closeSubScreen()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
