package com.group2.movi.ui.post

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.group2.movi.domain.model.CrossingPort
import com.group2.movi.domain.model.TaskCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTaskScreen(
    onPosted: () -> Unit,
    vm: PostTaskViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.submitted) { if (state.submitted) onPosted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a task · Step ${state.step + 1} of 4", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepProgress(current = state.step, total = 4)

            when (state.step) {
                0 -> Step1Category(state, vm)
                1 -> Step2Locations(state, vm)
                2 -> Step3PortTiming(state, vm)
                3 -> Step4PriceReview(state, vm)
            }

            state.submitError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.size(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = vm::back,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back") }
                }
                Button(
                    onClick = { if (state.step == 3) vm.submit() else vm.next() },
                    enabled = state.stepValid && !state.submitting && !state.uploadingPhoto,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (state.submitting) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text(if (state.step == 3) "Post task" else "Next", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StepProgress(current: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun Step1Category(state: PostFormState, vm: PostTaskViewModel) {
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> vm.selectPhoto(uri) }

    Text("What do you need delivered?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TaskCategory.ALL.forEach { cat ->
            FilterChip(
                selected = state.category == cat,
                onClick = { vm.setCategory(cat) },
                label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }

    OutlinedTextField(
        value = state.title,
        onValueChange = vm::setTitle,
        label = { Text("Title (5–80 chars)") },
        supportingText = { Text("${state.title.length}/80") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = state.description,
        onValueChange = vm::setDescription,
        label = { Text("Description (optional, max 500)") },
        supportingText = { Text("${state.description.length}/500") },
        modifier = Modifier.fillMaxWidth().height(120.dp)
    )

    Text("Item photo (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { pickImage.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        when {
            state.uploadingPhoto -> CircularProgressIndicator()
            state.uploadedPhotoUrl != null -> AsyncImage(
                model = state.uploadedPhotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            state.localPhotoUri != null -> AsyncImage(
                model = state.localPhotoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Tap to add a photo", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Step2Locations(state: PostFormState, vm: PostTaskViewModel) {
    Text("Where is it going?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        "Enter pickup and drop-off addresses. Paste a Google Maps link or lat,lng to enable map discovery and smarter matching.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = state.pickupAddress,
        onValueChange = vm::setPickupAddress,
        label = { Text("Pickup address") },
        modifier = Modifier.fillMaxWidth()
    )
    state.pickupLocation?.let {
        Text(
            "Pickup coordinates detected: %.4f, %.4f".format(it.latitude, it.longitude),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
    OutlinedTextField(
        value = state.dropoffAddress,
        onValueChange = vm::setDropoffAddress,
        label = { Text("Drop-off address") },
        modifier = Modifier.fillMaxWidth()
    )
    state.dropoffLocation?.let {
        Text(
            "Drop-off coordinates detected: %.4f, %.4f".format(it.latitude, it.longitude),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Step3PortTiming(state: PostFormState, vm: PostTaskViewModel) {
    Text("Crossing port & deadline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    Text("Port", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CrossingPort.ALL.forEach { port ->
            PortRow(
                label = CrossingPort.label(port),
                selected = state.crossingPort == port,
                onClick = { vm.setPort(port) }
            )
        }
    }

    Spacer(Modifier.size(4.dp))
    Text("Direction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.direction == "HK_TO_SZ",
            onClick = { vm.setDirection("HK_TO_SZ") },
            label = { Text("HK → Shenzhen") }
        )
        FilterChip(
            selected = state.direction == "SZ_TO_HK",
            onClick = { vm.setDirection("SZ_TO_HK") },
            label = { Text("Shenzhen → HK") }
        )
    }

    Spacer(Modifier.size(4.dp))
    DeadlinePicker(
        deadlineMs = state.deadlineEpochMs,
        onSelect = { vm.setDeadline(it) }
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = state.isUrgent, onCheckedChange = vm::setUrgent)
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Mark as urgent", fontWeight = FontWeight.SemiBold)
            Text(
                "Urgent tasks appear first in carriers' feeds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DeadlinePicker(deadlineMs: Long?, onSelect: (Long) -> Unit) {
    val ctx = LocalContext.current
    val sdf = remember { SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()) }
    val label = deadlineMs?.let { sdf.format(Date(it)) } ?: "Pick a deadline"
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(ctx, { _, y, m, d ->
                TimePickerDialog(ctx, { _, h, min ->
                    val c = Calendar.getInstance()
                    c.set(y, m, d, h, min, 0)
                    onSelect(c.timeInMillis)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(label)
    }
    Text(
        "Deadline must be at least 2 hours from now.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Step4PriceReview(state: PostFormState, vm: PostTaskViewModel) {
    Text("Price & review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    OutlinedTextField(
        value = state.priceHkd,
        onValueChange = vm::setPrice,
        label = { Text("Price offered (HKD, min 5)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Summary", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(8.dp))
            Text("• ${state.category}: ${state.title}")
            Text("• From: ${state.pickupAddress}")
            Text("• To: ${state.dropoffAddress}")
            if (state.pickupLocation != null && state.dropoffLocation != null) {
                Text("• Geo matching: enabled")
            }
            Text("• Via: ${CrossingPort.label(state.crossingPort)}")
            Text("• Direction: ${if (state.direction == "HK_TO_SZ") "HK → Shenzhen" else "Shenzhen → HK"}")
            state.deadlineEpochMs?.let {
                val sdf = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
                Text("• Deadline: ${sdf.format(Date(it))}")
            }
            if (state.isUrgent) Text("• Urgent: yes")
            Text("• Offered: HK$ ${state.priceHkd.ifBlank { "—" }}")
        }
    }

    Row(verticalAlignment = Alignment.Top) {
        Checkbox(checked = state.customsConfirmed, onCheckedChange = vm::setCustoms)
        Spacer(Modifier.width(8.dp))
        Text(
            "I confirm this item is not a prohibited good (no firearms, fresh meat, live animals, counterfeit goods, prescription medication without docs, or cash over HKD 20,000 / RMB 20,000) and complies with customs rules.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
