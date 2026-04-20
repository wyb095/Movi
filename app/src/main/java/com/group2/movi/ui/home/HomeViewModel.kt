package com.group2.movi.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.HIGH_ROUTE_MATCH_PERCENT
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
    val match: TaskMatchInsight? = null,
    val matchPercent: Int? = match?.matchPercent,
    val isOwnTask: Boolean = false,
    val matchState: DiscoverMatchState = when {
        isOwnTask -> DiscoverMatchState.OWN
        (match?.matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT -> DiscoverMatchState.MATCHED
        else -> DiscoverMatchState.UNMATCHED
    }
)

enum class DiscoverMatchState {
    MATCHED,
    UNMATCHED,
    OWN
}

data class HomeUiState(
    val tasks: List<DiscoverTask> = emptyList(),
    val selectedCategory: String? = null,
    val hasCommuteSchedule: Boolean = false,
    val corridors: List<List<GeoPoint>> = emptyList(),
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
            combine(rawTasks, me) { tasks, user ->
                val schedule = user?.commuteSchedule.orEmpty()
                val hasRouteSchedule = schedule.any { commuteCorridor(it).isNotEmpty() }
                val corridors = schedule.map { commuteCorridor(it) }.filter { it.isNotEmpty() }
                val visible = buildDiscoverFeed(
                    tasks = tasks,
                    currentUid = currentUid,
                    schedule = schedule
                )
                val matchedCount = visible.count { it.matchState == DiscoverMatchState.MATCHED }
                val unmatchedCount = visible.count { it.matchState == DiscoverMatchState.UNMATCHED }
                val ownCount = visible.count { it.matchState == DiscoverMatchState.OWN }
                Log.d(
                    "HomeViewModel",
                    "Discover feed: matched=$matchedCount, unmatched=$unmatchedCount, own=$ownCount, totalOpen=${tasks.size}, hasRouteSchedule=$hasRouteSchedule"
                )
                HomeUiState(
                    tasks = visible,
                    selectedCategory = selectedCategory.value,
                    hasCommuteSchedule = hasRouteSchedule,
                    corridors = corridors,
                    loading = false
                )
            }.collect { newState -> _state.value = newState }
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    val currentUid: String? get() = userRepo.currentUid
}

private val matchedTaskComparator =
    compareByDescending<DiscoverTask> { it.matchPercent ?: 0 }
        .thenByDescending { it.task.isUrgent }
        .thenByDescending { it.task.createdAt?.seconds ?: 0L }

private val unmatchedTaskComparator =
    compareByDescending<DiscoverTask> { it.matchPercent ?: -1 }
        .thenByDescending { it.task.isUrgent }
        .thenByDescending { it.task.createdAt?.seconds ?: 0L }

private val fallbackTaskComparator =
    compareByDescending<DiscoverTask> { it.task.isUrgent }
        .thenByDescending { it.task.createdAt?.seconds ?: 0L }

internal fun buildDiscoverFeed(
    tasks: List<Task>,
    currentUid: String?,
    schedule: List<CommuteEntry>,
    nowMillis: Long = System.currentTimeMillis()
): List<DiscoverTask> {
    val matched = mutableListOf<DiscoverTask>()
    val unmatched = mutableListOf<DiscoverTask>()
    val own = mutableListOf<DiscoverTask>()
    val hasRouteSchedule = schedule.any { commuteCorridor(it).isNotEmpty() }

    tasks.forEach { task ->
        val isOwnTask = currentUid != null && task.requesterId == currentUid
        if (isOwnTask) {
            own += DiscoverTask(
                task = task,
                isOwnTask = true,
                matchState = DiscoverMatchState.OWN
            )
            return@forEach
        }

        val bestMatch = if (hasRouteSchedule) {
            findBestMatch(task, schedule, nowMillis)
        } else {
            null
        }

        if ((bestMatch?.matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT) {
            matched += DiscoverTask(
                task = task,
                match = bestMatch,
                matchPercent = bestMatch?.matchPercent,
                matchState = DiscoverMatchState.MATCHED
            )
        } else {
            unmatched += DiscoverTask(
                task = task,
                match = bestMatch,
                matchPercent = bestMatch?.matchPercent,
                matchState = DiscoverMatchState.UNMATCHED
            )
        }
    }

    matched.sortWith(matchedTaskComparator)
    if (hasRouteSchedule) {
        unmatched.sortWith(unmatchedTaskComparator)
    } else {
        unmatched.sortWith(fallbackTaskComparator)
    }
    own.sortWith(fallbackTaskComparator)
    return matched + unmatched + own
}
