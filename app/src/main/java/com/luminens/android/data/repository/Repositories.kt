package com.luminens.android.data.repository

import com.luminens.android.data.model.Profile
import com.luminens.android.data.remote.SupabaseDataSource
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val dataSource: SupabaseDataSource,
) {
    suspend fun getProfile(): Profile? = dataSource.getProfile()

    suspend fun updateDisplayName(name: String) {
        dataSource.updateProfile(mapOf("display_name" to name))
    }
}

class PhotoRepository @Inject constructor(
    private val dataSource: SupabaseDataSource,
) {
    suspend fun getPhotos() = dataSource.getPhotos()
    suspend fun deletePhoto(id: String, storagePath: String?) =
        dataSource.deletePhoto(id, storagePath)
    suspend fun updatePhotoPublic(id: String, isPublic: Boolean) =
        dataSource.updatePhotoPublic(id, isPublic)
    suspend fun insertPhoto(photo: Map<String, Any>) = dataSource.insertPhoto(photo)
    suspend fun getSignedUrl(path: String) = dataSource.getSignedUrl(path)
    suspend fun uploadPhoto(bytes: ByteArray, path: String, contentType: String = "image/jpeg") =
        dataSource.uploadPhoto(bytes, path, contentType)
    suspend fun getPublicPhoto(id: String) = dataSource.getPublicPhoto(id)
    suspend fun updatePhotoFile(photoId: String, file: java.io.File) = dataSource.updatePhotoFile(photoId, file)
}

class AlbumRepository @Inject constructor(
    private val dataSource: SupabaseDataSource,
) {
    suspend fun getAlbums() = dataSource.getAlbums()
    suspend fun getAlbumPhotos(albumId: String) = dataSource.getAlbumPhotos(albumId)
    suspend fun createAlbum(name: String) = dataSource.createAlbum(name)
    suspend fun deleteAlbum(id: String) = dataSource.deleteAlbum(id)
    suspend fun updateAlbumPublic(id: String, isPublic: Boolean) =
        dataSource.updateAlbumPublic(id, isPublic)
    suspend fun getPublicAlbum(id: String) = dataSource.getPublicAlbum(id)
    suspend fun getPublicAlbumPhotos(albumId: String) = dataSource.getPublicAlbumPhotos(albumId)
}
