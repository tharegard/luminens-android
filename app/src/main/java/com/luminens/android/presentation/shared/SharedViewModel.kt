package com.luminens.android.presentation.shared

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.Album
import com.luminens.android.data.model.Photo
import com.luminens.android.data.repository.AlbumRepository
import com.luminens.android.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _album = mutableStateOf<Album?>(null)
    val album: State<Album?> = _album

    private val _albumPhotos = mutableStateOf<List<Photo>>(emptyList())
    val albumPhotos: State<List<Photo>> = _albumPhotos

    private val _sharedPhoto = mutableStateOf<Photo?>(null)
    val sharedPhoto: State<Photo?> = _sharedPhoto

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadSharedAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { albumRepository.getPublicAlbum(albumId) }
                .onSuccess { result ->
                    _album.value = result
                    runCatching { albumRepository.getAlbumPhotos(albumId) }
                        .onSuccess { _albumPhotos.value = it }
                }
            _isLoading.value = false
        }
    }

    fun loadSharedPhoto(photoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { photoRepository.getPublicPhoto(photoId) }
                .onSuccess { _sharedPhoto.value = it }
            _isLoading.value = false
        }
    }
}
