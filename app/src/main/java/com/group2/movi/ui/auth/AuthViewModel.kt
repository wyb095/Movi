package com.group2.movi.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.AuthRepository
import com.group2.movi.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class Notice(val message: String) : AuthUiState
    data object Success : AuthUiState
    data object NeedsVerification : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val userRepo: UserRepository,
    private val firebaseMessaging: FirebaseMessaging
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun resetState() { _state.value = AuthUiState.Idle }

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _state.value = AuthUiState.Error("Please enter your email and password.")
            return
        }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repo.login(trimmedEmail, password)
            _state.value = result.fold(
                onSuccess = {
                    syncFcmToken()
                    val verified = repo.reloadUser()
                    if (verified) AuthUiState.Success else AuthUiState.NeedsVerification
                },
                onFailure = { AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    fun register(displayName: String, email: String, password: String, confirmPassword: String) {
        val name = displayName.trim()
        val mail = email.trim()
        if (name.length < 2) { _state.value = AuthUiState.Error("Enter your full name."); return }
        if (!AuthRepository.isValidEmail(mail)) {
            _state.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) { _state.value = AuthUiState.Error("Password must be at least 6 characters."); return }
        if (password != confirmPassword) { _state.value = AuthUiState.Error("Passwords do not match."); return }

        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repo.register(name, mail, password)
            _state.value = result.fold(
                onSuccess = {
                    syncFcmToken()
                    AuthUiState.NeedsVerification
                },
                onFailure = { AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        val mail = email.trim()
        if (mail.isEmpty()) { _state.value = AuthUiState.Error("Enter your email first."); return }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repo.sendPasswordReset(mail)
            _state.update {
                result.fold(
                    onSuccess = { AuthUiState.Notice("Password reset email sent. Check your inbox.") },
                    onFailure = { AuthUiState.Error(friendlyError(it.message)) }
                )
            }
        }
    }

    fun resendVerification() {
        viewModelScope.launch { repo.resendVerification() }
    }

    fun checkVerified(onVerified: () -> Unit) {
        viewModelScope.launch {
            if (repo.reloadUser()) onVerified()
        }
    }

    fun logout() = repo.logout()

    private suspend fun syncFcmToken() {
        val uid = repo.currentUser?.uid ?: return
        val token = runCatching { firebaseMessaging.token.await() }.getOrNull() ?: return
        userRepo.updateFcmToken(uid, token)
    }

    private fun friendlyError(raw: String?): String = when {
        raw == null -> "Something went wrong."
        raw.contains("password is invalid", true) || raw.contains("wrong password", true) -> "Wrong password. Try again."
        raw.contains("no user record", true) -> "No account found with that email."
        raw.contains("email address is already", true) -> "This email is already registered."
        raw.contains("network", true) -> "Network error. Check your connection."
        else -> raw.take(120)
    }
}
