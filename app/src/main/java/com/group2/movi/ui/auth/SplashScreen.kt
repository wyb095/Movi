package com.group2.movi.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@androidx.compose.runtime.Composable
fun SplashScreen(
    onLoggedInVerified: () -> Unit,
    onLoggedInUnverified: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            onNotLoggedIn()
        } else {
            runCatching { user.reload().await() }
            if (user.isEmailVerified) onLoggedInVerified() else onLoggedInUnverified()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("MOVI", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mobility and Movement",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )
        }
    }
}
