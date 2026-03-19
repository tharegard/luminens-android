package com.luminens.android.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    private val auth get() = client.auth

    val currentUser: Flow<UserInfo?> = auth.sessionStatus.map { status ->
        auth.currentUserOrNull()
    }

    val isAuthenticated: Boolean
        get() = auth.currentSessionOrNull() != null

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("display_name", JsonPrimitive(displayName)) }
        }
    }

    suspend fun signInWithGoogle() {
        auth.signInWith(Google)
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun handleDeepLink(url: String) {
        // Deep links are handled automatically by the supabase-kt Auth plugin
    }

    fun currentUserId(): String? = auth.currentUserOrNull()?.id
}


