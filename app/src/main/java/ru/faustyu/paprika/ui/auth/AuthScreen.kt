package ru.faustyu.paprika.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLoginSuccess: (String) -> Unit,
    onUrlChanged: (String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Settings Button for Custom Backend URL
        IconButton(
            onClick = { showUrlDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings, 
                contentDescription = "Server Settings"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLogin) "Paprika Login" else "Join Paprika",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.error != null) {
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }


            if (!isLogin) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (isLogin) "Username" else "Choose unique username") },
                supportingText = {
                    if (!isLogin) {
                        Text("This will be your unique handle (e.g. @paprika_fan)")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    viewModel.authenticate(isLogin, username, password, firstName, lastName, onSuccess = { token ->
                        onLoginSuccess(token)
                    }) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isLogin) "Sign In" else "Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isLogin = !isLogin }, enabled = !viewModel.isLoading) {
                Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Sign In")
            }
        }
        
        if (showUrlDialog) {
            var tempUrl by remember { mutableStateOf(ru.faustyu.paprika.data.network.NetworkModule.getCurrentUrl()) }
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text("Server URL") },
                text = {
                    Column {
                        Text("Enter backend address:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            placeholder = { Text("http://192.168.1.5:8080") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        ru.faustyu.paprika.data.network.NetworkModule.setCustomUrl(tempUrl)
                        onUrlChanged(tempUrl)
                        showUrlDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
