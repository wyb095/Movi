package com.group2.movi.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val task: Task? = null,
    val loading: Boolean = true,
    val acting: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class TaskProgressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState.get<String>("taskId"))
    private val _state = MutableStateFlow(ProgressUiState(loading = true))
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    val currentUid: String? get() = userRepo.currentUid

    init {
        viewModelScope.launch {
            taskRepo.observeTask(taskId).collect { t ->
                _state.value = _state.value.copy(task = t, loading = false)
            }
        }
    }

    fun markPickedUp() = run("pickup") { taskRepo.markPickedUp(taskId) }
    fun markDelivered() = run("delivered") { taskRepo.markDelivered(taskId) }
    fun cancelTask() = run("cancel") { taskRepo.cancelTask(taskId) }

    private fun run(label: String, block: suspend () -> Result<Unit>) {
        _state.value = _state.value.copy(acting = true, error = null, success = null)
        viewModelScope.launch {
            val result = block()
            _state.value = _state.value.copy(
                acting = false,
                success = if (result.isSuccess) successMessage(label) else null,
                error = result.exceptionOrNull()?.let(::friendlyError)
            )
        }
    }

    private fun successMessage(label: String): String = when (label) {
        "pickup" -> "Task marked as picked up."
        "delivered" -> "Task marked as delivered."
        "cancel" -> "Task cancelled."
        else -> "Task updated."
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("network", ignoreCase = true) ->
                "Network error. Please check your connection and try again."
            message.contains("accepted before pickup", ignoreCase = true) ->
                "Task must be accepted before you can mark pickup."
            message.contains("picked up before delivery", ignoreCase = true) ->
                "Task must be picked up before you can mark delivery."
            message.contains("only open tasks can be cancelled", ignoreCase = true) ->
                "Only open tasks can be cancelled."
            else -> "Failed to update task status. Please try again."
        }
    }
}
