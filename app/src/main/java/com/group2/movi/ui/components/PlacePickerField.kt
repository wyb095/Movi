package com.group2.movi.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.domain.model.extractGeoPoint

@Composable
fun PlacePickerField(
    label: String,
    value: String,
    onPlaceSelected: (address: String, loc: GeoPoint, placeId: String) -> Unit,
    onPasteFallback: (raw: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPasteDialog by rememberSaveable { mutableStateOf(false) }
    var pastedValue by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    val fields = remember {
        listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
    }

    LaunchedEffect(value) {
        if (value.isNotBlank()) feedback = null
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data ?: return@rememberLauncherForActivityResult
                val place = Autocomplete.getPlaceFromIntent(data)
                val latLng = place.latLng
                if (latLng == null) {
                    feedback = "We couldn't read coordinates from that place."
                    return@rememberLauncherForActivityResult
                }
                feedback = null
                onPlaceSelected(
                    place.address ?: place.name ?: "${latLng.latitude}, ${latLng.longitude}",
                    GeoPoint(latLng.latitude, latLng.longitude),
                    place.id.orEmpty()
                )
            }

            AutocompleteActivity.RESULT_ERROR -> {
                val data = result.data
                feedback = if (data != null) {
                    Autocomplete.getStatusFromIntent(data).statusMessage ?: "Couldn't open place search."
                } else {
                    "Couldn't open place search."
                }
            }
        }
    }

    val launchAutocomplete = {
        if (!Places.isInitialized()) {
            feedback = "Place search needs a configured MAPS_API_KEY."
        } else {
            feedback = null
            launcher.launch(
                Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountries(listOf("HK", "CN"))
                    .build(context)
            )
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = launchAutocomplete)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = "Search a place in Hong Kong or Shenzhen",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap to open Google Places Autocomplete",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        AssistChip(
                            onClick = launchAutocomplete,
                            label = {
                                Text(
                                    text = value,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        TextButton(
                            onClick = launchAutocomplete,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("Change")
                        }
                    }
                }
            }

            OutlinedButton(onClick = { showPasteDialog = true }) {
                Text("Paste link")
            }
        }

        feedback?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showPasteDialog) {
        val parsedPoint = extractGeoPoint(pastedValue)
        AlertDialog(
            onDismissRequest = {
                showPasteDialog = false
                pastedValue = ""
            },
            title = { Text("Paste Google Maps link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pastedValue,
                        onValueChange = { pastedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Maps link or lat,lng") },
                        supportingText = {
                            Text("Supports links like https://maps.google.com/?q=22.54,114.10")
                        }
                    )
                    Text(
                        text = when {
                            pastedValue.isBlank() -> "Paste a Google Maps link as a fallback when search isn't available."
                            parsedPoint != null -> {
                                "Coordinates detected: %.4f, %.4f".format(
                                    parsedPoint.latitude,
                                    parsedPoint.longitude
                                )
                            }

                            else -> "Couldn't detect coordinates. Paste a Maps link or raw lat,lng."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (parsedPoint != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPasteFallback(pastedValue.trim())
                        showPasteDialog = false
                        pastedValue = ""
                        feedback = null
                    },
                    enabled = parsedPoint != null
                ) {
                    Text("Use link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasteDialog = false
                        pastedValue = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
