package com.group2.movi.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.group2.movi.domain.model.EscrowStatus
import com.group2.movi.domain.model.TaskStatus
import com.group2.movi.ui.components.displayablePlace
import com.group2.movi.ui.components.InfoRow
import com.group2.movi.ui.components.LoadingBox
import com.group2.movi.ui.theme.MoviAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskProgressScreen(
    onBack: () -> Unit,
    onChat: () -> Unit,
    onConfirmDelivery: () -> Unit,
    vm: TaskProgressViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task progress", fontWeight = FontWeight.SemiBold) },
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
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            task == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found.")
            }
            else -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val uid = vm.currentUid
                val isRequester = uid != null && uid == task.requesterId
                val isCarrier = uid != null && uid == task.carrierId

                StatusStepper(status = task.status)

                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                        if (!task.itemPhotoUrl.isNullOrBlank()) {
                            Spacer(Modifier.size(12.dp))
                            AsyncImage(
                                model = task.itemPhotoUrl,
                                contentDescription = "Item photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        InfoRow("Category", task.category)
                        InfoRow("Pickup", displayablePlace(task.pickupAddress, "—"))
                        InfoRow("Drop-off", displayablePlace(task.dropoffAddress, "—"))
                        InfoRow("Price", "HK$ ${task.offeredPrice.toInt()}")
                        InfoRow("Status", task.status)
                        task.carrierName?.let { InfoRow("Carrier", it) }
                        if (task.description.isNotBlank()) {
                            Spacer(Modifier.size(8.dp))
                            Text("Description", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Escrow & verification", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.size(8.dp))
                        InfoRow("Escrow status", escrowLabel(task.escrowStatus))
                        InfoRow("Hold amount", "HK$ ${(task.finalPrice ?: task.offeredPrice).toInt()}")
                        when {
                            isCarrier && task.deliveryPin.isNotBlank() -> {
                                InfoRow("PIN", task.deliveryPin)
                                InfoRow("QR token", task.deliveryQrToken)
                                Text(
                                    "Show either code at handoff. Payment stays frozen until the requester confirms one of them.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            isRequester && task.status == TaskStatus.DELIVERED -> {
                                Text(
                                    "Ask the carrier for the 6-digit PIN or QR token before releasing payment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            task.escrowStatus == EscrowStatus.RELEASED -> {
                                Text(
                                    "Payment has been released to the carrier.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviAccent
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Open chat") }

                when {
                    isCarrier && task.status == TaskStatus.ACCEPTED -> {
                        Button(
                            onClick = vm::markPickedUp,
                            enabled = !state.acting,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Mark as picked up", fontWeight = FontWeight.SemiBold) }
                    }
                    isCarrier && task.status == TaskStatus.PICKED_UP -> {
                        Button(
                            onClick = vm::markDelivered,
                            enabled = !state.acting,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Mark as delivered", fontWeight = FontWeight.SemiBold) }
                    }
                    isRequester && task.status == TaskStatus.DELIVERED -> {
                        Button(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Confirm receipt", fontWeight = FontWeight.SemiBold) }
                    }
                    isRequester && task.status == TaskStatus.CONFIRMED && !task.requesterReviewed -> {
                        OutlinedButton(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Leave carrier review") }
                    }
                    isCarrier && (task.status == TaskStatus.DELIVERED || task.status == TaskStatus.CONFIRMED) && !task.carrierReviewed -> {
                        OutlinedButton(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Review requester") }
                    }
                    isRequester && task.status == TaskStatus.OPEN -> {
                        OutlinedButton(
                            onClick = vm::cancelTask,
                            enabled = !state.acting,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Cancel task", color = MaterialTheme.colorScheme.error) }
                    }
                }

                state.success?.let {
                    Text(
                        it,
                        color = MoviAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                state.error?.let {
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

private fun escrowLabel(status: String): String = when (status) {
    EscrowStatus.HELD -> "Held"
    EscrowStatus.RELEASED -> "Released"
    EscrowStatus.DISPUTED -> "Disputed"
    else -> "Pending"
}

@Composable
private fun StatusStepper(status: String) {
    val steps = listOf(
        TaskStatus.OPEN to "Posted",
        TaskStatus.ACCEPTED to "Accepted",
        TaskStatus.PICKED_UP to "Picked up",
        TaskStatus.DELIVERED to "Delivered",
        TaskStatus.CONFIRMED to "Confirmed"
    )
    val currentIndex = steps.indexOfFirst { it.first == status }.let { if (it < 0) 0 else it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { i, (_, label) ->
            val done = i <= currentIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (done) MoviAccent else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    else Text("${i + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal)
            }
            if (i < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .height(2.dp)
                        .background(if (i < currentIndex) MoviAccent else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}
