package com.group2.movi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.Timestamp
import com.group2.movi.config.MapsConfig
import com.group2.movi.domain.model.ComplianceChecker
import com.group2.movi.domain.model.ComplianceSeverity
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskCategory
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.components.LoadingBox
import com.group2.movi.ui.components.Pill
import com.group2.movi.ui.components.displayablePlace
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviSecondary
import com.group2.movi.ui.theme.MoviWarning
import java.text.SimpleDateFormat
import java.util.Locale

private enum class DiscoverMode { LIST, MAP }

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    onTaskClick: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var mode by rememberSaveable { mutableStateOf(DiscoverMode.LIST) }
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    var quickAcceptCard by androidx.compose.runtime.remember { mutableStateOf<DiscoverTask?>(null) }

    LaunchedEffect(state.feedbackMessage, state.feedbackError) {
        val feedback = state.feedbackError ?: state.feedbackMessage
        if (!feedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedback)
            if (state.feedbackMessage != null) quickAcceptCard = null
            vm.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Discover tasks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            DiscoverToolbar(
                state = state,
                mode = mode,
                onModeChange = { mode = it },
                onSelectCategory = vm::selectCategory
            )

            when {
                state.loading -> LoadingBox()
                state.tasks.isEmpty() -> EmptyState(
                    title = "No open tasks right now",
                    subtitle = "Pull down to refresh or check back soon."
                )
                mode == DiscoverMode.MAP -> TaskMap(
                    tasks = state.tasks,
                    hasCommuteSchedule = state.hasCommuteSchedule,
                    corridors = state.corridors,
                    onTaskClick = onTaskClick,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tasks, key = { it.task.taskId }) { card ->
                        TaskCard(
                            task = card.task,
                            matchState = card.matchState,
                            matchPercent = card.matchPercent,
                            matchReason = card.matchReason,
                            hasCommuteSchedule = state.hasCommuteSchedule,
                            onClick = { onTaskClick(card.task.taskId) },
                            onQuickAccept = if (
                                card.matchState == DiscoverMatchState.MATCHED &&
                                !card.isOwnTask
                            ) {
                                { quickAcceptCard = card }
                            } else {
                                null
                            },
                            accepting = state.acceptingTaskId == card.task.taskId
                        )
                    }
                }
            }
        }
    }

    quickAcceptCard?.let { card ->
        val alerts = ComplianceChecker.checkTaskCompliance(card.task)
        val warningAlerts = alerts.filter { it.severity != ComplianceSeverity.INFO }.take(2)
        val isAccepting = state.acceptingTaskId == card.task.taskId
        AlertDialog(
            onDismissRequest = { if (!isAccepting) quickAcceptCard = null },
            title = { Text("Quick accept task?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.task.title.ifBlank { "Task" }, fontWeight = FontWeight.SemiBold)
                    Text("HK$ ${card.task.offeredPrice.toInt()}")
                    card.matchReason?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (warningAlerts.isNotEmpty()) {
                        warningAlerts.forEach { alert ->
                            Text(
                                "• ${alert.title}: ${alert.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "Please confirm the item still complies with customs rules before pickup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.quickAcceptTask(card.task.taskId) },
                    enabled = !isAccepting
                ) {
                    Text(if (isAccepting) "Accepting..." else "Accept task")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { quickAcceptCard = null },
                    enabled = !isAccepting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DiscoverToolbar(
    state: HomeUiState,
    mode: DiscoverMode,
    onModeChange: (DiscoverMode) -> Unit,
    onSelectCategory: (String?) -> Unit
) {
    Column {
        CategoryFilterRow(
            selected = state.selectedCategory,
            onSelect = onSelectCategory
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = mode == DiscoverMode.LIST,
                    onClick = { onModeChange(DiscoverMode.LIST) },
                    label = { Text("List") }
                )
            }
            item {
                FilterChip(
                    selected = mode == DiscoverMode.MAP,
                    onClick = { onModeChange(DiscoverMode.MAP) },
                    label = { Text("Map") }
                )
            }
        }
        if (state.hasCommuteSchedule) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Showing all open tasks. Stronger route overlap is ranked first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskMap(
    tasks: List<DiscoverTask>,
    hasCommuteSchedule: Boolean,
    corridors: List<List<GeoPoint>>,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!MapsConfig.isConfigured) {
        EmptyState(
            title = "Map view needs the shared Maps key",
            subtitle = "Add MAPS_API_KEY to local.properties to enable the team map. List view and pasted coordinates still work without it.",
            icon = "🗺️"
        )
        return
    }

    val taskPoints = tasks.mapNotNull { taskMarkerPoint(it.task) }
    val corridorPoints = corridors.flatten()
    val allPoints = taskPoints + corridorPoints
    val fallbackCenter = LatLng(22.5431, 114.0579)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            allPoints.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: fallbackCenter,
            10.5f
        )
    }

    LaunchedEffect(allPoints) {
        if (allPoints.size >= 2) {
            val bounds = LatLngBounds.Builder().apply {
                allPoints.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 160))
        } else if (allPoints.size == 1) {
            val p = allPoints.first()
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 12f)
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = false)
    ) {
        corridors.forEach { corridor ->
            if (corridor.size >= 2) {
                Polyline(
                    points = corridor.map { LatLng(it.latitude, it.longitude) },
                    color = MoviAccent.copy(alpha = 0.5f),
                    width = 10f
                )
                val origin = corridor.first()
                val destination = corridor.last()
                Marker(
                    state = MarkerState(position = LatLng(origin.latitude, origin.longitude)),
                    title = "My commute origin",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                Marker(
                    state = MarkerState(position = LatLng(destination.latitude, destination.longitude)),
                    title = "My commute destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }
        tasks.forEach { card ->
            val point = taskMarkerPoint(card.task) ?: return@forEach
            Marker(
                state = MarkerState(position = LatLng(point.latitude, point.longitude)),
                title = card.task.title.ifBlank { "Task" },
                snippet = mapSnippet(card, hasCommuteSchedule),
                onClick = {
                    onTaskClick(card.task.taskId)
                    true
                }
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    val categoryItems = listOf<Pair<String?, String>>(
        null to "All",
        TaskCategory.FOOD to "Food",
        TaskCategory.PARCEL to "Parcel",
        TaskCategory.DOCUMENT to "Document",
        TaskCategory.MEDICINE to "Medicine",
        TaskCategory.OTHER to "Other"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categoryItems.size) { i ->
            val (value, label) = categoryItems[i]
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    matchState: DiscoverMatchState,
    matchPercent: Int?,
    matchReason: String?,
    hasCommuteSchedule: Boolean,
    onClick: () -> Unit,
    onQuickAccept: (() -> Unit)? = null,
    accepting: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(categoryEmoji(task.category), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    task.title.ifEmpty { "(untitled)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "HK$ ${task.offeredPrice.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MoviAccent
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayablePlace(task.pickupAddress, "Shenzhen"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(horizontal = 4.dp)
                )
                Text(
                    displayablePlace(task.dropoffAddress, "Hong Kong"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.isUrgent) {
                        Pill("URGENT", MoviWarning)
                    }
                    when (matchState) {
                        DiscoverMatchState.MATCHED -> {
                            if (matchPercent != null) {
                                Pill("${matchPercent}% match", MoviAccent)
                            }
                        }
                        DiscoverMatchState.OWN -> Pill("MY TASK", MoviSecondary)
                        DiscoverMatchState.UNMATCHED -> {
                            if (matchPercent != null) {
                                Pill("${matchPercent}% match", MoviAccent)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    deadlineDateTimeLabel(task.requiredBefore),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (task.direction == "HK_TO_SZ") "HK → SZ" else "SZ → HK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val supportingText = when {
                !matchReason.isNullOrBlank() -> matchReason
                matchState == DiscoverMatchState.OWN -> "Your posted task stays visible here while carriers browse the market."
                hasCommuteSchedule -> "No usable route overlap with your commute yet."
                else -> null
            }
            if (!supportingText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (matchState == DiscoverMatchState.MATCHED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (onQuickAccept != null) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onQuickAccept, enabled = !accepting) {
                        Text(if (accepting) "Accepting..." else "Quick accept")
                    }
                }
            }
        }
    }
}

private fun mapSnippet(card: DiscoverTask, hasCommuteSchedule: Boolean): String = when (card.matchState) {
    DiscoverMatchState.MATCHED -> card.matchPercent?.let { "$it% match" } ?: "Matched task"
    DiscoverMatchState.OWN -> "My task"
    DiscoverMatchState.UNMATCHED -> if (hasCommuteSchedule) {
        card.matchPercent?.let { "$it% match" } ?: "No route overlap yet"
    } else {
        "HK$ ${card.task.offeredPrice.toInt()}"
    }
}

private fun taskMarkerPoint(task: Task): GeoPoint? =
    task.pickupLocation ?: task.dropoffLocation

internal fun categoryEmoji(category: String): String = when (category) {
    TaskCategory.FOOD -> "🍱"
    TaskCategory.PARCEL -> "📦"
    TaskCategory.DOCUMENT -> "📄"
    TaskCategory.MEDICINE -> "💊"
    else -> "🧾"
}

internal fun deadlineDateTimeLabel(ts: Timestamp?): String {
    if (ts == null) return "No deadline"
    return SimpleDateFormat("M月d号 E HH:mm", Locale.SIMPLIFIED_CHINESE).format(ts.toDate())
}
