package com.luminens.android.presentation.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.FilmPreset
import com.luminens.android.data.model.FilmPresetsData
import com.luminens.android.data.model.GrainSize
import com.luminens.android.data.repository.GenerationRepository.PhotoCritiqueResult
import com.luminens.android.data.repository.GenerationRepository
import com.luminens.android.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.min
import javax.inject.Inject

data class EditorState(
    val originalUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val currentBitmap: Bitmap? = null,
    val selectedPreset: FilmPreset? = null,
    val brightness: Float = 0f,         // -1..1 (GPUImage: -1..1)
    val contrast: Float = 1f,           // 0..4 (GPUImage: 0..4)
    val saturation: Float = 1f,         // 0..2 (GPUImage: 0..2)
    val sharpen: Float = 0f,            // 0..4
    val hue: Float = 0f,                // 0..360
    val sepia: Float = 0f,              // 0..1
    val warmth: Float = 0f,             // -50..50
    val fade: Float = 0f,               // 0..100
    val shadows: Float = 0f,            // -25..25
    val highlights: Float = 0f,         // -25..25
    val grain: Float = 0f,              // 0..100
    val grainSize: GrainSize = GrainSize.FINE,
    val currentTab: EditorTab = EditorTab.FILM,
    val magicPreviewUrl: String? = null,
    val magicError: String? = null,
    val isMagicGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: String? = null,
    val isCritiqueLoading: Boolean = false,
    val critiqueResult: PhotoCritiqueResult? = null,
    val critiqueError: String? = null,
)

enum class EditorTab { FILM, FINE_TUNE }

