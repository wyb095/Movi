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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.group2.movi.domain.model.Task
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.theme.MoviAccent
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val tasks by vm.myCarrierTasks.collectAsState()
    val (week, month, all) = vm.earnings(tasks)
    val completed = tasks.filter(::countsTowardsEarnings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EarningsCard("This week", week, Modifier.weight(1f))
                EarningsCard("This month", month, Modifier.weight(1f))
                EarningsCard("All time", all, Modifier.weight(1f))
            }
            Text("Completed tasks", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)

            if (completed.isEmpty()) {
                Spacer(Modifier.padding(16.dp))
                EmptyState(
                    title = "No earnings yet",
                    subtitle = "Accept and complete tasks to start earning.",
                    icon = "💰"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(completed, key = { it.taskId }) { task ->
                        EarningsRow(task)
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsCard(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HK$ ${amount.toInt()}", color = MoviAccent, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EarningsRow(task: Task) {
    val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
    val earned = earningAmount(task).toInt()
    Card(shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title.ifBlank { "(untitled)" }, fontWeight = FontWeight.Medium)
                Text(
                    earningTimestampMillis(task)?.let { sdf.format(Date(it)) } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("+HK$ $earned", color = MoviAccent, fontWeight = FontWeight.Bold)
        }
    }
}
