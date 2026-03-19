package com.luminens.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = if (authRepository.isAuthenticated) {
                AuthState.Authenticated
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            runCatching {
                authRepository.signInWithEmail(email.trim(), password)
            }.onSuccess {
                _authState.value = AuthState.Authenticated
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Errore di login")
                _isLoading.value = false
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            runCatching {
                authRepository.signUpWithEmail(email.trim(), password, displayName.trim())
            }.onSuccess {
                _authState.value = AuthState.Authenticated
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Errore di registrazione")
                _isLoading.value = false
            }
            _isLoading.value = false
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                authRepository.signInWithGoogle()
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Errore con Google")
                _isLoading.value = false
            }
            // Actual auth state update happens via deep link → handleDeepLink
        }
    }

    fun handleDeepLink(url: String) {
        viewModelScope.launch {
            runCatching { authRepository.handleDeepLink(url) }
            checkSession()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
