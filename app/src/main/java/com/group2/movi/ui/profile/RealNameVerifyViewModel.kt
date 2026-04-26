package com.group2.movi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.data.repository.VerificationRepository
import com.group2.movi.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RealNameVerifyUiState(
    val loading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class RealNameVerifyViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val verificationRepo: VerificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RealNameVerifyUiState())
    val state: StateFlow<RealNameVerifyUiState> = _state.asStateFlow()

    val user: StateFlow<User?> = run {
        val uid = userRepo.currentUid
        if (uid == null) MutableStateFlow<User?>(null).asStateFlow()
        else userRepo.observeUser(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun verify(realName: String, idCardNumber: String, onVerified: () -> Unit) {
        val uid = userRepo.currentUid ?: run {
            _state.value = RealNameVerifyUiState(errorMessage = "Please sign in first.")
            return
        }
        val trimmedName = realName.trim()
        val trimmedId = idCardNumber.trim().uppercase()
        if (trimmedName.length < 2) {
            _state.value = RealNameVerifyUiState(errorMessage = "Enter your legal name.")
            return
        }
        if (!looksLikeIdCard(trimmedId)) {
            _state.value = RealNameVerifyUiState(errorMessage = "Enter a valid ID number.")
            return
        }

        _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = verificationRepo.verifyRealName(uid, trimmedName, trimmedId)
            if (result.isSuccess) {
                _state.value = RealNameVerifyUiState(
                    loading = false,
                    successMessage = "Verification completed."
                )
                onVerified()
            } else {
                _state.value = RealNameVerifyUiState(
                    loading = false,
                    errorMessage = friendlyVerificationError(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun friendlyVerificationError(raw: String?): String = when {
        raw.isNullOrBlank() -> "Verification failed. Please try again."
        raw.contains("permission", ignoreCase = true) -> "Verification service is not configured yet."
        raw.contains("network", ignoreCase = true) -> "Network error. Please try again."
        else -> raw.take(120)
    }
}

internal fun looksLikeIdCard(value: String): Boolean {
    val mainland = Regex("""\d{17}[\dX]""")
    val passportLike = Regex("""[A-Z0-9]{6,18}""")
    return mainland.matches(value) || passportLike.matches(value)
}
