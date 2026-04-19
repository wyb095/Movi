package com.group2.movi.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EmailVerifyScreen(
    onVerified: () -> Unit,
    onLogout: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📧", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Verify your email", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a verification link to your email. Please click it, then tap the button below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { vm.checkVerified(onVerified) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("I have verified my email", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.resendVerification() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Resend verification email")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            vm.logout()
            onLogout()
        }) {
            Text("Use a different account")
        }
    }
}
