package com.example.inventoryapp.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (UserEntity) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(user)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        onSuccess: (UserEntity) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signUp(fullName, email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(user)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Sign up failed"
                    )
                }
            )
        }
    }

    fun resetPassword(
        email: String,
        newPassword: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (newPassword != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.resetPassword(email, newPassword)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Password reset successfully. You can now login."
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to reset password"
                    )
                }
            )
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }
}

class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val sessionStore = SessionStore(application)
            val repo = AuthRepository(db.userDao(), sessionStore)
            return AuthViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
