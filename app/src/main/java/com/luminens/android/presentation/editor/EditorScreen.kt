package com.luminens.android.presentation.editor

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.luminens.android.R
import com.luminens.android.data.model.FilmPreset
import com.luminens.android.data.model.FilmPresetsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    photoId: String,
    photoUri: String,
    onBack: () -> Unit,
    onCropRequested: (Uri) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var magicDialogOpen by remember { mutableStateOf(false) }
    var magicPrompt by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("1:1") }

    LaunchedEffect(photoUri) {
        val uri = Uri.parse(photoUri)
        viewModel.loadPhoto(uri, context)
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.magicError) {
        state.magicError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { magicDialogOpen = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.magic_ai_title))
                    }
                    IconButton(onClick = { viewModel.undo(context) }) {
                        Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.undo))
                    }
                    IconButton(onClick = { viewModel.redo(context) }) {
                        Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.redo))
                    }
                    IconButton(
                        onClick = { viewModel.savePhoto(photoId, context) { onBack() } },
                        enabled = !state.isSaving,
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
            ) {
                TabRow(
                    selectedTabIndex = if (state.currentTab == EditorTab.FILM) 0 else 1,
                ) {
                    Tab(
                        selected = state.currentTab == EditorTab.FILM,
                        onClick = { viewModel.setTab(EditorTab.FILM) },
                        text = { Text(stringResource(R.string.editor_tab_film)) },
                    )
                    Tab(
                        selected = state.currentTab == EditorTab.FINE_TUNE,
                        onClick = { viewModel.setTab(EditorTab.FINE_TUNE) },
                        text = { Text(stringResource(R.string.editor_tab_finetune)) },
                    )
                }

                when (state.currentTab) {
                    EditorTab.FILM -> FilmTab(
                        presets = FilmPresetsData.all,
                        selectedPreset = state.selectedPreset,
                        onPresetSelected = { viewModel.selectPreset(it, context) },
                        onCropClick = {
                            state.originalUri?.let { onCropRequested(it) }
                        },
                    )
                    EditorTab.FINE_TUNE -> FineTuneTab(
                        brightness = state.brightness,
                        contrast = state.contrast,
                        saturation = state.saturation,
                        sharpen = state.sharpen,
                        onBrightnessChange = { viewModel.setBrightness(it, context) },
                        onContrastChange = { viewModel.setContrast(it, context) },
                        onSaturationChange = { viewModel.setSaturation(it, context) },
                        onSharpenChange = { viewModel.setSharpen(it, context) },
                    )
                }
            }
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            state.currentBitmap?.let { bmp ->
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } ?: CircularProgressIndicator()
        }

        if (magicDialogOpen) {
            MagicAiDialog(
                prompt = magicPrompt,
                aspectRatio = aspectRatio,
                isGenerating = state.isMagicGenerating,
                previewUrl = state.magicPreviewUrl,
                onPromptChange = { magicPrompt = it },
                onAspectRatioChange = { aspectRatio = it },
                onGenerate = { viewModel.generateMagicAi(magicPrompt, aspectRatio) },
                onApply = {
                    viewModel.applyMagicPreview(context)
                    viewModel.clearMagicState()
                    magicDialogOpen = false
                },
                onDismiss = {
                    viewModel.clearMagicState()
                    magicDialogOpen = false
                },
                onFeaturePrompt = { magicPrompt = it },
            )
        }
    }
}

@Composable
private fun MagicAiDialog(
    prompt: String,
    aspectRatio: String,
    isGenerating: Boolean,
    previewUrl: String?,
    onPromptChange: (String) -> Unit,
    onAspectRatioChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onFeaturePrompt: (String) -> Unit,
) {
    val features = listOf(
        stringResource(R.string.magic_ai_feature_remove_bg) to stringResource(R.string.magic_ai_feature_remove_bg_prompt),
        stringResource(R.string.magic_ai_feature_studio_light) to stringResource(R.string.magic_ai_feature_studio_light_prompt),
        stringResource(R.string.magic_ai_feature_enhance) to stringResource(R.string.magic_ai_feature_enhance_prompt),
        stringResource(R.string.magic_ai_feature_bokeh) to stringResource(R.string.magic_ai_feature_bokeh_prompt),
    )
    val ratios = listOf("1:1", "4:5", "3:4", "9:16")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.magic_ai_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(features, key = { it.first }) { (label, featurePrompt) ->
                        OutlinedButton(onClick = { onFeaturePrompt(featurePrompt) }) {
                            Text(label)
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = { Text(stringResource(R.string.magic_ai_prompt_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ratios, key = { it }) { ratio ->
                        OutlinedButton(onClick = { onAspectRatioChange(ratio) }) {
                            Text(if (aspectRatio == ratio) "* $ratio" else ratio)
                        }
                    }
                }

                if (previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        },
        confirmButton = {
            if (previewUrl == null) {
                Button(onClick = onGenerate, enabled = !isGenerating && prompt.isNotBlank()) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.magic_ai_generate))
                    }
                }
            } else {
                Button(onClick = onApply) {
                    Text(stringResource(R.string.magic_ai_apply))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun FilmTab(
    presets: List<FilmPreset>,
    selectedPreset: FilmPreset?,
    onPresetSelected: (FilmPreset) -> Unit,
    onCropClick: () -> Unit,
) {
    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(presets, key = { it.id }) { preset ->
                val isSelected = preset.id == selectedPreset?.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(64.dp)
                        .clickable { onPresetSelected(preset) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(8.dp),
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = preset.name.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Crop button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onCropClick) {
                Icon(Icons.Default.Crop, contentDescription = stringResource(R.string.crop),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun FineTuneTab(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpen: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onSharpenChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SliderRow(
            label = stringResource(R.string.editor_brightness),
            value = brightness,
            range = -1f..1f,
            onValueChange = onBrightnessChange,
        )
        SliderRow(
            label = stringResource(R.string.editor_contrast),
            value = contrast,
            range = 0f..4f,
            onValueChange = onContrastChange,
        )
        SliderRow(
            label = stringResource(R.string.editor_saturation),
            value = saturation,
            range = 0f..2f,
            onValueChange = onSaturationChange,
        )
        SliderRow(
            label = stringResource(R.string.editor_sharpen),
            value = sharpen,
            range = 0f..4f,
            onValueChange = onSharpenChange,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
