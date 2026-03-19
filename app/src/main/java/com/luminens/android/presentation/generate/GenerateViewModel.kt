package com.luminens.android.presentation.generate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.GenerationParams
import com.luminens.android.data.model.Photo
import com.luminens.android.data.model.PhotoStyle
import com.luminens.android.data.repository.GenerationRepository
import com.luminens.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

enum class GenerateStep { UPLOAD, STYLE, SETTINGS, GENERATING, RESULTS }

@HiltViewModel
class GenerateViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val generationRepository: GenerationRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    companion object {
        private const val MAX_REFERENCE_PHOTOS = 4
    }

    private val _step = MutableStateFlow(GenerateStep.UPLOAD)
    val step: StateFlow<GenerateStep> = _step.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    private val _selectedStyle = MutableStateFlow<PhotoStyle?>(null)
    val selectedStyle: StateFlow<PhotoStyle?> = _selectedStyle.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _generationParams = MutableStateFlow(GenerationParams())
    val generationParams: StateFlow<GenerationParams> = _generationParams.asStateFlow()

    private val _generatedPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val generatedPhotos: StateFlow<List<Photo>> = _generatedPhotos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _availableCredits = MutableStateFlow(0)
    val availableCredits: StateFlow<Int> = _availableCredits.asStateFlow()

    private val _maxShots = MutableStateFlow(4)
    val maxShots: StateFlow<Int> = _maxShots.asStateFlow()

    private val _enhancedPrompt = MutableStateFlow<String?>(null)
    val enhancedPrompt: StateFlow<String?> = _enhancedPrompt.asStateFlow()

    private val _generationStatus = MutableStateFlow<String?>(null)
    val generationStatus: StateFlow<String?> = _generationStatus.asStateFlow()

    init {
        loadCredits()
    }

    private fun loadCredits() {
        viewModelScope.launch {
            runCatching { profileRepository.getProfile() }
                .onSuccess {
                    it?.let { p ->
                        _availableCredits.value = p.creditsRemaining
                        _maxShots.value = p.maxShots
                        _generationParams.update { params ->
                            params.copy(numShots = params.numShots.coerceIn(1, p.maxShots))
                        }
                    }
                }
        }
    }

    fun onImageSelected(uri: Uri) {
        _selectedImageUris.value = (_selectedImageUris.value + uri).distinct().take(MAX_REFERENCE_PHOTOS)
        _step.value = GenerateStep.STYLE
    }

    fun onImagesSelected(uris: List<Uri>) {
        _selectedImageUris.value = uris.distinct().take(MAX_REFERENCE_PHOTOS)
        _step.value = GenerateStep.STYLE
    }

    fun onStyleSelected(style: PhotoStyle) {
        _selectedStyle.value = style
        val category = if (style.category.name.equals("KIDS", ignoreCase = true)) "kids" else "adults"
        val defaultSetting = if (category == "kids") "kids-studio" else "studio"
        _generationParams.update { params ->
            params.copy(
                category = category,
                setting = if (params.setting.isBlank()) defaultSetting else params.setting,
            )
        }
        _step.value = GenerateStep.SETTINGS
    }

    fun onPromptChanged(text: String) {
        _prompt.value = text
        _enhancedPrompt.value = null
    }

    fun onParamsChanged(params: GenerationParams) {
        _generationParams.value = params.copy(numShots = params.numShots.coerceIn(1, _maxShots.value))
    }

    fun enhancePrompt() {
        val style = _selectedStyle.value ?: return
        val params = _generationParams.value
        viewModelScope.launch {
            _isLoading.value = true
            _generationStatus.value = "Miglioro il prompt..."
            runCatching {
                retryWithTimeout(
                    timeoutMs = 30_000,
                    maxAttempts = 3,
                ) {
                    generationRepository.enhancePrompt(
                        style = style.id,
                        setting = params.setting.ifBlank { "outdoor" },
                        photoCount = params.numShots,
                        userPrompt = _prompt.value.takeIf { it.isNotBlank() },
                    )
                }
            }
                .onSuccess { _enhancedPrompt.value = it; _prompt.value = it }
                .onFailure {
                    _error.value = when (it) {
                        is TimeoutCancellationException -> "Timeout nel miglioramento prompt. Riprova."
                        else -> it.message
                    }
                }
            _isLoading.value = false
            _generationStatus.value = null
        }
    }

    fun generatePhotos() {
        val uris = _selectedImageUris.value.take(MAX_REFERENCE_PHOTOS)
        if (uris.isEmpty()) {
            _error.value = "Carica almeno una foto di riferimento"
            return
        }
        val style = _selectedStyle.value ?: return
        val params = _generationParams.value
        viewModelScope.launch {
            _isLoading.value = true
            _step.value = GenerateStep.GENERATING
            _error.value = null
            _generationStatus.value = "Preparo l'immagine..."
            runCatching {
                val refPaths = uris.mapIndexed { index, uri ->
                    _generationStatus.value = "Carico foto di riferimento ${index + 1}/${uris.size}..."
                    val bytes = withContext(Dispatchers.IO) {
                        appContext.contentResolver.openInputStream(uri)?.readBytes()
                            ?: error("Cannot read image")
                    }
                    generationRepository.uploadReferencePhoto(bytes)
                }

                _generationStatus.value = "Genero ${params.numShots} foto..."
                val urls = retryWithTimeout(
                    timeoutMs = 90_000,
                    maxAttempts = 2,
                ) {
                    generationRepository.generatePhotos(
                        imagePaths = refPaths,
                        style = style.id,
                        setting = params.setting.ifBlank { if (params.category == "kids") "kids-studio" else "studio" },
                        subSetting = params.subSetting,
                        category = params.category,
                        shotCount = params.numShots,
                        aspectRatio = params.aspectRatio,
                        resolution = params.resolution,
                        customPrompt = _prompt.value.takeIf { it.isNotBlank() },
                    )
                }
                _generationStatus.value = "Finalizzo i risultati..."
                urls.map { url -> Photo(id = UUID.randomUUID().toString(), url = url, isGenerated = true) }
            }.onSuccess { photos ->
                _generatedPhotos.value = photos
                _step.value = GenerateStep.RESULTS
                loadCredits()
            }.onFailure {
                _error.value = when (it) {
                    is TimeoutCancellationException -> "Generazione troppo lenta. Riprova tra poco."
                    else -> extractEdgeError(it.message)
                }
                _step.value = GenerateStep.SETTINGS
            }
            _isLoading.value = false
            _generationStatus.value = null
        }
    }

    fun clearError() { _error.value = null }

    fun reset() {
        _step.value = GenerateStep.UPLOAD
        _selectedImageUris.value = emptyList()
        _selectedStyle.value = null
        _prompt.value = ""
        _generationParams.value = GenerationParams()
        _generatedPhotos.value = emptyList()
        _error.value = null
        _enhancedPrompt.value = null
    }

    fun goBack() {
        when (_step.value) {
            GenerateStep.STYLE -> _step.value = GenerateStep.UPLOAD
            GenerateStep.SETTINGS -> _step.value = GenerateStep.STYLE
            else -> {}
        }
    }

    private suspend fun <T> retryWithTimeout(
        timeoutMs: Long,
        maxAttempts: Int,
        block: suspend () -> T,
    ): T {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                return withTimeout(timeoutMs) { block() }
            } catch (e: Throwable) {
                lastError = e
                if (attempt < maxAttempts - 1) {
                    delay(700L * (attempt + 1))
                }
            }
        }
        throw (lastError ?: IllegalStateException("Operazione fallita"))
    }

    private fun extractEdgeError(message: String?): String {
        val raw = message.orEmpty()
        val regex = Regex("\\\"error\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        val extracted = regex.find(raw)?.groupValues?.getOrNull(1)
        return extracted ?: raw.ifBlank { "Errore durante la generazione" }
    }
}