data class SmartRetouchOptions(
    val enhanceLighting: Boolean = true,
    val cleanBackground: Boolean = false,
    val blurBackground: Boolean = false,
    val neutralBackground: Boolean = false,
    val keepOriginal: Boolean = true,
    val restoreOldPhoto: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    companion object {
        private const val MAGIC_TIMEOUT_MS = 60_000L
        private const val MAGIC_MAX_ATTEMPTS = 2
        private const val SLIDER_DEBOUNCE_MS = 140L
    }

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Undo stack of filter states (limited to 10 entries)
    private val history = ArrayDeque<EditorState>()
    private var redoStack = ArrayDeque<EditorState>()
    private var rerenderJob: Job? = null
    private var lastRenderSignature: String? = null

    fun loadPhoto(uri: Uri, context: Context) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val stream = when (uri.scheme?.lowercase()) {
                        "content", "file" -> context.contentResolver.openInputStream(uri)
                        else -> java.net.URL(uri.toString()).openStream()
                    }
                    stream?.use { android.graphics.BitmapFactory.decodeStream(it) }
                } catch (e: Exception) {
                    null
                }
            } ?: return@launch

            _state.value = EditorState(
                originalUri = uri,
                originalBitmap = bitmap,
                currentBitmap = bitmap,
            )
            lastRenderSignature = null
            history.clear()
            redoStack.clear()
        }
    }

    fun selectPreset(preset: FilmPreset, context: Context) {
        pushHistory()
        val updated = _state.value.copy(
            selectedPreset = preset,
            // CSS-like preset params are expressed as percentages where 100 is neutral.
            brightness = (preset.brightness - 100) / 100f,
            contrast = preset.contrast / 100f,
            saturation = preset.saturation / 100f,
            hue = preset.hueRotate.toFloat(),
            sepia = preset.sepia.toFloat().coerceIn(0f, 1f),
            warmth = preset.warmth.toFloat().coerceIn(-50f, 50f),
            fade = preset.fade.toFloat().coerceIn(0f, 100f),
            shadows = preset.shadows.toFloat().coerceIn(-25f, 25f),
            highlights = preset.highlights.toFloat().coerceIn(-25f, 25f),
            grain = preset.grain.toFloat().coerceIn(0f, 100f),
            grainSize = preset.grainSize,
        )
        _state.value = updated
        scheduleRerender(context)
    }

    fun setBrightness(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(brightness = v); scheduleRerender(context, SLIDER_DEBOUNCE_MS)
    }
    fun setContrast(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(contrast = v); scheduleRerender(context, SLIDER_DEBOUNCE_MS)
    }
    fun setSaturation(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(saturation = v); scheduleRerender(context, SLIDER_DEBOUNCE_MS)
    }
    fun setSharpen(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(sharpen = v); scheduleRerender(context, SLIDER_DEBOUNCE_MS)
    }
    fun setHue(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(hue = v); scheduleRerender(context, SLIDER_DEBOUNCE_MS)
    }

    fun setTab(tab: EditorTab) { _state.value = _state.value.copy(currentTab = tab) }

    fun undo(context: Context) {
        if (history.isEmpty()) return
        redoStack.addFirst(_state.value)
        _state.value = history.removeLast()
        scheduleRerender(context)
    }

    fun redo(context: Context) {
        if (redoStack.isEmpty()) return
        history.addLast(_state.value)
        _state.value = redoStack.removeFirst()
        scheduleRerender(context)
    }

    fun savePhoto(photoId: String, context: Context, onSuccess: () -> Unit) {
        val bitmap = _state.value.currentBitmap ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, saveError = null, saveSuccess = null)
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    val f = java.io.File(context.cacheDir, "edited_${System.currentTimeMillis()}.jpg")
                    f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    f
                }
                photoRepository.updatePhotoFile(photoId, file)
            }.onSuccess { updated ->
                if (updated.url.isBlank()) error("URL foto non disponibile dopo il salvataggio")
                _state.value = _state.value.copy(saveSuccess = "Foto salvata correttamente")
                onSuccess()
            }
             .onFailure { _state.value = _state.value.copy(saveError = it.message) }
            _state.value = _state.value.copy(isSaving = false)
        }
    }

    fun clearSaveStatus() {
        _state.value = _state.value.copy(saveError = null, saveSuccess = null)
    }

    fun generateMagicAi(prompt: String, aspectRatio: String) {
        val bitmap = _state.value.currentBitmap ?: return
        if (prompt.isBlank()) {
            _state.value = _state.value.copy(magicError = "Prompt required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isMagicGenerating = true, magicError = null, magicPreviewUrl = null)
            runCatching {
                val imageDataUrl = withContext(Dispatchers.Default) {
                    bitmapToDataUrl(bitmap)
                }
                requestMagicAiWithRetry(prompt = prompt, aspectRatio = aspectRatio, imageDataUrl = imageDataUrl)
            }.onSuccess { editedUrl ->
                _state.value = _state.value.copy(magicPreviewUrl = editedUrl)
            }.onFailure {
                val raw = it.message.orEmpty()
                val message = when {
                    it is TimeoutCancellationException -> "Magic AI timed out. Please retry shortly."
                    raw.startsWith("REFUSAL:") -> raw.removePrefix("REFUSAL:").trim()
                    raw.isNotBlank() -> raw
                    else -> "Magic AI error"
                }
                _state.value = _state.value.copy(magicError = message)
            }
            _state.value = _state.value.copy(isMagicGenerating = false)
        }
    }

    fun generateSmartRetouch(options: SmartRetouchOptions, aspectRatio: String = "1:1") {
        val bitmap = _state.value.currentBitmap ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isMagicGenerating = true, magicError = null, magicPreviewUrl = null)
            runCatching {
                val imageDataUrl = withContext(Dispatchers.Default) {
                    bitmapToDataUrl(bitmap)
                }
                val prompt = buildSmartRetouchPrompt(options)
                requestMagicAiWithRetry(
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    imageDataUrl = imageDataUrl,
                    functionName = if (options.restoreOldPhoto) "restore-photo" else "edit-with-ai",
                )
            }.onSuccess { editedUrl ->
                _state.value = _state.value.copy(magicPreviewUrl = editedUrl)
            }.onFailure {
                val raw = it.message.orEmpty()
                val message = when {
                    it is TimeoutCancellationException -> "Smart Retouch in timeout. Riprova tra poco."
                    raw.startsWith("REFUSAL:") -> raw.removePrefix("REFUSAL:").trim()
                    raw.isNotBlank() -> raw
                    else -> "Errore Smart Retouch"
                }
                _state.value = _state.value.copy(magicError = message)
            }
            _state.value = _state.value.copy(isMagicGenerating = false)
        }
    }

    fun applyMagicPreview(context: Context) {
        val preview = _state.value.magicPreviewUrl ?: return
        loadPhoto(Uri.parse(preview), context)
    }

    fun analyzePhotoCritique() {
        val bitmap = _state.value.currentBitmap ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isCritiqueLoading = true,
                critiqueError = null,
                critiqueResult = null,
            )
            runCatching {
                val imageDataUrl = withContext(Dispatchers.Default) {
                    bitmapToDataUrl(bitmap)
                }
                generationRepository.critiquePhoto(imageDataUrl = imageDataUrl)
            }.onSuccess { result ->
                _state.value = _state.value.copy(critiqueResult = result)
            }.onFailure {
                _state.value = _state.value.copy(
                    critiqueError = it.message ?: "Errore analisi foto",
                )
            }
            _state.value = _state.value.copy(isCritiqueLoading = false)
        }
    }

    fun clearCritique() {
        _state.value = _state.value.copy(
            isCritiqueLoading = false,
            critiqueError = null,
            critiqueResult = null,
        )
    }

    fun clearMagicState() {
        _state.value = _state.value.copy(magicPreviewUrl = null, magicError = null, isMagicGenerating = false)
    }

    private fun pushHistory() {
        if (history.size >= 10) history.removeFirst()
        history.addLast(_state.value)
        redoStack.clear()
    }

    private fun scheduleRerender(context: Context, debounceMs: Long = 0L) {
        rerenderJob?.cancel()
        rerenderJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            rerender(context)
        }
    }

    private suspend fun rerender(context: Context) {
        val stateSnapshot = _state.value
        val original = stateSnapshot.originalUri ?: return
        val renderSignature = buildRenderSignature(stateSnapshot)
        if (renderSignature == lastRenderSignature) return

        val sourceBitmap = stateSnapshot.originalBitmap ?: withContext(Dispatchers.IO) {
            try {
                val stream = when (original.scheme?.lowercase()) {
                    "content", "file" -> context.contentResolver.openInputStream(original)
                    else -> java.net.URL(original.toString()).openStream()
                }
                stream?.use { android.graphics.BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        } ?: return

        val targetState = _state.value
        val filteredBitmap = withContext(Dispatchers.Default) {
            if (hasAdvancedPresetAdjustments(targetState)) {
                applyBasicFiltersCpu(sourceBitmap, targetState)
            } else {
                val gpuResult = runCatching { applyFilters(context, sourceBitmap, targetState) }.getOrNull()
                when {
                    gpuResult == null -> applyBasicFiltersCpu(sourceBitmap, targetState)
                    hasActiveAdjustments(targetState) && isBitmapLikelyUnchanged(sourceBitmap, gpuResult) -> {
                        applyBasicFiltersCpu(sourceBitmap, targetState)
                    }
                    else -> gpuResult
                }
            }
        }

        lastRenderSignature = renderSignature
        _state.value = _state.value.copy(
            originalBitmap = _state.value.originalBitmap ?: sourceBitmap,
            currentBitmap = filteredBitmap,
        )
    }

    private fun buildRenderSignature(state: EditorState): String = listOf(
        state.originalUri?.toString().orEmpty(),
        state.brightness,
        state.contrast,
        state.saturation,
        state.sharpen,
        state.hue,
        state.sepia,
        state.warmth,
        state.fade,
        state.shadows,
        state.highlights,
        state.grain,
        state.grainSize.name,
    ).joinToString("|")

    private fun applyFilters(context: Context, bitmap: Bitmap, state: EditorState): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(bitmap)
        val filters = GPUImageFilterGroup(
            mutableListOf(
                GPUImageBrightnessFilter(state.brightness),
                GPUImageContrastFilter(state.contrast),
                GPUImageSaturationFilter(state.saturation),
                GPUImageHueFilter(state.hue),
                GPUImageSharpenFilter(state.sharpen),
            )
        )
        gpuImage.setFilter(filters)
        return gpuImage.bitmapWithFilterApplied
    }

    private fun applyBasicFiltersCpu(source: Bitmap, state: EditorState): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val saturationMatrix = ColorMatrix().apply {
            setSaturation(state.saturation.coerceIn(0f, 2f))
        }

        val hueMatrix = ColorMatrix().apply {
            // Approximate hue rotation by rotating each RGB axis in color space.
            setRotate(0, state.hue)
            setRotate(1, state.hue)
            setRotate(2, state.hue)
        }

        val warmthNorm = (state.warmth.coerceIn(-50f, 50f) / 50f)
        val rScale = (1f + 0.10f * warmthNorm).coerceIn(0.8f, 1.2f)
        val bScale = (1f - 0.10f * warmthNorm).coerceIn(0.8f, 1.2f)
        val warmthMatrix = ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, bScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )

        val fadeNorm = state.fade.coerceIn(0f, 100f) / 100f
        val contrast = ((state.contrast + state.sharpen.coerceIn(0f, 4f) * 0.15f) * (1f - 0.35f * fadeNorm)).coerceIn(0f, 4.8f)
        val brightnessOffset = state.brightness.coerceIn(-1f, 1f) * 255f
        val translate = 128f * (1f - contrast) + brightnessOffset + (30f * fadeNorm)
        val contrastBrightnessMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )

        val sepiaAmount = state.sepia.coerceIn(0f, 1f)
        val invSepia = 1f - sepiaAmount
        val sepiaMatrix = ColorMatrix(
            floatArrayOf(
                (0.393f * sepiaAmount) + invSepia, 0.769f * sepiaAmount, 0.189f * sepiaAmount, 0f, 0f,
                0.349f * sepiaAmount, (0.686f * sepiaAmount) + invSepia, 0.168f * sepiaAmount, 0f, 0f,
                0.272f * sepiaAmount, 0.534f * sepiaAmount, (0.131f * sepiaAmount) + invSepia, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )

        saturationMatrix.postConcat(hueMatrix)
        saturationMatrix.postConcat(warmthMatrix)
        saturationMatrix.postConcat(sepiaMatrix)
        saturationMatrix.postConcat(contrastBrightnessMatrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(saturationMatrix)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)

        val shadowsNorm = state.shadows.coerceIn(-25f, 25f) / 25f
        val highlightsNorm = state.highlights.coerceIn(-25f, 25f) / 25f
        val grainNorm = state.grain.coerceIn(0f, 100f) / 100f
        if (abs(shadowsNorm) > 0.001f || abs(highlightsNorm) > 0.001f || grainNorm > 0.001f) {
            val block = when (state.grainSize) {
                GrainSize.FINE -> 1
                GrainSize.MEDIUM -> 2
                GrainSize.COARSE -> 3
            }
            val width = out.width
            val height = out.height
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val c = out.getPixel(x, y)
                    var r = (c shr 16) and 0xFF
                    var g = (c shr 8) and 0xFF
                    var b = c and 0xFF

                    val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    val shadowWeight = (1f - luma) * (1f - luma)
                    val highlightWeight = luma * luma
                    val shDelta = (shadowsNorm * 32f * shadowWeight)
                    val hiDelta = (highlightsNorm * 32f * highlightWeight)

                    r = (r + shDelta + hiDelta).toInt().coerceIn(0, 255)
                    g = (g + shDelta + hiDelta).toInt().coerceIn(0, 255)
                    b = (b + shDelta + hiDelta).toInt().coerceIn(0, 255)

                    if (grainNorm > 0f) {
                        val bx = x / block
                        val by = y / block
                        val hash = ((bx * 73856093) xor (by * 19349663)) and 0x7fffffff
                        val noise01 = (hash % 1000) / 1000f
                        val noise = ((noise01 * 2f) - 1f) * (18f * grainNorm)
                        r = (r + noise).toInt().coerceIn(0, 255)
                        g = (g + noise).toInt().coerceIn(0, 255)
                        b = (b + noise).toInt().coerceIn(0, 255)
                    }

                    val outColor = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    out.setPixel(x, y, outColor)
                }
            }
        }

        return out
    }

    private fun hasActiveAdjustments(state: EditorState): Boolean {
        return abs(state.brightness) > 0.001f ||
            abs(state.contrast - 1f) > 0.001f ||
            abs(state.saturation - 1f) > 0.001f ||
            abs(state.hue) > 0.001f ||
            abs(state.sharpen) > 0.001f
    }

    private fun hasAdvancedPresetAdjustments(state: EditorState): Boolean {
        return abs(state.sepia) > 0.001f ||
            abs(state.warmth) > 0.001f ||
            abs(state.fade) > 0.001f ||
            abs(state.shadows) > 0.001f ||
            abs(state.highlights) > 0.001f ||
            abs(state.grain) > 0.001f
    }

    private fun isBitmapLikelyUnchanged(a: Bitmap, b: Bitmap): Boolean {
        if (a.width != b.width || a.height != b.height) return false
        if (a.sameAs(b)) return true

        val w = a.width
        val h = a.height
        val samplePoints = arrayOf(
            0 to 0,
            (w / 2) to (h / 2),
            (w - 1).coerceAtLeast(0) to (h - 1).coerceAtLeast(0),
            (w / 3) to (h / 3),
            ((w * 2) / 3) to ((h * 2) / 3),
        )
        val size = min(samplePoints.size, 5)
        for (i in 0 until size) {
            val (x, y) = samplePoints[i]
            if (a.getPixel(x, y) != b.getPixel(x, y)) return false
        }
        return true
    }

    private fun bitmapToDataUrl(bitmap: Bitmap): String {
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        val bytes = out.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private suspend fun requestMagicAiWithRetry(
        prompt: String,
        aspectRatio: String,
        imageDataUrl: String,
        functionName: String = "edit-with-ai",
    ): String {
        var lastError: Throwable? = null
        repeat(MAGIC_MAX_ATTEMPTS) { attempt ->
            try {
                return withTimeout(MAGIC_TIMEOUT_MS) {
                    generationRepository.editWithAi(
                        prompt = prompt,
                        aspectRatio = aspectRatio,
                        imageDataUrl = imageDataUrl,
                        functionName = functionName,
                    )
                }
            } catch (e: Throwable) {
                lastError = e
                if (attempt < MAGIC_MAX_ATTEMPTS - 1) {
                    delay(800L)
                }
            }
        }
        throw (lastError ?: IllegalStateException("Magic AI request failed"))
    }

    private fun buildSmartRetouchPrompt(options: SmartRetouchOptions): String {
        val promptParts = mutableListOf(
            "Agisci come un ritoccatore fotografico professionista di alta moda.",
            "Migliora questa foto mantenendo identita, posa ed espressione del soggetto.",
        )

        if (options.restoreOldPhoto) {
            promptParts += "Ripristina foto datata: rimuovi graffi, polvere e pieghe; migliora nitidezza e dettagli del volto; ricostruisci colori realistici se sbiaditi."
        }
        if (options.enhanceLighting) {
            promptParts += "Applica una luce professionale morbida da studio e migliora bilanciamento del bianco e contrasto."
        }

        if (options.keepOriginal) {
            if (options.cleanBackground) {
                promptParts += "Mantieni lo sfondo originale ma puliscilo da elementi di disturbo."
            } else {
                promptParts += "Mantieni lo sfondo originale."
            }
        } else if (options.neutralBackground) {
            promptParts += "Sostituisci lo sfondo con uno sfondo neutro da studio, pulito e minimale."
        } else if (options.blurBackground) {
            promptParts += "Mantieni lo sfondo originale ma applica un bokeh naturale per enfatizzare il soggetto."
        }

        return promptParts.joinToString(" ")
    }
}
