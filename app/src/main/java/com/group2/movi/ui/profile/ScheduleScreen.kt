package com.group2.movi.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.ORDERED_DAYS_OF_WEEK
import com.group2.movi.domain.model.WEEKEND_DAYS
import com.group2.movi.domain.model.WORKDAY_DAYS
import com.group2.movi.domain.model.extractGeoPoint
import com.group2.movi.domain.model.normalizeDaysOfWeek
import com.group2.movi.domain.model.scheduleDaySummary
import com.group2.movi.domain.model.shortDayLabel
import com.group2.movi.ui.components.displayablePlace
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.components.PlacePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My commute schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add schedule")
            }
        }
    ) { padding ->
        val list = user?.commuteSchedule ?: emptyList()
        if (list.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                EmptyState(
                    title = "No schedule yet",
                    subtitle = "Add your regular commute so we can match you to nearby tasks.",
                    icon = "🗓️"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { entry ->
                    ScheduleCard(entry = entry, onDelete = { vm.removeSchedule(entry) })
                }
            }
        }
    }

    if (showAdd) {
        AddScheduleDialog(
            onDismiss = { showAdd = false },
            onAdd = { entry ->
                vm.addSchedule(entry)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ScheduleCard(entry: CommuteEntry, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.scheduleDaySummary(), fontWeight = FontWeight.SemiBold)
                Text(
                    if (entry.direction == "HK_TO_SZ") "HK → Shenzhen" else "Shenzhen → HK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.originLocation != null && entry.destinationLocation != null) {
                    Spacer(Modifier.height(4.dp))
                    val fromFallback = entry.originAddress.ifBlank {
                        "%.3f, %.3f".format(entry.originLocation.latitude, entry.originLocation.longitude)
                    }
                    val toFallback = entry.destinationAddress.ifBlank {
                        "%.3f, %.3f".format(entry.destinationLocation.latitude, entry.destinationLocation.longitude)
                    }
                    Text(
                        "From: ${displayablePlace(entry.originAddress, fromFallback)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Text(
                        "To: ${displayablePlace(entry.destinationAddress, toFallback)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(onDismiss: () -> Unit, onAdd: (CommuteEntry) -> Unit) {
    var selectedDays by remember { mutableStateOf(WORKDAY_DAYS) }
    var direction by remember { mutableStateOf("SZ_TO_HK") }
    var originAddress by remember { mutableStateOf("") }
    var originLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationAddress by remember { mutableStateOf("") }
    var destinationLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val locationsValid = originLocation != null && destinationLocation != null
    val hasWorkdays = WORKDAY_DAYS.all(selectedDays::contains)
    val hasWeekend = WEEKEND_DAYS.all(selectedDays::contains)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add commute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose the days this route usually applies. Workdays and Weekend are shortcuts only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = hasWorkdays,
                            onClick = { selectedDays = togglePresetDays(selectedDays, WORKDAY_DAYS) },
                            label = { Text("Workdays") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hasWeekend,
                            onClick = { selectedDays = togglePresetDays(selectedDays, WEEKEND_DAYS) },
                            label = { Text("Weekend") }
                        )
                    }
                }
                DaySelectorRow(
                    days = ORDERED_DAYS_OF_WEEK.take(4),
                    selectedDays = selectedDays,
                    onToggle = { day -> selectedDays = toggleSpecificDay(selectedDays, day) }
                )
                DaySelectorRow(
                    days = ORDERED_DAYS_OF_WEEK.drop(4),
                    selectedDays = selectedDays,
                    onToggle = { day -> selectedDays = toggleSpecificDay(selectedDays, day) }
                )
                if (selectedDays.isEmpty()) {
                    Text(
                        "Pick at least one day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DropdownPicker(
                    label = "Direction",
                    options = listOf("HK_TO_SZ", "SZ_TO_HK"),
                    displayOf = { if (it == "HK_TO_SZ") "HK → Shenzhen" else "Shenzhen → HK" },
                    selected = direction,
                    onSelect = { direction = it }
                )
                PlacePickerField(
                    label = "Origin (home / office)",
                    value = originAddress,
                    onPlaceSelected = { addr, loc, _ ->
                        originAddress = addr
                        originLocation = loc
                    },
                    onPasteFallback = { raw ->
                        val geo = extractGeoPoint(raw)
                        if (geo != null) {
                            originLocation = geo
                            originAddress = "%.4f, %.4f".format(geo.latitude, geo.longitude)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                PlacePickerField(
                    label = "Destination (office / home)",
                    value = destinationAddress,
                    onPlaceSelected = { addr, loc, _ ->
                        destinationAddress = addr
                        destinationLocation = loc
                    },
                    onPasteFallback = { raw ->
                        val geo = extractGeoPoint(raw)
                        if (geo != null) {
                            destinationLocation = geo
                            destinationAddress = "%.4f, %.4f".format(geo.latitude, geo.longitude)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!locationsValid) {
                    Text(
                        "Pick both origin and destination so Movi can match tasks along your route.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        CommuteEntry(
                            daysOfWeek = selectedDays,
                            direction = direction,
                            originLocation = originLocation,
                            originAddress = originAddress,
                            destinationLocation = destinationLocation,
                            destinationAddress = destinationAddress
                        )
                    )
                },
                enabled = selectedDays.isNotEmpty() && locationsValid
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DaySelectorRow(
    days: List<String>,
    selectedDays: List<String>,
    onToggle: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onToggle(day) },
                label = { Text(shortDayLabel(day)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownPicker(
    label: String,
    options: List<T>,
    selected: T,
    displayOf: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(displayOf(opt)) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

internal fun toggleSpecificDay(current: List<String>, day: String): List<String> {
    return normalizeDaysOfWeek(
        if (day in current) current - day else current + day
    )
}

internal fun togglePresetDays(current: List<String>, presetDays: List<String>): List<String> {
    val presetSelected = presetDays.all(current::contains)
    return normalizeDaysOfWeek(
        if (presetSelected) current - presetDays.toSet() else current + presetDays
    )
}
