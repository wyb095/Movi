package com.group2.movi.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.data.repository.UserPreferenceRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.EnhancedMatchInsight
import com.group2.movi.domain.model.HIGH_ROUTE_MATCH_PERCENT
import com.group2.movi.domain.model.PreferenceProfile
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.calculateEnhancedMatch
import com.group2.movi.domain.model.commuteCorridor
import com.group2.movi.domain.model.enhancedMatchReason
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
    val enhancedMatch: EnhancedMatchInsight? = null,
    val matchPercent: Int? = enhancedMatch?.baseMatch?.matchPercent,
    val matchReason: String? = enhancedMatchReason(enhancedMatch),
    val compositeScore: Double = enhancedMatch?.compositeScore ?: 0.0,
    val isOwnTask: Boolean = false,
    val matchState: DiscoverMatchState = when {
        isOwnTask -> DiscoverMatchState.OWN
        (enhancedMatch?.baseMatch?.matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT -> DiscoverMatchState.MATCHED
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
    val acceptingTaskId: String? = null,
    val feedbackMessage: String? = null,
    val feedbackError: String? = null,
    val loading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository,
    private val preferenceRepo: UserPreferenceRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)

    private val me = userRepo.currentUid?.let { uid ->
        userRepo.observeUser(uid)
    } ?: flowOf(null)

    private val rawTasks: StateFlow<List<Task>> = selectedCategory
        .flatMapLatest { cat -> taskRepo.observeOpenTasks(cat) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val preferences = userRepo.currentUid?.let { uid ->
        preferenceRepo.observePreferenceProfile(uid)
    } ?: flowOf(PreferenceProfile.empty(""))

    private val _state = MutableStateFlow(HomeUiState(loading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(rawTasks, me, preferences) { tasks, user, preferenceProfile ->
                val schedule = user?.commuteSchedule.orEmpty()
                val hasRouteSchedule = schedule.any { commuteCorridor(it).isNotEmpty() }
                val corridors = schedule.map { commuteCorridor(it) }.filter { it.isNotEmpty() }
                val visible = buildDiscoverFeed(
                    tasks = tasks,
                    currentUid = currentUid,
                    schedule = schedule,
                    preferences = preferenceProfile
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
                    acceptingTaskId = _state.value.acceptingTaskId,
                    feedbackMessage = _state.value.feedbackMessage,
                    feedbackError = _state.value.feedbackError,
                    loading = false
                )
            }.collect { newState -> _state.value = newState }
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun quickAcceptTask(taskId: String) {
        val uid = currentUid ?: run {
            _state.value = _state.value.copy(feedbackError = "Please sign in first.")
            return
        }
        _state.value = _state.value.copy(acceptingTaskId = taskId, feedbackError = null, feedbackMessage = null)
        viewModelScope.launch {
            val me = userRepo.getUser(uid)
            val result = taskRepo.acceptTaskAtomic(taskId, uid, me?.displayName ?: "User")
            if (result.isSuccess) {
                val accepted = _state.value.tasks.firstOrNull { it.task.taskId == taskId }
                if (accepted != null) {
                    runCatching {
                        preferenceRepo.recordTaskInteraction(
                            userId = uid,
                            taskId = taskId,
                            action = com.group2.movi.domain.model.InteractionAction.ACCEPT,
                            context = com.group2.movi.domain.model.InteractionContext(
                                category = accepted.task.category,
                                price = accepted.task.offeredPrice,
                                matchPercent = accepted.matchPercent ?: 0
                            )
                        )
                    }
                }
                _state.value = _state.value.copy(
                    acceptingTaskId = null,
                    feedbackMessage = "Task accepted. You can continue in My Tasks."
                )
            } else {
                _state.value = _state.value.copy(
                    acceptingTaskId = null,
                    feedbackError = friendlyAcceptError(result.exceptionOrNull())
                )
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(feedbackMessage = null, feedbackError = null)
    }

    val currentUid: String? get() = userRepo.currentUid

    private fun friendlyAcceptError(error: Throwable?): String {
        val message = error?.message.orEmpty()
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

private val matchedTaskComparator =
    compareByDescending<DiscoverTask> { it.compositeScore }
        .thenByDescending { it.matchPercent ?: 0 }
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
    preferences: PreferenceProfile = PreferenceProfile.empty(currentUid.orEmpty()),
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

        val enhancedMatch = if (hasRouteSchedule) {
            calculateEnhancedMatch(task, schedule, preferences, nowMillis)
        } else {
            null
        }

        if ((enhancedMatch?.baseMatch?.matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT) {
            matched += DiscoverTask(
                task = task,
                enhancedMatch = enhancedMatch,
                matchState = DiscoverMatchState.MATCHED
            )
        } else {
            unmatched += DiscoverTask(
                task = task,
                enhancedMatch = enhancedMatch,
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
