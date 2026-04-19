package com.group2.movi.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.AuthRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.countMatchingCarriers
import com.group2.movi.service.MoviLocalNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyTasksUiState(
    val asRequester: List<Task> = emptyList(),
    val asCarrier: List<Task> = emptyList(),
    val requesterMatchCounts: Map<String, Int> = emptyMap(),
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyTasksViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository,
    private val notifier: MoviLocalNotifier
) : ViewModel() {

    private val _state = MutableStateFlow(MyTasksUiState(loading = true))
    val state: StateFlow<MyTasksUiState> = _state.asStateFlow()
    private val lastMatchCounts = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow()
                .map { it?.uid }
                .onStart { emit(authRepo.currentUser?.uid ?: userRepo.currentUid) }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid == null) {
                        lastMatchCounts.clear()
                        _state.value = MyTasksUiState(loading = false)
                        return@collectLatest
                    }

                    // Seed each source with an empty list so combine() emits
                    // the task list immediately, even if the users collection
                    // is blocked by Firestore rules or slow to respond.
                    val requesterTasks = taskRepo.observeTasksAsRequester(uid)
                        .onStart { emit(emptyList()) }
                    val carrierTasks = taskRepo.observeTasksAsCarrier(uid)
                        .onStart { emit(emptyList()) }
                    val carriers = userRepo.observeUsersWithCommuteSchedule(excludeUid = uid)
                        .onStart { emit(emptyList()) }

                    combine(requesterTasks, carrierTasks, carriers) { requester, carrier, users ->
                        val counts = requester.associate { task ->
                            task.taskId to countMatchingCarriers(task, users)
                        }
                        MyTasksUiState(
                            asRequester = requester,
                            asCarrier = carrier,
                            requesterMatchCounts = counts,
                            loading = false
                        )
                    }.collect { newState ->
                        newState.requesterMatchCounts.forEach { (taskId, count) ->
                            val previous = lastMatchCounts[taskId] ?: 0
                            if (count > previous && count > 0) {
                                newState.asRequester.firstOrNull { it.taskId == taskId }?.title?.let { title ->
                                    notifier.showMatchReady(title.ifBlank { "your task" }, count)
                                }
                            }
                        }
                        lastMatchCounts.clear()
                        lastMatchCounts.putAll(newState.requesterMatchCounts)
                        _state.value = newState
                    }
                }
        }
    }
}
