package com.group2.movi.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state == AuthUiState.NeedsVerification) onRegistered()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create your account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Use your email address to create an account and verify it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pw,
            onValueChange = { pw = it },
            label = { Text("Password (min 6 chars)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pw2,
            onValueChange = { pw2 = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (state is AuthUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                (state as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state is AuthUiState.Notice) {
            Spacer(Modifier.height(8.dp))
            Text(
                (state as AuthUiState.Notice).message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.register(name, email, pw, pw2) },
            enabled = state != AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state == AuthUiState.Loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text("Create account", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Sign in")
        }
    }
}
