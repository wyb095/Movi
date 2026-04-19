package com.group2.movi.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.firestore.GeoPoint

/**
 * Location input with two modes:
 *  - Tap body → Places Autocomplete (returns placeId + formatted address + lat/lng).
 *  - Tap 🔗 → paste a Google Maps URL / raw lat,lng as fallback; parsed via extractGeoPoint().
 */
@Composable
fun PlacePickerField(
    label: String,
    value: String,
    onPlaceSelected: (address: String, loc: GeoPoint, placeId: String) -> Unit,
    onPasteFallback: (raw: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val place = Autocomplete.getPlaceFromIntent(data)
            val latLng = place.latLng ?: return@rememberLauncherForActivityResult
            val addr = place.address ?: place.name.orEmpty()
            onPlaceSelected(addr, GeoPoint(latLng.latitude, latLng.longitude), place.id.orEmpty())
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable {
                        val fields = listOf(
                            Place.Field.ID,
                            Place.Field.NAME,
                            Place.Field.ADDRESS,
                            Place.Field.LAT_LNG
                        )
                        val intent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.OVERLAY,
                            fields
                        )
                            .setCountries(listOf("HK", "CN"))
                            .build(context)
                        launcher.launch(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (value.isBlank()) Icons.Filled.LocationOn else Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (value.isBlank()) "Search address…" else value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(onClick = { showPasteDialog = true }) {
                Icon(Icons.Filled.Link, contentDescription = "Paste Google Maps link")
            }
        }
    }

    if (showPasteDialog) {
        PasteLinkDialog(
            onDismiss = { showPasteDialog = false },
            onSubmit = { raw ->
                onPasteFallback(raw)
                showPasteDialog = false
            }
        )
    }
}

@Composable
private fun PasteLinkDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Google Maps link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste a Google Maps URL, or raw coordinates like 22.54, 114.10.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Link or coordinates") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) { Text("Use") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
