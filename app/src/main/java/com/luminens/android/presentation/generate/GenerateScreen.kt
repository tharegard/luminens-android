package com.luminens.android.presentation.generate

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.luminens.android.R
import com.luminens.android.data.model.GenerationParams
import com.luminens.android.data.model.PhotoStylesData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onNavigateToCamera: () -> Unit,
    onGenerationComplete: () -> Unit,
    onNavigateToBuyCredits: () -> Unit,
    capturedImageUri: String? = null,
    onCapturedImageConsumed: () -> Unit = {},
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsState()
    val selectedImageUris by viewModel.selectedImageUris.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val params by viewModel.generationParams.collectAsState()
    val generatedPhotos by viewModel.generatedPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val credits by viewModel.availableCredits.collectAsState()
    val maxShots by viewModel.maxShots.collectAsState()
    val generationStatus by viewModel.generationStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onImagesSelected(uris)
    }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    LaunchedEffect(step) {
        if (step == GenerateStep.RESULTS) onGenerationComplete()
    }

    LaunchedEffect(capturedImageUri) {
        val uri = capturedImageUri ?: return@LaunchedEffect
        viewModel.onImageSelected(Uri.parse(uri))
        onCapturedImageConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = when (step) {
                        GenerateStep.UPLOAD -> stringResource(R.string.generate_step_upload)
                        GenerateStep.STYLE -> stringResource(R.string.generate_step_style)
                        GenerateStep.SETTINGS -> stringResource(R.string.generate_step_settings)
                        GenerateStep.GENERATING -> stringResource(R.string.generating)
                        GenerateStep.RESULTS -> stringResource(R.string.generate_results)
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (step == GenerateStep.STYLE || step == GenerateStep.SETTINGS) {
                        IconButton(onClick = viewModel::goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { currentStep ->
            when (currentStep) {
                GenerateStep.UPLOAD -> UploadStep(
                    onPickImage = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onOpenCamera = onNavigateToCamera,
                    credits = credits,
                    onBuyCredits = onNavigateToBuyCredits,
                )
                GenerateStep.STYLE -> StyleStep(
                    onStyleSelected = viewModel::onStyleSelected,
                )
                GenerateStep.SETTINGS -> SettingsStep(
                    selectedImageUris = selectedImageUris,
                    prompt = prompt,
                    params = params,
                    maxShots = maxShots,
                    isLoading = isLoading,
                    onPromptChanged = viewModel::onPromptChanged,
                    onParamsChanged = viewModel::onParamsChanged,
                    onEnhancePrompt = viewModel::enhancePrompt,
                    onGenerate = viewModel::generatePhotos,
                    credits = credits,
                )
                GenerateStep.GENERATING -> GeneratingStep(status = generationStatus)
                GenerateStep.RESULTS -> ResultsStep(
                    photos = generatedPhotos,
                    onDone = { viewModel.reset(); onGenerationComplete() },
                )
            }
        }
    }
}

@Composable
private fun UploadStep(
    onPickImage: () -> Unit,
    onOpenCamera: () -> Unit,
    credits: Int,
    onBuyCredits: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.generate_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.generate_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.pick_from_gallery))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenCamera,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo))
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.credits_available, credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.buy_credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBuyCredits() },
            )
        }
    }
}

