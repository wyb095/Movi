package com.group2.movi.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.group2.movi.domain.model.CrossingPort
import com.group2.movi.domain.model.EscrowStatus
import com.group2.movi.domain.model.TaskStatus
import com.group2.movi.ui.components.InfoRow
import com.group2.movi.ui.components.LoadingBox
import com.group2.movi.ui.components.Pill
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onAccepted: () -> Unit,
    onChat: () -> Unit,
    vm: TaskDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showCustomsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task details", fontWeight = FontWeight.SemiBold) },
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
        val task = state.task
        val myMatch = state.myMatch
        when {
            state.loading -> LoadingBox(modifier = Modifier.padding(padding))
            task == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found.")
            }
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header card with photo + price
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(categoryEmoji(task.category), fontSize = 40.sp)
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(task.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("HK$ ${task.offeredPrice.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MoviAccent)
                                if (task.isUrgent) {
                                    Spacer(Modifier.size(4.dp))
                                    Pill("URGENT", MoviWarning)
                                }
                            }
                        }
                        if (!task.itemPhotoUrl.isNullOrBlank()) {
                            Spacer(Modifier.size(12.dp))
                            AsyncImage(
                                model = task.itemPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }

                // Description
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(6.dp))
                        Text(task.description.ifBlank { "(no description)" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Logistics
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Logistics", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(8.dp))
                        InfoRow("Pickup", task.pickupAddress.ifBlank { "—" })
                        InfoRow("Drop-off", task.dropoffAddress.ifBlank { "—" })
                        InfoRow("Port", CrossingPort.label(task.crossingPort))
                        InfoRow("Direction", if (task.direction == "HK_TO_SZ") "HK → Shenzhen" else "Shenzhen → HK")
                        InfoRow("Deadline", timeLeftLabel(task.requiredBefore))
                    }
                }

                // Requester
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Requester", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(8.dp))
                        InfoRow("Name", task.requesterName.ifBlank { "Anonymous" })
                        InfoRow("Rating", "★ ${"%.1f".format(task.requesterRating)}")
                    }
                }

                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Route match", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(8.dp))
                        when {
                            vm.isRequester -> {
                                Text(
                                    if (state.matchingCarrierCount > 0) {
                                        "${state.matchingCarrierCount} carriers currently have at least 60% route overlap for this task."
                                    } else {
                                        "No carrier has reached the 60% route-overlap threshold yet."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            myMatch != null -> {
                                Text(
                                    myMatch.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "Best matching commute: ${myMatch.scheduleSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            state.hasCommuteRoute -> {
                                Text(
                                    "This task is currently below the 60% overlap threshold for your saved commute routes.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            else -> Text(
                                "Add a commute route with origin and destination to unlock route-overlap ranking.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Escrow & handoff", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(8.dp))
                        InfoRow("Amount", "HK$ ${task.offeredPrice.toInt()}")
                        InfoRow(
                            "Escrow",
                            when (task.escrowStatus) {
                                EscrowStatus.HELD -> "Held"
                                EscrowStatus.RELEASED -> "Released"
                                EscrowStatus.DISPUTED -> "Disputed"
                                else -> "Will be held when a carrier accepts"
                            }
                        )
                        Text(
                            "Movi now freezes the payment on accept and releases it only after PIN or QR-token confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.size(12.dp))

                // Actions
                when {
                    task.status != TaskStatus.OPEN -> {
                        OutlinedButton(
                            onClick = onChat,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Open chat") }
                    }
                    vm.isRequester -> {
                        Text(
                            "This is your own task. Waiting for a carrier to accept.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Button(
                            onClick = { showCustomsDialog = true },
                            enabled = !state.accepting,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (state.accepting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Accept & hold payment", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        state.error?.let {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomsDialog = false },
            title = { Text("Customs compliance") },
            text = {
                Text(
                    "By accepting this task, you confirm the item is not prohibited (firearms, fresh meat, live animals, counterfeit goods, prescription medication without documentation, or cash over HKD 20,000 / RMB 20,000).\n\nMovi is a shared-commute platform, not a smuggling service."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCustomsDialog = false
                    vm.accept(onAccepted)
                }) { Text("Confirm, hold escrow, accept") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomsDialog = false }) { Text("Cancel") }
            }
        )
    }
}
