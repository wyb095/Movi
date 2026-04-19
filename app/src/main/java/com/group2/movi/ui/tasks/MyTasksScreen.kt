package com.group2.movi.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.components.LoadingBox
import com.group2.movi.ui.components.Pill
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviSecondary
import com.group2.movi.ui.theme.MoviWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    onTaskClick: (String) -> Unit,
    vm: MyTasksViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My tasks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("As requester") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("As carrier") })
            }

            val list = if (tab == 0) state.asRequester else state.asCarrier

            when {
                state.loading -> LoadingBox()
                list.isEmpty() -> EmptyState(
                    title = if (tab == 0) "You haven't posted any tasks" else "You haven't accepted any tasks",
                    subtitle = if (tab == 0) "Tap the Post tab to create your first task." else "Browse the Home feed to find tasks to accept."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.taskId }) { task ->
                        MyTaskRow(
                            task = task,
                            matchCount = if (tab == 0) state.requesterMatchCounts[task.taskId] else null,
                            onClick = { onTaskClick(task.taskId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyTaskRow(task: Task, matchCount: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title.ifBlank { "(untitled)" }, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(2.dp))
                Text(
                    "${task.category} · HK$ ${task.offeredPrice.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.status == TaskStatus.OPEN && matchCount != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (matchCount > 0) "$matchCount matching commuters right now"
                        else "No matching commuters yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (matchCount > 0) MoviAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Pill(
                    text = statusLabel(task.status),
                    color = statusColor(task.status)
                )
                if (task.status == TaskStatus.OPEN && (matchCount ?: 0) > 0) {
                    Spacer(Modifier.height(4.dp))
                    Pill("$matchCount matches", MoviAccent)
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    TaskStatus.OPEN -> "OPEN"
    TaskStatus.ACCEPTED -> "ACCEPTED"
    TaskStatus.PICKED_UP -> "IN TRANSIT"
    TaskStatus.DELIVERED -> "DELIVERED"
    TaskStatus.CONFIRMED -> "DONE"
    TaskStatus.CANCELLED -> "CANCELLED"
    TaskStatus.DISPUTED -> "DISPUTE"
    else -> status
}

private fun statusColor(status: String): Color = when (status) {
    TaskStatus.OPEN -> MoviWarning
    TaskStatus.ACCEPTED, TaskStatus.PICKED_UP -> MoviSecondary
    TaskStatus.DELIVERED, TaskStatus.CONFIRMED -> MoviAccent
    TaskStatus.CANCELLED -> Color(0xFF757575)
    TaskStatus.DISPUTED -> Color(0xFFC0392B)
    else -> MoviSecondary
}
