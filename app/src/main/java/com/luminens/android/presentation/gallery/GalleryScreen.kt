package com.luminens.android.presentation.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.luminens.android.R
import com.luminens.android.data.model.Photo
import com.luminens.android.presentation.components.ConfirmDeleteDialog
import com.luminens.android.presentation.components.EmptyState
import com.luminens.android.presentation.theme.OnSurfaceVariant
import com.luminens.android.presentation.theme.SurfaceBorder
import com.luminens.android.presentation.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onPhotoClick: (String) -> Unit,
    onEditPhoto: (String, String) -> Unit,
    onCartClick: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedIds by viewModel.selectedPhotoIds.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_photo_confirm),
            onConfirm = {
                photoToDelete?.let { p ->
                    if (selectedIds.isNotEmpty()) viewModel.deleteSelectedPhotos()
                    else viewModel.deletePhoto(p.id, p.storagePath)
                }
                showDeleteDialog = false
                photoToDelete = null
            },
            onDismiss = { showDeleteDialog = false; photoToDelete = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildAnnotatedString {
                            pushStyle(SpanStyle(color = Color.White))
                            append("Lumi")
                            pop()
                            pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                            append("nens")
                            pop()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = {
                            showDeleteDialog = true
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(onClick = onCartClick) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = stringResource(R.string.cart),
                            tint = OnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceElevated),
            )
        },
    ) { padding ->
        when {
            isLoading && photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            photos.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = stringResource(R.string.gallery_empty),
                )
            }
            else -> {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategory == "adults",
                            onClick = { viewModel.setCategory("adults") },
                            label = { Text(stringResource(R.string.category_adults)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                            ),
                        )
                        FilterChip(
                            selected = selectedCategory == "kids",
                            onClick = { viewModel.setCategory("kids") },
                            label = { Text(stringResource(R.string.category_kids)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                            ),
                        )
                    }

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            PhotoGridItem(
                                photo = photo,
                                isSelected = photo.id in selectedIds,
                                isSelectionMode = selectedIds.isNotEmpty(),
                                onClick = {
                                    if (selectedIds.isNotEmpty()) viewModel.toggleSelection(photo.id)
                                    else onPhotoClick(photo.id)
                                },
                                onLongClick = { viewModel.toggleSelection(photo.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, SurfaceBorder.copy(alpha = 0.55f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.url)
                .size(520, 700)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f)),
                    )
                )
                .height(46.dp)
                .alpha(if (isSelectionMode) 0.92f else 1f),
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.TopEnd,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(28.dp),
                )
            }
        }
    }
}
