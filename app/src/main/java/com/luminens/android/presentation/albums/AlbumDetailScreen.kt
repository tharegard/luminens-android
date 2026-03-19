package com.luminens.android.presentation.albums

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import coil3.compose.AsyncImage
import com.luminens.android.R
import com.luminens.android.data.model.Photo
import com.luminens.android.presentation.components.ConfirmDeleteDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onEditPhoto: (String, String) -> Unit,
    onPrintAlbum: () -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsState()
    val photos by viewModel.albumPhotos.collectAsState()
    val availablePhotos by viewModel.availablePhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val album = albums.firstOrNull { it.id == albumId }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showAddPhotosDialog by remember { mutableStateOf(false) }
    var selectedToAdd by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(albumId) {
        viewModel.loadAlbumPhotos(albumId)
        viewModel.loadAvailablePhotosForAlbum(albumId)
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_album_confirm),
            onConfirm = { viewModel.deleteAlbum(albumId); onBack() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showShareDialog && album != null) {
        val shareUrl = buildAlbumShareUrl(albumId)
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(R.string.share_album)) },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (album.isPublic) stringResource(R.string.album_public) else stringResource(R.string.album_private))
                        Switch(
                            checked = album.isPublic,
                            onCheckedChange = { viewModel.toggleAlbumPublic(albumId, album.isPublic) },
                        )
                    }
                    if (album.isPublic) {
                        Text(
                            shareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                if (album.isPublic) {
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(shareUrl))
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_copied)) }
                        showShareDialog = false
                    }) {
                        Text(stringResource(R.string.copy_link))
                    }
                }
            },
            dismissButton = {
                if (album.isPublic) {
                    TextButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_album)))
                        showShareDialog = false
                    }) {
                        Text(stringResource(R.string.share_album))
                    }
                } else {
                    TextButton(onClick = { showShareDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    if (showAddPhotosDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddPhotosDialog = false
                selectedToAdd = emptySet()
            },
            title = { Text(stringResource(R.string.add_photos)) },
            text = {
                if (availablePhotos.isEmpty()) {
                    Text(stringResource(R.string.gallery_empty))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availablePhotos, key = { it.id }) { photo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            selectedToAdd = if (photo.id in selectedToAdd) {
                                                selectedToAdd - photo.id
                                            } else {
                                                selectedToAdd + photo.id
                                            }
                                        },
                                        onLongClick = {},
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked = photo.id in selectedToAdd,
                                    onCheckedChange = { checked ->
                                        selectedToAdd = if (checked) selectedToAdd + photo.id else selectedToAdd - photo.id
                                    },
                                )
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.25f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addPhotosToAlbum(albumId, selectedToAdd.toList())
                        showAddPhotosDialog = false
                        selectedToAdd = emptySet()
                    },
                    enabled = selectedToAdd.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPhotosDialog = false
                    selectedToAdd = emptySet()
                }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(album?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPhotosDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_photos))
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_album))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_album))
                    }
                },
            )
        }
    ) { padding ->
        when {
            isLoading && photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(photos.size, key = { photos[it].id }) { index ->
                        val photo = photos[index]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .combinedClickable(
                                    onClick = {
                                        photo.storagePath?.let { onEditPhoto(photo.id, it) }
                                    }
                                ),
                        ) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            IconButton(
                                onClick = { viewModel.removePhotoFromAlbum(albumId, photo.id) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircle,
                                    contentDescription = stringResource(R.string.remove_from_album),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val reordered = photos.map { it.id }.toMutableList()
                                            val item = reordered.removeAt(index)
                                            reordered.add(index - 1, item)
                                            viewModel.reorderAlbumPhotos(albumId, reordered)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.offset(x = (-6).dp),
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBackIosNew,
                                        contentDescription = "Move previous",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index < photos.lastIndex) {
                                            val reordered = photos.map { it.id }.toMutableList()
                                            val item = reordered.removeAt(index)
                                            reordered.add(index + 1, item)
                                            viewModel.reorderAlbumPhotos(albumId, reordered)
                                        }
                                    },
                                    enabled = index < photos.lastIndex,
                                    modifier = Modifier.offset(x = 6.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Move next",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildAlbumShareUrl(albumId: String): String = "https://luminens.com/share/$albumId"
