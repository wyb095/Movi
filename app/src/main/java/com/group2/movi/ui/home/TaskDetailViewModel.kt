package com.group2.movi.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.UserPreferenceRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.InteractionAction
import com.group2.movi.domain.model.InteractionContext
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskMatchInsight
import com.group2.movi.domain.model.User
import com.group2.movi.domain.model.commuteCorridor
import com.group2.movi.domain.model.countMatchingCarriers
import com.group2.movi.domain.model.findBestMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    val requesterUser: User? = null,
    val hasCommuteRoute: Boolean = false,
    val loading: Boolean = true,
    val accepting: Boolean = false,
    val error: String? = null
)

private data class TaskDetailSnapshot(
    val task: Task?,
    val myMatch: TaskMatchInsight?,
    val matchingCarrierCount: Int,
    val requesterUser: User?,
    val hasCommuteRoute: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository,
    private val preferenceRepo: UserPreferenceRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState.get<String>("taskId"))

    private val _state = MutableStateFlow(TaskDetailUiState(loading = true))
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()
    private var recordedViewForTaskId: String? = null

    val currentUid: String? get() = userRepo.currentUid
    val isRequester: Boolean get() = _state.value.task?.requesterId == currentUid

    init {
        viewModelScope.launch {
            val taskFlow = taskRepo.observeTask(taskId)
            val me = currentUid?.let { userRepo.observeUser(it) } ?: flowOf(null)
            val carriers = userRepo.observeUsersWithCommuteSchedule(excludeUid = currentUid)
            val requester = taskFlow.flatMapLatest { task ->
                if (task?.requesterId.isNullOrBlank()) flowOf(null)
                else userRepo.observeUser(task!!.requesterId)
            }
            combine(taskFlow, me, carriers, requester) { task, user, users, requesterUser ->
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
                    requesterUser = requesterUser,
                    hasCommuteRoute = hasCommuteRoute
                )
            }.collect { snapshot ->
                val task = snapshot.task
                if (
                    task != null &&
                    currentUid != null &&
                    currentUid != task.requesterId &&
                    recordedViewForTaskId != task.taskId
                ) {
                    recordedViewForTaskId = task.taskId
                    val matchPercent = snapshot.myMatch?.matchPercent ?: 0
                    viewModelScope.launch {
                        runCatching {
                            preferenceRepo.recordTaskInteraction(
                                userId = currentUid!!,
                                taskId = task.taskId,
                                action = InteractionAction.VIEW,
                                context = InteractionContext(
                                    category = task.category,
                                    price = task.offeredPrice,
                                    matchPercent = matchPercent
                                )
                            )
                        }
                    }
                }
                _state.value = _state.value.copy(
                    task = snapshot.task,
                    myMatch = snapshot.myMatch,
                    matchingCarrierCount = snapshot.matchingCarrierCount,
                    requesterUser = snapshot.requesterUser,
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
            if (res.isSuccess) {
                _state.value.task?.let { task ->
                    runCatching {
                        preferenceRepo.recordTaskInteraction(
                            userId = uid,
                            taskId = taskId,
                            action = InteractionAction.ACCEPT,
                            context = InteractionContext(
                                category = task.category,
                                price = task.offeredPrice,
                                matchPercent = _state.value.myMatch?.matchPercent ?: 0
                            )
                        )
                    }
                }
                onAccepted()
            }
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
