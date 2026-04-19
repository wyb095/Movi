package com.group2.movi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.group2.movi.ui.navigation.MoviNavHost
import com.group2.movi.ui.theme.MoviTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoviTheme {
                NotificationPermissionRequester()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MoviNavHost(navController = navController)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var askedThisSession by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    LaunchedEffect(Unit) {
        if (askedThisSession) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            askedThisSession = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
