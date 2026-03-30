package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.ui.components.PinPad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onSessionsClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var appLockEnabled by remember { mutableStateOf(prefs.appLockEnabled) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    if (showPinDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            showPinDialog = false
            pinInput = ""
            appLockEnabled = !appLockEnabled
        }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    PinPad(
                        currentPin = pinInput,
                        onPinChange = { it ->
                            pinInput = it
                            if (it.length == 4) {
                                val md = java.security.MessageDigest.getInstance("SHA-256")
                                prefs.pinHash = md.digest(it.toByteArray()).joinToString("") { "%02x".format(it) }
                                prefs.appLockEnabled = appLockEnabled
                                showPinDialog = false
                                pinInput = ""
                            }
                        },
                        showBiometric = false,
                        title = "Set PIN",
                        isError = false
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "Security",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("App Lock") },
                        supportingContent = { Text(if (appLockEnabled) "PIN enabled" else "Disabled") },
                        leadingContent = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        },
                        trailingContent = {
                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = {
                                    appLockEnabled = it
                                    if (it) {
                                        showPinDialog = true
                                    } else {
                                        prefs.appLockEnabled = false
                                        prefs.pinHash = null
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Active Sessions") },
                        supportingContent = { Text("Manage connected devices") },
                        leadingContent = {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF9C27B0))
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.clickable { onSessionsClick() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
                Text(
                    "When App Lock is enabled, you'll need a PIN or biometrics to open Paprika.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}
