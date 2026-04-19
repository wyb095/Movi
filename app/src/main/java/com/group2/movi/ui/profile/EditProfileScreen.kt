package com.group2.movi.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsState()
    val editState by vm.editProfileState.collectAsState()
    var name by remember { mutableStateOf("") }

    LaunchedEffect(user) { user?.let { if (name.isEmpty()) name = it.displayName } }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(vm::uploadPhoto) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit profile") },
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
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = !editState.uploadingPhoto && !editState.savingDisplayName) {
                        pickImage.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    editState.uploadingPhoto -> CircularProgressIndicator()
                    !user?.profilePhotoUrl.isNullOrBlank() -> AsyncImage(
                        model = user!!.profilePhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    else -> Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text("Tap the photo to upload", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            editState.infoMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            editState.errorMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    vm.clearEditProfileFeedback()
                },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(4.dp))
            Button(
                onClick = { vm.updateDisplayName(name, onBack) },
                enabled = name.trim().length >= 2 && !editState.savingDisplayName && !editState.uploadingPhoto,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (editState.savingDisplayName) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
