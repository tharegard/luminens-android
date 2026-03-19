package com.luminens.android.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.Photo
import com.luminens.android.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    companion object {
        private const val CATEGORY_ADULTS = "adults"
        private const val CATEGORY_KIDS = "kids"
    }

    private var allPhotos: List<Photo> = emptyList()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CATEGORY_ADULTS)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<String>> = _selectedPhotoIds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isSelectionMode get() = _selectedPhotoIds.value.isNotEmpty()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { photoRepository.getPhotos() }
                .onSuccess {
                    allPhotos = it
                    applyCategoryFilter()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun setCategory(category: String) {
        if (category != CATEGORY_ADULTS && category != CATEGORY_KIDS) return
        _selectedCategory.value = category
        applyCategoryFilter()
    }

    fun ensureCategoryForPhoto(photoId: String) {
        if (photoId.isBlank()) return
        if (_photos.value.any { it.id == photoId }) return

        val target = allPhotos.firstOrNull { it.id == photoId } ?: return
        val targetCategory = target.positionData?.category
        val nextCategory = if (targetCategory == CATEGORY_KIDS) CATEGORY_KIDS else CATEGORY_ADULTS

        if (_selectedCategory.value != nextCategory) {
            _selectedCategory.value = nextCategory
            applyCategoryFilter()
        }
    }

    fun toggleSelection(photoId: String) {
        _selectedPhotoIds.value = _selectedPhotoIds.value.toMutableSet().apply {
            if (contains(photoId)) remove(photoId) else add(photoId)
        }
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun deletePhoto(photoId: String, storagePath: String?) {
        viewModelScope.launch {
            runCatching { photoRepository.deletePhoto(photoId, storagePath) }
                .onSuccess { loadPhotos() }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            val toDelete = _photos.value.filter { it.id in _selectedPhotoIds.value }
            val results = toDelete.map { photo ->
                async {
                    runCatching { photoRepository.deletePhoto(photo.id, photo.storagePath) }
                }
            }.awaitAll()

            val failedCount = results.count { it.isFailure }
            if (failedCount > 0) {
                _error.value = "Eliminate ${toDelete.size - failedCount}/${toDelete.size}. Alcune foto non sono state eliminate."
            }
            clearSelection()
            loadPhotos()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun togglePhotoPublic(photoId: String, current: Boolean) {
        viewModelScope.launch {
            runCatching { photoRepository.updatePhotoPublic(photoId, !current) }
                .onSuccess { loadPhotos() }
        }
    }

    private fun applyCategoryFilter() {
        val selected = _selectedCategory.value
        _photos.value = allPhotos.filter { photo ->
            val category = photo.positionData?.category
            if (selected == CATEGORY_KIDS) category == CATEGORY_KIDS
            else category == CATEGORY_ADULTS || category.isNullOrBlank()
        }
    }
}
