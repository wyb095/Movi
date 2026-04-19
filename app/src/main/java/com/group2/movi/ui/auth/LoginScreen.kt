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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginNeedsVerify: () -> Unit,
    onGoToRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            AuthUiState.Success -> onLoginSuccess()
            AuthUiState.NeedsVerification -> onLoginNeedsVerify()
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Sign in to continue with Movi",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

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
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(
                        if (showPw) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
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

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.sendPasswordReset(email) }, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.login(email, password) },
            enabled = state != AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state == AuthUiState.Loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text("Sign in", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onGoToRegister) {
            Text("New to Movi? Create an account")
        }
    }
}
