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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.group2.movi.domain.model.CrossingPort
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskCategory
import com.group2.movi.domain.model.taskMarkerPoint
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.components.LoadingBox
import com.group2.movi.ui.components.Pill
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviWarning
import java.util.concurrent.TimeUnit

private enum class DiscoverMode { LIST, MAP }

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    onTaskClick: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var mode by rememberSaveable { mutableStateOf(DiscoverMode.LIST) }

    Scaffold(
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
                onSelectCategory = vm::selectCategory,
                onDetourChange = vm::setMaxDetourMinutes
            )

            when {
                state.loading -> LoadingBox()
                state.tasks.isEmpty() -> EmptyState(
                    title = if (state.hasCommuteSchedule) {
                        "No tasks fit your commute right now"
                    } else {
                        "No open tasks right now"
                    },
                    subtitle = if (state.hasCommuteSchedule) {
                        "Try widening your detour filter or add more commute entries in Schedule."
                    } else {
                        "Pull down to refresh or check back soon."
                    }
                )
                mode == DiscoverMode.MAP -> TaskMap(
                    tasks = state.tasks,
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
                            matchReason = card.match?.reason,
                            detourMinutes = card.match?.detourMinutes,
                            onClick = { onTaskClick(card.task.taskId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverToolbar(
    state: HomeUiState,
    mode: DiscoverMode,
    onModeChange: (DiscoverMode) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onDetourChange: (Float) -> Unit
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
                    "Smart matching is on. Showing tasks that fit your commute and deadline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Max detour: ${state.maxDetourMinutes.toInt()} min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = state.maxDetourMinutes,
                    onValueChange = onDetourChange,
                    valueRange = 10f..120f
                )
            }
        }
    }
}

@Composable
private fun TaskMap(
    tasks: List<DiscoverTask>,
    corridors: List<List<GeoPoint>>,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskPoints = tasks.map { taskMarkerPoint(it.task) }
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
            val point = taskMarkerPoint(card.task)
            Marker(
                state = MarkerState(position = LatLng(point.latitude, point.longitude)),
                title = card.task.title.ifBlank { "Task" },
                snippet = card.match?.reason ?: "HK$ ${card.task.offeredPrice.toInt()}",
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
    matchReason: String?,
    detourMinutes: Int?,
    onClick: () -> Unit
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
                    shortCity(task.pickupAddress, "Shenzhen"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(horizontal = 4.dp)
                )
                Text(
                    shortCity(task.dropoffAddress, "Hong Kong"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                if (task.isUrgent) {
                    Pill("URGENT", MoviWarning)
                } else if (detourMinutes != null) {
                    Pill("${detourMinutes}m detour", MoviAccent)
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
                    timeLeftLabel(task.requiredBefore),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "via ${CrossingPort.label(task.crossingPort)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!matchReason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    matchReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

internal fun categoryEmoji(category: String): String = when (category) {
    TaskCategory.FOOD -> "🍱"
    TaskCategory.PARCEL -> "📦"
    TaskCategory.DOCUMENT -> "📄"
    TaskCategory.MEDICINE -> "💊"
    else -> "🧾"
}

private fun shortCity(address: String, fallback: String): String {
    if (address.isBlank()) return fallback
    return address.split(",").firstOrNull()?.trim()?.take(24) ?: fallback
}

internal fun timeLeftLabel(ts: Timestamp?): String {
    if (ts == null) return "no deadline"
    val ms = ts.toDate().time - System.currentTimeMillis()
    if (ms <= 0) return "expired"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return when {
        h >= 24 -> "${h / 24}d left"
        h >= 1 -> "${h}h ${m}m left"
        else -> "${m}m left"
    }
}
