package ru.faustyu.paprika.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.R
import ru.faustyu.paprika.util.CryptoManager

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLoginSuccess: (token: String, username: String) -> Unit,
    onUrlChanged: (String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }

    // Generate auth key once on first launch (no-op if already exists)
    LaunchedEffect(Unit) {
        CryptoManager.generateAuthKeyIfNeeded()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { showUrlDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.auth_server_settings)
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
                text = if (isLogin) stringResource(R.string.auth_title_login)
                       else stringResource(R.string.auth_title_register),
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

            // ── Register: имя + логин ──────────────────────────────────
            if (!isLogin) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(R.string.auth_first_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(R.string.auth_last_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text(
                        if (isLogin) stringResource(R.string.auth_username_login)
                        else stringResource(R.string.auth_username_register)
                    )
                },
                supportingText = {
                    if (!isLogin) Text(stringResource(R.string.auth_username_hint))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmed = username.trim()
                    if (isLogin) {
                        viewModel.login(trimmed, onSuccess = { token -> onLoginSuccess(token, trimmed) })
                    } else {
                        viewModel.register(
                            username = trimmed,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            onSuccess = { token -> onLoginSuccess(token, trimmed) }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (isLogin) stringResource(R.string.auth_sign_in)
                        else stringResource(R.string.auth_sign_up)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    isLogin = !isLogin
                    viewModel.clearError()
                },
                enabled = !viewModel.isLoading
            ) {
                Text(
                    if (isLogin) stringResource(R.string.auth_no_account)
                    else stringResource(R.string.auth_have_account)
                )
            }
        }

        if (showUrlDialog) {
            var tempUrl by remember {
                mutableStateOf(ru.faustyu.paprika.data.network.NetworkModule.getCurrentUrl())
            }
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text(stringResource(R.string.auth_server_url_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.auth_server_url_text))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            placeholder = { Text(stringResource(R.string.auth_server_url_placeholder)) },
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
                        Text(stringResource(R.string.common_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}
