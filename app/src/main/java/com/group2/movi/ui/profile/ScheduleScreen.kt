package com.group2.movi.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
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
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.CrossingPort
import com.group2.movi.ui.components.EmptyState
import java.time.LocalTime

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
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.dayOfWeek} · ${entry.departureTime}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${CrossingPort.label(entry.port)} · ${if (entry.direction == "HK_TO_SZ") "HK → SZ" else "SZ → HK"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    var day by remember { mutableStateOf(days.first()) }
    var time by remember { mutableStateOf("18:00") }
    var port by remember { mutableStateOf(CrossingPort.FUTIAN) }
    var direction by remember { mutableStateOf("SZ_TO_HK") }
    val timeValid = isValidDepartureTime(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add commute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownPicker(label = "Day", options = days, selected = day, onSelect = { day = it })
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.take(5) },
                    label = { Text("Departure time (HH:mm)") },
                    isError = !timeValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (!timeValid) {
                            Text("Use 24-hour time like 08:30 or 18:00.")
                        }
                    }
                )
                DropdownPicker(
                    label = "Port",
                    options = CrossingPort.ALL,
                    displayOf = { CrossingPort.label(it) },
                    selected = port,
                    onSelect = { port = it }
                )
                DropdownPicker(
                    label = "Direction",
                    options = listOf("HK_TO_SZ", "SZ_TO_HK"),
                    displayOf = { if (it == "HK_TO_SZ") "HK → Shenzhen" else "Shenzhen → HK" },
                    selected = direction,
                    onSelect = { direction = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(CommuteEntry(day, time, port, direction))
            }, enabled = timeValid) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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

internal fun isValidDepartureTime(value: String): Boolean {
    return runCatching { LocalTime.parse(value) }.isSuccess
}
