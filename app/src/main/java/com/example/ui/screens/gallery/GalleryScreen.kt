package com.example.ui.screens.gallery

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.components.AccessibleCircleButton
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.GalleryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMediaItems.collectAsState()
    val filter by viewModel.galleryFilter.collectAsState()
    val searchQuery by viewModel.gallerySearchQuery.collectAsState()
    val isGrid by viewModel.isGridView.collectAsState()
    val selectedUris by viewModel.selectedMediaUris.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Filter media items
    val filteredMedia = allMedia.filter { item ->
        val matchesFilter = when (filter) {
            GalleryFilter.ALL -> !item.isTrashed
            GalleryFilter.PHOTOS -> !item.isVideo && !item.isTrashed
            GalleryFilter.VIDEOS -> item.isVideo && !item.isTrashed
            GalleryFilter.FAVORITES -> item.isFavorite && !item.isTrashed
            GalleryFilter.TRASH -> item.isTrashed
        }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            item.displayName.contains(searchQuery, ignoreCase = true) ||
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(item.dateModifiedMs)).contains(searchQuery)
        }
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar & View Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setGallerySearchQuery(it) },
                placeholder = { Text("Search media...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setGallerySearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gallery_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            AccessibleCircleButton(
                icon = if (isGrid) Icons.Default.Menu else Icons.Default.GridView,
                contentDescription = if (isGrid) "Switch to list view" else "Switch to grid view",
                onClick = { viewModel.toggleGridView() },
                size = 48.dp,
                testTag = "toggle_view_mode_button"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryFilter.entries.forEach { f ->
                val label = when (f) {
                    GalleryFilter.ALL -> "All"
                    GalleryFilter.PHOTOS -> "Photos"
                    GalleryFilter.VIDEOS -> "Videos"
                    GalleryFilter.FAVORITES -> "Favorites"
                    GalleryFilter.TRASH -> "Trash"
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.setGalleryFilter(f) },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        this.contentDescription = "Filter by $label"
                    }
                )
            }
        }

        // Multi-select Action Bar
        if (selectedUris.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedUris.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (filter == GalleryFilter.TRASH) {
                            IconButton(
                                onClick = {
                                    val selected = filteredMedia.filter { selectedUris.contains(it.uri.toString()) }
                                    viewModel.restoreFromTrash(selected)
                                }
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore selected items")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Permanently delete selected items")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val selected = filteredMedia.filter { selectedUris.contains(it.uri.toString()) }
                                    val uris = ArrayList(selected.map { it.uri })
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share media"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share selected items")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Move selected items to trash")
                            }
                        }
                        IconButton(onClick = { viewModel.clearMediaSelection() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancel selection")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Empty State
        if (filteredMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (filter == GalleryFilter.TRASH) "Trash is empty" else "No media found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Captured photos and videos will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Content Grid or List
            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMedia, key = { it.uri.toString() }) { item ->
                        val isSelected = selectedUris.contains(item.uri.toString())
                        MediaGridTile(
                            item = item,
                            isSelected = isSelected,
                            onItemClick = {
                                if (selectedUris.isNotEmpty()) {
                                    viewModel.toggleMediaSelection(item)
                                } else {
                                    viewModel.openMediaViewer(item)
                                }
                            },
                            onLongClick = { viewModel.toggleMediaSelection(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMedia, key = { it.uri.toString() }) { item ->
                        val isSelected = selectedUris.contains(item.uri.toString())
                        MediaListRow(
                            item = item,
                            isSelected = isSelected,
                            onItemClick = {
                                if (selectedUris.isNotEmpty()) {
                                    viewModel.toggleMediaSelection(item)
                                } else {
                                    viewModel.openMediaViewer(item)
                                }
                            },
                            onLongClick = { viewModel.toggleMediaSelection(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        val count = selectedUris.size
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (filter == GalleryFilter.TRASH) "Permanently Delete" else "Move to Trash") },
            text = {
                Text(
                    if (filter == GalleryFilter.TRASH) {
                        "Are you sure you want to permanently delete $count items? This cannot be undone."
                    } else {
                        "Move $count items to recently deleted trash?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = filteredMedia.filter { selectedUris.contains(it.uri.toString()) }
                        if (filter == GalleryFilter.TRASH) {
                            viewModel.deletePermanently(selected)
                        } else {
                            viewModel.moveToTrash(selected)
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridTile(
    item: MediaItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(item.dateModifiedMs))
    val accessibilityLabel = buildString {
        append(if (item.isVideo) "Video " else "Photo ")
        append(item.displayName)
        append(", date $dateStr")
        if (isSelected) append(", selected")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onItemClick, onLongClick = onLongClick)
            .semantics {
                this.contentDescription = accessibilityLabel
                this.role = Role.Button
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video indicator badge
            if (item.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        val mins = item.durationSeconds / 60
                        val secs = item.durationSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListRow(
    item: MediaItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(item.dateModifiedMs))
    val sizeMb = String.format(Locale.US, "%.1f MB", item.sizeBytes / (1024f * 1024f))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onItemClick, onLongClick = onLongClick)
            .semantics {
                this.contentDescription = "${item.displayName}, $dateStr, $sizeMb"
                this.role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.isVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr - $sizeMb",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (item.isFavorite) "Remove favorite" else "Add to favorites",
                    tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
