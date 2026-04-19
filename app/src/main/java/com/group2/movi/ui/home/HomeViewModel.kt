package com.group2.movi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskMatchInsight
import com.group2.movi.domain.model.commuteCorridor
import com.group2.movi.domain.model.findBestMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverTask(
    val task: Task,
    val match: TaskMatchInsight? = null
)

data class HomeUiState(
    val tasks: List<DiscoverTask> = emptyList(),
    val selectedCategory: String? = null,
    val maxDetourMinutes: Float = 60f,
    val hasCommuteSchedule: Boolean = false,
    val commuteCorridor: List<GeoPoint> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val maxDetourMinutes = MutableStateFlow(60f)

    private val me = userRepo.currentUid?.let { uid ->
        userRepo.observeUser(uid)
    } ?: flowOf(null)

    private val rawTasks: StateFlow<List<Task>> = selectedCategory
        .flatMapLatest { cat -> taskRepo.observeOpenTasks(cat) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(HomeUiState(loading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(rawTasks, me, maxDetourMinutes, selectedCategory) { tasks, user, detour, category ->
                val schedule = user?.commuteSchedule.orEmpty()
                val hasSchedule = schedule.isNotEmpty()
                val highlightedCorridor = schedule.firstNotNullOfOrNull { entry ->
                    commuteCorridor(entry).takeIf { it.size >= 3 }
                } ?: emptyList()
                val mapped = tasks.map { task ->
                    DiscoverTask(
                        task = task,
                        match = if (hasSchedule) findBestMatch(task, schedule) else null
                    )
                }
                val visible = if (hasSchedule) {
                    mapped
                        .filter { it.match != null && it.match.detourMinutes <= detour.toInt() }
                        .sortedWith(
                            compareByDescending<DiscoverTask> { it.match?.score ?: 0.0 }
                                .thenByDescending { it.task.isUrgent }
                                .thenByDescending { it.task.createdAt?.seconds ?: 0L }
                        )
                } else {
                    mapped.sortedWith(
                        compareByDescending<DiscoverTask> { it.task.isUrgent }
                            .thenByDescending { it.task.createdAt?.seconds ?: 0L }
                    )
                }
                HomeUiState(
                    tasks = visible,
                    selectedCategory = category,
                    maxDetourMinutes = detour,
                    hasCommuteSchedule = hasSchedule,
                    commuteCorridor = highlightedCorridor,
                    loading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun setMaxDetourMinutes(value: Float) {
        maxDetourMinutes.value = value
    }

    val currentUid: String? get() = userRepo.currentUid
}
