package com.luminens.android.presentation.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.luminens.android.R
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import com.luminens.android.presentation.components.ConfirmDeleteDialog

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    initialPhotoId: String,
    onBack: () -> Unit,
    onEdit: (String, String) -> Unit,
    onPrint: () -> Unit = {},
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0) { photos.size }
    LaunchedEffect(photos.isNotEmpty()) {
        if (photos.isNotEmpty()) {
            val idx = photos.indexOfFirst { it.id == initialPhotoId }.coerceAtLeast(0)
            pagerState.scrollToPage(idx)
        }
    }
    val currentPhoto = if (photos.isNotEmpty()) photos.getOrNull(pagerState.currentPage) else null
    var showBars by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && currentPhoto != null) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_photo_confirm),
            onConfirm = {
                viewModel.deletePhoto(currentPhoto.id, currentPhoto.storagePath)
                showDeleteDialog = false
                onBack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showBars,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            currentPhoto?.url?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_photo))
                        }
                        IconButton(onClick = {
                            currentPhoto?.url?.let { url ->
                                val filename = currentPhoto.storagePath?.substringAfterLast("/")
                                    ?: "photo_${System.currentTimeMillis()}.jpg"
                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                val request = DownloadManager.Request(android.net.Uri.parse(url))
                                    .setTitle(filename)
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Luminens/$filename")
                                dm.enqueue(request)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.download))
                        }
                        IconButton(onClick = onPrint) {
                            Icon(Icons.Default.Print, contentDescription = stringResource(R.string.print_order_title))
                        }
                        if (currentPhoto?.storagePath != null) {
                            IconButton(onClick = { onEdit(currentPhoto.id, currentPhoto.storagePath!!) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            }
        },
        containerColor = Color.Black,
    ) { padding ->
        if (photos.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showBars = !showBars },
            ) { page ->
                val photo = photos[page]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}
