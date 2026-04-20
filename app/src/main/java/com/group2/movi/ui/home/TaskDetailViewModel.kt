package com.group2.movi.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskMatchInsight
import com.group2.movi.domain.model.commuteCorridor
import com.group2.movi.domain.model.countMatchingCarriers
import com.group2.movi.domain.model.findBestMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val myMatch: TaskMatchInsight? = null,
    val matchingCarrierCount: Int = 0,
    val hasCommuteRoute: Boolean = false,
    val loading: Boolean = true,
    val accepting: Boolean = false,
    val error: String? = null
)

private data class TaskDetailSnapshot(
    val task: Task?,
    val myMatch: TaskMatchInsight?,
    val matchingCarrierCount: Int,
    val hasCommuteRoute: Boolean
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState.get<String>("taskId"))

    private val _state = MutableStateFlow(TaskDetailUiState(loading = true))
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    val currentUid: String? get() = userRepo.currentUid
    val isRequester: Boolean get() = _state.value.task?.requesterId == currentUid

    init {
        viewModelScope.launch {
            val me = currentUid?.let { userRepo.observeUser(it) } ?: flowOf(null)
            val carriers = userRepo.observeUsersWithCommuteSchedule(excludeUid = currentUid)
            combine(taskRepo.observeTask(taskId), me, carriers) { task, user, users ->
                val hasCommuteRoute = user?.commuteSchedule?.any { commuteCorridor(it).isNotEmpty() } == true
                val myMatch = if (task != null && user != null && task.requesterId != user.userId) {
                    findBestMatch(task, user.commuteSchedule)
                } else {
                    null
                }
                val carrierCount = if (task != null) countMatchingCarriers(task, users) else 0
                TaskDetailSnapshot(
                    task = task,
                    myMatch = myMatch,
                    matchingCarrierCount = carrierCount,
                    hasCommuteRoute = hasCommuteRoute
                )
            }.collect { snapshot ->
                _state.value = _state.value.copy(
                    task = snapshot.task,
                    myMatch = snapshot.myMatch,
                    matchingCarrierCount = snapshot.matchingCarrierCount,
                    hasCommuteRoute = snapshot.hasCommuteRoute,
                    loading = false
                )
            }
        }
    }

    fun accept(onAccepted: () -> Unit) {
        val uid = currentUid ?: return
        _state.value = _state.value.copy(accepting = true, error = null)
        viewModelScope.launch {
            val me = userRepo.getUser(uid)
            val res = taskRepo.acceptTaskAtomic(taskId, uid, me?.displayName ?: "User")
            _state.value = _state.value.copy(
                accepting = false,
                error = res.exceptionOrNull()?.let(::friendlyAcceptError)
            )
            if (res.isSuccess) onAccepted()
        }
    }

    private fun friendlyAcceptError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("already been accepted", ignoreCase = true) ->
                "This task was just accepted by another carrier."
            message.contains("your own task", ignoreCase = true) ->
                "You can't accept your own task."
            message.contains("network", ignoreCase = true) ->
                "Network error. Please check your connection and try again."
            else -> "Failed to accept task. Please try again."
        }
    }
}
