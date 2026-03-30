package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var notifyPrivate by remember { mutableStateOf(prefs.notifyPrivateChats) }
    var notifyGroups  by remember { mutableStateOf(prefs.notifyGroups) }
    var notifySound   by remember { mutableStateOf(prefs.notificationSound) }
    var notifyVibrate by remember { mutableStateOf(prefs.notifyVibrate) }
    var showBadge     by remember { mutableStateOf(prefs.showBadge) }
    var badgeMuted    by remember { mutableStateOf(prefs.badgeIncludeMuted) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications and Sounds") },
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Notifications for chats ──────────────────────────────────────
            item {
                Text(
                    "Notifications for chats",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Private Chats") },
                        supportingContent = { Text(if (notifyPrivate) "On" else "Off") },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(checked = notifyPrivate, onCheckedChange = {
                                notifyPrivate = it; prefs.notifyPrivateChats = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Groups") },
                        supportingContent = { Text(if (notifyGroups) "On" else "Off") },
                        leadingContent = {
                            Icon(Icons.Default.Group, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(checked = notifyGroups, onCheckedChange = {
                                notifyGroups = it; prefs.notifyGroups = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Sounds ───────────────────────────────────────────────────────
            item {
                Text(
                    "Sounds",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Message Sound") },
                        supportingContent = { Text("Play sound for new messages") },
                        leadingContent = {
                            Icon(Icons.Default.VolumeUp, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(checked = notifySound, onCheckedChange = {
                                notifySound = it; prefs.notificationSound = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Calls ────────────────────────────────────────────────────────
            item {
                Text(
                    "Calls",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Vibrate") },
                        supportingContent = { Text(if (notifyVibrate) "On" else "Off") },
                        leadingContent = {
                            Icon(Icons.Default.Vibration, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(checked = notifyVibrate, onCheckedChange = {
                                notifyVibrate = it; prefs.notifyVibrate = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Ringtone") },
                        supportingContent = { Text("Default") },
                        leadingContent = {
                            Icon(Icons.Default.MusicNote, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Badge Counter ────────────────────────────────────────────────
            item {
                Text(
                    "Badge Counter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Show Badge Icon") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(checked = showBadge, onCheckedChange = {
                                showBadge = it; prefs.showBadge = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Include Muted Chats") },
                        leadingContent = {
                            Icon(
                                Icons.Default.VolumeOff, contentDescription = null,
                                tint = if (showBadge) MaterialTheme.colorScheme.onSurfaceVariant
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = badgeMuted,
                                enabled = showBadge,
                                onCheckedChange = { badgeMuted = it; prefs.badgeIncludeMuted = it }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Text(
                    "Controls the number shown on the app icon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}
