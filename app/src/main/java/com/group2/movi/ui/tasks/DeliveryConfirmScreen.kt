package com.group2.movi.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.group2.movi.domain.model.TaskStatus
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryConfirmScreen(
    onDone: () -> Unit,
    vm: DeliveryConfirmViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val task = state.task
    val currentUid = vm.currentUid
    val isRequester = task != null && currentUid == task.requesterId
    val needsVerification = isRequester && task?.status == TaskStatus.DELIVERED
    val reviewSubmitted = if (isRequester) task?.requesterReviewed == true else task?.carrierReviewed == true
    val reviewTarget = when {
        isRequester -> task?.carrierName ?: "carrier"
        else -> task?.requesterName ?: "requester"
    }

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            needsVerification -> "Confirm delivery"
                            isRequester -> "Review carrier"
                            else -> "Review requester"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                when {
                    needsVerification -> "Verify the handoff and release payment"
                    isRequester -> "Leave feedback for your carrier"
                    else -> "Leave feedback for the requester"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    needsVerification -> "Enter the carrier's 6-digit PIN or QR token. Escrow releases only after a valid handoff code."
                    isRequester -> "Delivery is already confirmed. You can still leave a review."
                    else -> "Once the item is delivered, carriers can also review requesters."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            task?.let {
                Text(
                    "Escrow amount: HK$ ${(it.finalPrice ?: it.offeredPrice).toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (needsVerification) {
                OutlinedTextField(
                    value = state.verificationCode,
                    onValueChange = vm::setVerificationCode,
                    label = { Text("PIN or QR token") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!reviewSubmitted) {
                Text("Rate $reviewTarget", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { i ->
                        val filled = i <= state.rating
                        androidx.compose.material3.Icon(
                            imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (filled) MoviWarning else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp).clickable { vm.setRating(i) }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.comment,
                    onValueChange = vm::setComment,
                    label = { Text("Leave a comment (optional, max 300)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    supportingText = { Text("${state.comment.length}/300") }
                )
            } else {
                Text(
                    "Your review has already been submitted for this task.",
                    color = MoviAccent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.success?.let { Text(it, color = MoviAccent) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = vm::submit,
                enabled = !state.submitting && task != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(
                        when {
                            needsVerification && !reviewSubmitted -> "Confirm, release payment, and review"
                            needsVerification -> "Confirm and release payment"
                            else -> "Submit review"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.size(4.dp))
            OutlinedButton(
                onClick = vm::raiseDispute,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Something's wrong - raise a dispute", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
