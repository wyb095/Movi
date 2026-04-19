package com.group2.movi.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.ReviewRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Review
import com.group2.movi.domain.model.ReviewRole
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfirmUiState(
    val task: Task? = null,
    val verificationCode: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val submitting: Boolean = false,
    val done: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class DeliveryConfirmViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val taskRepo: TaskRepository,
    private val reviewRepo: ReviewRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState.get<String>("taskId"))
    private val _state = MutableStateFlow(ConfirmUiState())
    val state: StateFlow<ConfirmUiState> = _state.asStateFlow()

    val currentUid: String? get() = userRepo.currentUid

    init {
        viewModelScope.launch {
            taskRepo.observeTask(taskId).collect { task ->
                _state.value = _state.value.copy(
                    task = task,
                    loading = false
                )
            }
        }
    }

    fun setVerificationCode(code: String) {
        _state.value = _state.value.copy(verificationCode = code.trim())
    }

    fun setRating(r: Int) { _state.value = _state.value.copy(rating = r.coerceIn(1, 5)) }
    fun setComment(c: String) { _state.value = _state.value.copy(comment = c.take(300)) }

    fun submit() {
        val uid = currentUid ?: run {
            _state.value = _state.value.copy(error = "Please sign in first.")
            return
        }
        val task = _state.value.task ?: run {
            _state.value = _state.value.copy(error = "Task not found.")
            return
        }
        _state.value = _state.value.copy(submitting = true, error = null, success = null)
        viewModelScope.launch {
            val isRequester = uid == task.requesterId
            if (!canReviewTask(task, uid)) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = "Review is not available for this task yet."
                )
                return@launch
            }
            val needsConfirmation = isRequester && task.status == TaskStatus.DELIVERED
            val reviewAlreadySubmitted = if (isRequester) task.requesterReviewed else task.carrierReviewed

            if (needsConfirmation) {
                val confirmResult = taskRepo.confirmReceipt(taskId, _state.value.verificationCode)
                if (confirmResult.isFailure) {
                    _state.value = _state.value.copy(
                        submitting = false,
                        error = friendlyConfirmError(confirmResult.exceptionOrNull())
                    )
                    return@launch
                }
            }

            if (reviewAlreadySubmitted) {
                _state.value = _state.value.copy(
                    submitting = false,
                    done = true,
                    success = if (needsConfirmation) {
                        "Delivery confirmed and payment released."
                    } else {
                        "Review already submitted."
                    }
                )
                return@launch
            }

            val targetId = if (isRequester) task.carrierId else task.requesterId
            if (targetId.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = "The other user is missing for this task."
                )
                return@launch
            }

            val me = userRepo.getUser(uid)
            val reviewResult = reviewRepo.postReview(
                Review(
                    taskId = taskId,
                    reviewerId = uid,
                    reviewerName = me?.displayName ?: "User",
                    revieweeId = targetId,
                    reviewerRole = if (isRequester) ReviewRole.REQUESTER else ReviewRole.CARRIER,
                    revieweeRole = if (isRequester) ReviewRole.CARRIER else ReviewRole.REQUESTER,
                    rating = _state.value.rating,
                    comment = _state.value.comment.trim()
                )
            )
            if (reviewResult.isFailure) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = if (needsConfirmation) {
                        "Delivery confirmed, but review failed to submit. Please try again."
                    } else {
                        friendlyReviewError(reviewResult.exceptionOrNull())
                    }
                )
                return@launch
            }
            _state.value = _state.value.copy(
                submitting = false,
                done = true,
                success = if (needsConfirmation) {
                    "Delivery confirmed, payment released, and review submitted."
                } else {
                    "Review submitted."
                }
            )
        }
    }

    fun raiseDispute() {
        _state.value = _state.value.copy(submitting = true, error = null, success = null)
        viewModelScope.launch {
            val result = taskRepo.raiseDispute(taskId)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = "Failed to raise dispute. Please try again."
                )
                return@launch
            }
            _state.value = _state.value.copy(
                submitting = false,
                done = true,
                success = "Dispute submitted."
            )
        }
    }

    private fun friendlyConfirmError(error: Throwable?): String {
        val message = error?.message.orEmpty()
        return when {
            message.contains("already confirmed", ignoreCase = true) ->
                "Delivery was already confirmed."
            message.contains("not ready for confirmation", ignoreCase = true) ->
                "Task is not ready for confirmation."
            message.contains("invalid pin", ignoreCase = true) ->
                "Invalid PIN or QR token."
            message.contains("network", ignoreCase = true) ->
                "Network error. Please check your connection and try again."
            else -> "Failed to confirm delivery."
        }
    }

    private fun friendlyReviewError(error: Throwable?): String {
        val message = error?.message.orEmpty()
        return when {
            message.contains("already reviewed", ignoreCase = true) ->
                "You already reviewed this task."
            message.contains("network", ignoreCase = true) ->
                "Network error. Please check your connection and try again."
            else -> "Failed to submit review."
        }
    }
}

internal fun canReviewTask(task: Task, uid: String?): Boolean {
    if (uid.isNullOrBlank()) return false
    if (uid != task.requesterId && uid != task.carrierId) return false
    return task.status == TaskStatus.DELIVERED || task.status == TaskStatus.CONFIRMED
}
