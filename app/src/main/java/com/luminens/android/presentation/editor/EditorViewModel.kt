package com.luminens.android.presentation.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.FilmPreset
import com.luminens.android.data.model.FilmPresetsData
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class EditorState(
    val originalUri: Uri? = null,
    val currentBitmap: Bitmap? = null,
    val selectedPreset: FilmPreset? = null,
    val brightness: Float = 0f,         // -1..1 (GPUImage: -1..1)
    val contrast: Float = 1f,           // 0..4 (GPUImage: 0..4)
    val saturation: Float = 1f,         // 0..2 (GPUImage: 0..2)
    val sharpen: Float = 0f,            // 0..4
    val hue: Float = 0f,                // 0..360
    val currentTab: EditorTab = EditorTab.FILM,
    val magicPreviewUrl: String? = null,
    val magicError: String? = null,
    val isMagicGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
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
    }

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Undo stack of filter states (limited to 10 entries)
    private val history = ArrayDeque<EditorState>()
    private var redoStack = ArrayDeque<EditorState>()

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

            val filtered = withContext(Dispatchers.Default) {
                runCatching { applyFilters(context, bitmap, EditorState()) }.getOrElse { bitmap }
            }
            _state.value = EditorState(originalUri = uri, currentBitmap = filtered)
            history.clear()
            redoStack.clear()
        }
    }

    fun selectPreset(preset: FilmPreset, context: Context) {
        pushHistory()
        val updated = _state.value.copy(
            selectedPreset = preset,
            brightness = preset.brightness / 100f,
            contrast = 1f + preset.contrast / 200f,
            saturation = 1f + preset.saturation / 100f,
            hue = preset.hueRotate.toFloat(),
        )
        _state.value = updated
        rerender(context)
    }

    fun setBrightness(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(brightness = v); rerender(context)
    }
    fun setContrast(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(contrast = v); rerender(context)
    }
    fun setSaturation(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(saturation = v); rerender(context)
    }
    fun setSharpen(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(sharpen = v); rerender(context)
    }
    fun setHue(v: Float, context: Context) {
        pushHistory(); _state.value = _state.value.copy(hue = v); rerender(context)
    }

    fun setTab(tab: EditorTab) { _state.value = _state.value.copy(currentTab = tab) }

    fun undo(context: Context) {
        if (history.isEmpty()) return
        redoStack.addFirst(_state.value)
        _state.value = history.removeLast()
        rerender(context)
    }

    fun redo(context: Context) {
        if (redoStack.isEmpty()) return
        history.addLast(_state.value)
        _state.value = redoStack.removeFirst()
        rerender(context)
    }

    fun savePhoto(photoId: String, context: Context, onSuccess: () -> Unit) {
        val bitmap = _state.value.currentBitmap ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, saveError = null)
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    val f = java.io.File(context.cacheDir, "edited_${System.currentTimeMillis()}.jpg")
                    f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    f
                }
                photoRepository.updatePhotoFile(photoId, file)
            }.onSuccess { onSuccess() }
             .onFailure { _state.value = _state.value.copy(saveError = it.message) }
            _state.value = _state.value.copy(isSaving = false)
        }
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

    fun clearMagicState() {
        _state.value = _state.value.copy(magicPreviewUrl = null, magicError = null, isMagicGenerating = false)
    }

    private fun pushHistory() {
        if (history.size >= 10) history.removeFirst()
        history.addLast(_state.value)
        redoStack.clear()
    }

    private fun rerender(context: Context) {
        val original = _state.value.originalUri ?: return
        viewModelScope.launch {
            val originalBitmap = withContext(Dispatchers.IO) {
                try {
                    val stream = when (original.scheme?.lowercase()) {
                        "content", "file" -> context.contentResolver.openInputStream(original)
                        else -> java.net.URL(original.toString()).openStream()
                    }
                    stream?.use { android.graphics.BitmapFactory.decodeStream(it) }
                } catch (e: Exception) {
                    null
                }
            } ?: return@launch
            val filteredBitmap = withContext(Dispatchers.Default) {
                runCatching { applyFilters(context, originalBitmap, _state.value) }.getOrElse { originalBitmap }
            }
            _state.value = _state.value.copy(currentBitmap = filteredBitmap)
        }
    }

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
