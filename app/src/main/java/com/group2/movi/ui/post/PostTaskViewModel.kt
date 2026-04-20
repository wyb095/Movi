package com.group2.movi.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskCategory
import com.group2.movi.domain.model.extractGeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostFormState(
    val step: Int = 0, // 0..3
    val category: String = TaskCategory.PARCEL,
    val title: String = "",
    val description: String = "",
    val localPhotoUri: Uri? = null,
    val uploadedPhotoUrl: String? = null,
    val uploadingPhoto: Boolean = false,
    val photoUploadError: String? = null,
    val pickupAddress: String = "",
    val pickupLocation: GeoPoint? = null,
    val dropoffAddress: String = "",
    val dropoffLocation: GeoPoint? = null,
    val direction: String = "SZ_TO_HK",
    val deadlineEpochMs: Long? = null,
    val isUrgent: Boolean = false,
    val priceHkd: String = "",
    val customsConfirmed: Boolean = false,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val submitted: Boolean = false
) {
    val hasSelectedPhoto: Boolean
        get() = localPhotoUri != null

    val hasUploadedPhoto: Boolean
        get() = !uploadedPhotoUrl.isNullOrBlank()

    val photoReadyForSubmit: Boolean
        get() = !hasSelectedPhoto || hasUploadedPhoto

    val stepValid: Boolean
        get() = when (step) {
            0 -> title.trim().length in 5..80 && description.trim().length <= 500 && category.isNotBlank()
            1 -> pickupAddress.trim().isNotBlank() && dropoffAddress.trim().isNotBlank()
            2 -> direction.isNotBlank() && deadlineEpochMs != null
                    && deadlineEpochMs > System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            3 -> {
                val price = priceHkd.toDoubleOrNull()
                price != null && price >= 5.0 && customsConfirmed
            }
            else -> false
        }
}

@HiltViewModel
class PostTaskViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostFormState())
    val state: StateFlow<PostFormState> = _state.asStateFlow()

    fun setCategory(c: String) = _state.update { it.copy(category = c) }
    fun setTitle(v: String) = _state.update { it.copy(title = v.take(80)) }
    fun setDescription(v: String) = _state.update { it.copy(description = v.take(500)) }

    fun setPickupPlace(address: String, loc: GeoPoint, placeId: String) {
        _state.update { it.copy(pickupAddress = address, pickupLocation = loc) }
    }

    fun setDropoffPlace(address: String, loc: GeoPoint, placeId: String) {
        _state.update { it.copy(dropoffAddress = address, dropoffLocation = loc) }
    }

    fun setPickupFromPaste(raw: String) {
        val geo = extractGeoPoint(raw)
        _state.update {
            it.copy(
                pickupAddress = if (geo != null) "%.4f, %.4f".format(geo.latitude, geo.longitude) else raw.trim(),
                pickupLocation = geo
            )
        }
    }

    fun setDropoffFromPaste(raw: String) {
        val geo = extractGeoPoint(raw)
        _state.update {
            it.copy(
                dropoffAddress = if (geo != null) "%.4f, %.4f".format(geo.latitude, geo.longitude) else raw.trim(),
                dropoffLocation = geo
            )
        }
    }

    fun setDirection(v: String) = _state.update { it.copy(direction = v) }
    fun setDeadline(epochMs: Long) = _state.update { it.copy(deadlineEpochMs = epochMs) }
    fun setUrgent(v: Boolean) = _state.update { it.copy(isUrgent = v) }
    fun setPrice(v: String) = _state.update { it.copy(priceHkd = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setCustoms(v: Boolean) = _state.update { it.copy(customsConfirmed = v) }

    fun next() {
        if (!_state.value.stepValid) return
        _state.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    }

    fun back() {
        _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    fun selectPhoto(uri: Uri?) {
        _state.update {
            it.copy(
                localPhotoUri = uri,
                uploadedPhotoUrl = null,
                uploadingPhoto = false,
                photoUploadError = null,
                submitError = null
            )
        }
        if (uri != null) uploadPhoto(uri)
    }

    private fun uploadPhoto(uri: Uri) {
        _state.update {
            it.copy(
                uploadingPhoto = true,
                uploadedPhotoUrl = null,
                photoUploadError = null,
                submitError = null
            )
        }
        viewModelScope.launch {
            val res = taskRepo.uploadItemPhoto(uri)
            _state.update {
                it.copy(
                    uploadingPhoto = false,
                    uploadedPhotoUrl = res.getOrNull(),
                    photoUploadError = res.exceptionOrNull()?.let(::friendlyPhotoUploadError)
                )
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (!s.stepValid) return
        if (s.uploadingPhoto) {
            _state.update { it.copy(submitError = "Please wait for the photo upload to finish.") }
            return
        }
        if (!s.photoReadyForSubmit) {
            _state.update {
                it.copy(
                    submitError = it.photoUploadError
                        ?: "The selected photo was not uploaded. Please try choosing the photo again."
                )
            }
            return
        }
        val uid = userRepo.currentUid ?: run {
            _state.update { it.copy(submitError = "Please sign in first.") }
            return
        }
        _state.update { it.copy(submitting = true, submitError = null) }
        viewModelScope.launch {
            val me = userRepo.getUser(uid)
            val task = Task(
                requesterId = uid,
                requesterName = me?.displayName ?: "",
                requesterRating = me?.rating ?: 0.0,
                category = s.category,
                title = s.title.trim(),
                description = s.description.trim(),
                itemPhotoUrl = s.uploadedPhotoUrl,
                pickupLocation = s.pickupLocation,
                pickupAddress = s.pickupAddress.trim(),
                dropoffLocation = s.dropoffLocation,
                dropoffAddress = s.dropoffAddress.trim(),
                direction = s.direction,
                requiredBefore = s.deadlineEpochMs?.let { Timestamp(java.util.Date(it)) },
                offeredPrice = s.priceHkd.toDoubleOrNull() ?: 0.0,
                isUrgent = s.isUrgent,
                customsDeclaration = s.customsConfirmed,
                escrowHoldAmount = s.priceHkd.toDoubleOrNull() ?: 0.0
            )
            val res = taskRepo.postTask(task)
            _state.update {
                if (res.isSuccess) it.copy(submitting = false, submitted = true)
                else it.copy(submitting = false, submitError = res.exceptionOrNull()?.message ?: "Failed to post task")
            }
        }
    }

    private fun friendlyPhotoUploadError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("permission", ignoreCase = true) ->
                "Photo upload was blocked by Firebase Storage permissions."
            message.contains("network", ignoreCase = true) ->
                "Photo upload failed because of a network problem."
            else -> "Photo upload failed. Please try selecting the image again."
        }
    }
}
