package com.group2.movi.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.AuthRepository
import com.group2.movi.data.repository.ReviewRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.Review
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
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

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
private const val MONTH_MS = 30L * 24 * 60 * 60 * 1000

data class EditProfileState(
    val savingDisplayName: Boolean = false,
    val uploadingPhoto: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val taskRepo: TaskRepository,
    private val reviewRepo: ReviewRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val currentUid: String? get() = userRepo.currentUid

    val user: StateFlow<User?> = run {
        val uid = currentUid
        if (uid == null) MutableStateFlow<User?>(null).asStateFlow()
        else userRepo.observeUser(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    val myCarrierTasks: StateFlow<List<Task>> = run {
        val uid = currentUid
        if (uid == null) MutableStateFlow<List<Task>>(emptyList()).asStateFlow()
        else taskRepo.observeTasksAsCarrier(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val reviews: StateFlow<List<Review>> = run {
        val uid = currentUid
        if (uid == null) MutableStateFlow<List<Review>>(emptyList()).asStateFlow()
        else reviewRepo.observeReviewsForUser(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _editProfileState = MutableStateFlow(EditProfileState())
    val editProfileState: StateFlow<EditProfileState> = _editProfileState.asStateFlow()

    fun updateDisplayName(name: String, onDone: () -> Unit) {
        val uid = currentUid ?: return
        _editProfileState.update {
            it.copy(savingDisplayName = true, errorMessage = null, infoMessage = null)
        }
        viewModelScope.launch {
            val result = userRepo.updateDisplayName(uid, name.trim())
            if (result.isSuccess) {
                _editProfileState.update {
                    it.copy(savingDisplayName = false, errorMessage = null, infoMessage = "Profile updated.")
                }
                onDone()
            } else {
                _editProfileState.update {
                    it.copy(
                        savingDisplayName = false,
                        errorMessage = friendlyProfileError(
                            result.exceptionOrNull(),
                            fallback = "Failed to update profile. Please try again."
                        ),
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        val uid = currentUid ?: return
        _editProfileState.update {
            it.copy(uploadingPhoto = true, errorMessage = null, infoMessage = null)
        }
        viewModelScope.launch {
            val result = userRepo.uploadProfilePhoto(uid, uri)
            _editProfileState.update {
                if (result.isSuccess) {
                    it.copy(
                        uploadingPhoto = false,
                        errorMessage = null,
                        infoMessage = "Photo updated."
                    )
                } else {
                    it.copy(
                        uploadingPhoto = false,
                        errorMessage = friendlyProfileError(
                            result.exceptionOrNull(),
                            fallback = "Failed to upload photo. Please try again."
                        ),
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun clearEditProfileFeedback() {
        _editProfileState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun addSchedule(entry: CommuteEntry) {
        val uid = currentUid ?: return
        viewModelScope.launch { userRepo.addCommuteEntry(uid, entry) }
    }

    fun removeSchedule(entry: CommuteEntry) {
        val uid = currentUid ?: return
        viewModelScope.launch { userRepo.removeCommuteEntry(uid, entry) }
    }

    fun logout() = authRepo.logout()

    /** Sum earnings only after payment release for tasks where the user was the carrier. */
    fun earnings(list: List<Task>): Triple<Double, Double, Double> =
        calculateEarnings(list, System.currentTimeMillis())

    fun computedStats(list: List<Task>): Pair<Int, Double> {
        val completed = list.filter(::countsTowardsEarnings)
        val all = completed.sumOf { it.finalPrice ?: it.offeredPrice }
        return completed.size to all
    }
}

internal fun calculateEarnings(list: List<Task>, nowMillis: Long): Triple<Double, Double, Double> {
    val completed = list.filter(::countsTowardsEarnings)
    val all = completed.sumOf(::earningAmount)
    val month = completed.filter {
        (earningTimestampMillis(it) ?: 0L) > nowMillis - MONTH_MS
    }
        .sumOf(::earningAmount)
    val week = completed.filter {
        (earningTimestampMillis(it) ?: 0L) > nowMillis - WEEK_MS
    }
        .sumOf(::earningAmount)
    return Triple(week, month, all)
}

internal fun countsTowardsEarnings(task: Task): Boolean = task.status == TaskStatus.CONFIRMED

internal fun earningTimestampMillis(task: Task): Long? {
    return task.escrowReleasedAt?.toDate()?.time ?: task.deliveredAt?.toDate()?.time
}

internal fun earningAmount(task: Task): Double = task.finalPrice ?: task.offeredPrice

private fun friendlyProfileError(error: Throwable?, fallback: String): String {
    val message = error?.message.orEmpty()
    return when {
        message.contains("network", ignoreCase = true) ->
            "Network error. Please check your connection and try again."
        else -> fallback
    }
}
