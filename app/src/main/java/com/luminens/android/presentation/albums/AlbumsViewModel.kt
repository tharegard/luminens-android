package com.luminens.android.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.Album
import com.luminens.android.data.model.Photo
import com.luminens.android.data.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Album detail state
    private val _albumPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val albumPhotos: StateFlow<List<Photo>> = _albumPhotos.asStateFlow()

    private val _availablePhotos = MutableStateFlow<List<Photo>>(emptyList())
    val availablePhotos: StateFlow<List<Photo>> = _availablePhotos.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadAlbums() }

    fun loadAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.getAlbums() }
                .onSuccess { _albums.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadAlbumPhotos(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.getAlbumPhotos(albumId) }
                .onSuccess { _albumPhotos.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            runCatching { albumRepository.createAlbum(name) }
                .onSuccess { loadAlbums() }
        }
    }

    fun loadAvailablePhotosForAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.getAvailablePhotosForAlbum(albumId) }
                .onSuccess { _availablePhotos.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addPhotosToAlbum(albumId: String, photoIds: List<String>) {
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.addPhotosToAlbum(albumId, photoIds) }
                .onSuccess {
                    refreshAlbumData(albumId)
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun removePhotoFromAlbum(albumId: String, photoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.removePhotoFromAlbum(albumId, photoId) }
                .onSuccess {
                    refreshAlbumData(albumId)
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun reorderAlbumPhotos(albumId: String, orderedPhotoIds: List<String>) {
        if (orderedPhotoIds.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.reorderAlbumPhotos(albumId, orderedPhotoIds) }
                .onSuccess { loadAlbumPhotos(albumId) }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            runCatching { albumRepository.deleteAlbum(albumId) }
                .onSuccess { loadAlbums() }
        }
    }

    fun toggleAlbumPublic(albumId: String, currentValue: Boolean) {
        viewModelScope.launch {
            runCatching { albumRepository.updateAlbumPublic(albumId, !currentValue) }
                .onSuccess { loadAlbums() }
        }
    }

    fun renameAlbum(albumId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            runCatching { albumRepository.updateAlbumName(albumId, newName.trim()) }
                .onSuccess { loadAlbums() }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun refreshAlbumData(albumId: String) = coroutineScope {
        awaitAll(
            async {
                runCatching { albumRepository.getAlbumPhotos(albumId) }
                    .onSuccess { _albumPhotos.value = it }
                    .onFailure { _error.value = it.message }
            },
            async {
                runCatching { albumRepository.getAvailablePhotosForAlbum(albumId) }
                    .onSuccess { _availablePhotos.value = it }
                    .onFailure { _error.value = it.message }
            },
            async {
                runCatching { albumRepository.getAlbums() }
                    .onSuccess { _albums.value = it }
                    .onFailure { _error.value = it.message }
            },
        )
    }
}
