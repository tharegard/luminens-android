package com.luminens.android.di

import com.luminens.android.data.remote.SupabaseDataSource
import com.luminens.android.data.repository.AlbumRepository
import com.luminens.android.data.repository.AuthRepository
import com.luminens.android.data.repository.GenerationRepository
import com.luminens.android.data.repository.PhotoRepository
import com.luminens.android.data.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSupabaseDataSource(client: SupabaseClient) = SupabaseDataSource(client)

    @Provides
    @Singleton
    fun provideAuthRepository(client: SupabaseClient) = AuthRepository(client)

    @Provides
    @Singleton
    fun provideProfileRepository(dataSource: SupabaseDataSource) = ProfileRepository(dataSource)

    @Provides
    @Singleton
    fun providePhotoRepository(dataSource: SupabaseDataSource) = PhotoRepository(dataSource)

    @Provides
    @Singleton
    fun provideAlbumRepository(dataSource: SupabaseDataSource) = AlbumRepository(dataSource)

    @Provides
    @Singleton
    fun provideGenerationRepository(client: SupabaseClient) = GenerationRepository(client)
}
