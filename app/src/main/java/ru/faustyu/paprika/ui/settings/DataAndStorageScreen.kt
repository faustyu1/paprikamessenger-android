package ru.faustyu.paprika.ui.settings

import android.net.TrafficStats
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0               -> "0 KB"
    bytes < 1024L * 1024     -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
    else                     -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAndStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    // Real cache size (calculated once on composition)
    val cacheBytes = remember {
        context.cacheDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
    }
    // Approximate total data usage since last reboot
    val dataBytes = remember {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) 0L
        else rx + tx
    }

    var autoMobile  by remember { mutableStateOf(prefs.autoDownloadMobile) }
    var autoWifi    by remember { mutableStateOf(prefs.autoDownloadWifi) }
    var autoRoaming by remember { mutableStateOf(prefs.autoDownloadRoaming) }
    var galPrivate  by remember { mutableStateOf(prefs.saveGalleryPrivate) }
    var galGroups   by remember { mutableStateOf(prefs.saveGalleryGroups) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data and Storage") },
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
            // ── Disk & network usage ─────────────────────────────────────────
            item {
                Text(
                    "Disk and network usage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Storage Usage") },
                        leadingContent = {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storage, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        trailingContent = {
                            Text(
                                formatBytes(cacheBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Data Usage") },
                        leadingContent = {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.BarChart, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        trailingContent = {
                            Text(
                                formatBytes(dataBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Auto download ─────────────────────────────────────────────────
            item {
                Text(
                    "Automatic media download",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("When using mobile data") },
                        supportingContent = { Text("Photos, Videos (10 MB), Files (1 MB)") },
                        trailingContent = {
                            Switch(checked = autoMobile, onCheckedChange = {
                                autoMobile = it; prefs.autoDownloadMobile = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("When connected to Wi-Fi") },
                        supportingContent = { Text("Photos, Videos (15 MB), Files (3 MB)") },
                        trailingContent = {
                            Switch(checked = autoWifi, onCheckedChange = {
                                autoWifi = it; prefs.autoDownloadWifi = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("When roaming") },
                        supportingContent = { Text("Photos") },
                        trailingContent = {
                            Switch(checked = autoRoaming, onCheckedChange = {
                                autoRoaming = it; prefs.autoDownloadRoaming = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    TextButton(
                        onClick = {
                            autoMobile = true; prefs.autoDownloadMobile = true
                            autoWifi = true;   prefs.autoDownloadWifi = true
                            autoRoaming = false; prefs.autoDownloadRoaming = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset Auto-Download Settings")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Save to Gallery ───────────────────────────────────────────────
            item {
                Text(
                    "Save to Gallery",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Private Chats") },
                        supportingContent = { Text(if (galPrivate) "On" else "Off") },
                        trailingContent = {
                            Switch(checked = galPrivate, onCheckedChange = {
                                galPrivate = it; prefs.saveGalleryPrivate = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Groups") },
                        supportingContent = { Text(if (galGroups) "On" else "Off") },
                        trailingContent = {
                            Switch(checked = galGroups, onCheckedChange = {
                                galGroups = it; prefs.saveGalleryGroups = it
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Text(
                    "Received photos and videos will be automatically saved to your device gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}