@Composable
private fun StyleStep(onStyleSelected: (com.luminens.android.data.model.PhotoStyle) -> Unit) {
    val styles = PhotoStylesData.all
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.choose_style),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(styles, key = { it.id }) { style ->
                var useFallback by remember(style.id) { mutableStateOf(false) }
                val fallbackUrl = remember(style.id) { "https://picsum.photos/seed/luminens-${style.id}/800/1000" }
                val model = if (useFallback) fallbackUrl else (style.previewUrl ?: fallbackUrl)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.85f)
                        .clickable { onStyleSelected(style) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = {
                                if (!useFallback) useFallback = true
                            },
                        )
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            Text(
                                text = style.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsStep(
    selectedImageUris: List<Uri>,
    prompt: String,
    params: GenerationParams,
    maxShots: Int,
    isLoading: Boolean,
    onPromptChanged: (String) -> Unit,
    onParamsChanged: (GenerationParams) -> Unit,
    onEnhancePrompt: () -> Unit,
    onGenerate: () -> Unit,
    credits: Int,
) {
    val subSettingsBySetting = mapOf(
        "urban" to listOf("rome", "paris", "new-york", "tokyo", "seoul"),
        "nature" to listOf("forest", "mountain", "jungle", "desert"),
        "luxury" to listOf("hotel-lobby", "rooftop-bar", "luxury-pool", "penthouse"),
    )
    val settings = if (params.category == "kids") {
        listOf("kids-studio", "playroom", "playground", "classroom")
    } else {
        listOf("studio", "minimal-desk", "coworking", "urban", "nature", "home")
    }
    val ratios = listOf("1:1", "4:5", "3:4", "9:16", "16:9")
    val resolutions = listOf("1K", "2K")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selectedImageUris.isNotEmpty()) {
            Text(
                stringResource(R.string.add_reference_photos),
                style = MaterialTheme.typography.labelMedium,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedImageUris, key = { it.toString() }) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .width(100.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            label = { Text(stringResource(R.string.prompt_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            trailingIcon = {
                if (!isLoading) {
                    IconButton(onClick = onEnhancePrompt) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.enhance_prompt))
                    }
                }
            },
        )

        Text(
            stringResource(R.string.select_setting),
            style = MaterialTheme.typography.labelMedium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings, key = { it }) { settingId ->
                val selected = params.setting == settingId
                OutlinedButton(
                    onClick = { onParamsChanged(params.copy(setting = settingId, subSetting = null)) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Text(settingId.replace('-', ' ').replaceFirstChar { c -> c.uppercase() })
                }
            }
        }

        val subSettings = subSettingsBySetting[params.setting].orEmpty()
        if (subSettings.isNotEmpty()) {
            Text(
                stringResource(R.string.select_setting),
                style = MaterialTheme.typography.labelMedium,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subSettings, key = { it }) { subSettingId ->
                    val selected = params.subSetting == subSettingId
                    OutlinedButton(
                        onClick = { onParamsChanged(params.copy(subSetting = subSettingId)) },
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Text(subSettingId.replace('-', ' ').replaceFirstChar { c -> c.uppercase() })
                    }
                }
            }
        }

        Text(
            stringResource(R.string.select_aspect_ratio),
            style = MaterialTheme.typography.labelMedium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ratios, key = { it }) { ratio ->
                val selected = params.aspectRatio == ratio
                OutlinedButton(
                    onClick = { onParamsChanged(params.copy(aspectRatio = ratio)) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Text(ratio)
                }
            }
        }

        Text(
            stringResource(R.string.select_resolution),
            style = MaterialTheme.typography.labelMedium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(resolutions, key = { it }) { resolution ->
                val selected = params.resolution == resolution
                OutlinedButton(
                    onClick = { onParamsChanged(params.copy(resolution = resolution)) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Text(resolution)
                }
            }
        }

        Text(
            stringResource(R.string.num_shots_label, params.numShots),
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = params.numShots.toFloat(),
            onValueChange = { onParamsChanged(params.copy(numShots = it.toInt())) },
            valueRange = 1f..maxShots.toFloat(),
            steps = (maxShots - 2).coerceAtLeast(0),
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isLoading && credits >= params.numShots && params.setting.isNotBlank(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.generate_button, params.numShots))
            }
        }
    }
}

@Composable
private fun GeneratingStep(status: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.generating_wait),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (!status.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
    }
}

@Composable
private fun ResultsStep(
    photos: List<com.luminens.android.data.model.Photo>,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(photos, key = { it.id }) { photo ->
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) { Text(stringResource(R.string.done)) }
    }
}
