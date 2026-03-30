package ru.faustyu.paprika.ui.settings

import android.net.TrafficStats
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.faustyu.paprika.data.PrefsManager

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0                  -> "0 KB"
    bytes < 1024L * 1024        -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
    else                        -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

data class DonutSegment(val color: Color, val fraction: Float, val label: String, val bytes: Long)

@Composable
private fun DonutChart(
    segments: List<DonutSegment>,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = segments.map { it.color }
    val fractions = segments.map { it.fraction }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.18f
            val radius = (size.minDimension - stroke) / 2f
            val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
            val arcSize = Size(radius * 2, radius * 2)
            var startAngle = -90f
            fractions.forEachIndexed { i, fraction ->
                val sweep = fraction * 360f
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweep - 1f, // 1° gap between segments
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Text(
            centerLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Storage Usage Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageUsageScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Calculate actual cache size once
    val totalCacheBytes = remember {
        context.cacheDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
    }
    var cacheBytes by remember { mutableLongStateOf(totalCacheBytes) }

    // Fake per-category breakdown (proportional to typical usage)
    val segments = remember(cacheBytes) {
        if (cacheBytes == 0L) {
            listOf(DonutSegment(Color(0xFF9E9E9E), 1f, "Empty", 0L))
        } else {
            val fractions = listOf(0.30f, 0.22f, 0.19f, 0.13f, 0.16f)
            val colors = listOf(
                Color(0xFF2196F3), Color(0xFF4CAF50),
                Color(0xFFFF9800), Color(0xFFF44336), Color(0xFF9C27B0)
            )
            val labels = listOf("Videos", "Photos", "Documents", "Stickers & Emoji", "Other")
            fractions.mapIndexed { i, f ->
                DonutSegment(colors[i], f, labels[i], (cacheBytes * f).toLong())
            }
        }
    }

    val totalLabel = formatBytes(cacheBytes)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Usage") },
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
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Donut
            item {
                DonutChart(
                    segments = segments,
                    centerLabel = totalLabel,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(200.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Storage Usage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Paprika cache on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }

            // Breakdown
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    segments.forEachIndexed { index, seg ->
                        if (index > 0) SettingsDivider()
                        ListItem(
                            headlineContent = { Text(seg.label) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(seg.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                Text(
                                    formatBytes(seg.bytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Clear Cache button
            item {
                Button(
                    onClick = {
                        context.cacheDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
                        cacheBytes = 0L
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Clear Cache  ${formatBytes(cacheBytes)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    "All media will stay on the server and can be re-downloaded if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Auto-remove section
            item {
                Text(
                    "Auto-remove cached media",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, bottom = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AutoRemoveRow(
                        icon = Icons.Default.Person,
                        iconColor = Color(0xFF2196F3),
                        label = "Private Chats",
                        value = "1 week"
                    )
                    SettingsDivider()
                    AutoRemoveRow(
                        icon = Icons.Default.Group,
                        iconColor = Color(0xFF4CAF50),
                        label = "Group Chats",
                        value = "1 month"
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoRemoveRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Data Usage Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(onBack: () -> Unit) {
    val rxBytes = remember { TrafficStats.getTotalRxBytes().coerceAtLeast(0L) }
    val txBytes = remember { TrafficStats.getTotalTxBytes().coerceAtLeast(0L) }
    val total = rxBytes + txBytes

    // Per-category breakdown (approximated fractions)
    val segments = remember(total) {
        if (total == 0L) {
            listOf(DonutSegment(Color(0xFF9E9E9E), 1f, "No data", 0L))
        } else {
            val f = listOf(0.58f, 0.16f, 0.14f, 0.10f, 0.02f)
            val colors = listOf(
                Color(0xFF2196F3), Color(0xFF4CAF50),
                Color(0xFFFF9800), Color(0xFF8BC34A), Color(0xFFF44336)
            )
            val labels = listOf("Videos", "Photos", "Messages", "Documents", "Music")
            f.mapIndexed { i, fr -> DonutSegment(colors[i], fr, labels[i], (total * fr).toLong()) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Usage") },
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
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                DonutChart(
                    segments = segments,
                    centerLabel = formatBytes(total),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(200.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Network usage since last reboot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(24.dp))
            }

            // Breakdown
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    segments.forEachIndexed { index, seg ->
                        if (index > 0) SettingsDivider()
                        ListItem(
                            headlineContent = { Text(seg.label) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(seg.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                Text(
                                    formatBytes(seg.bytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Total network usage
            item {
                Text(
                    "Total network usage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, bottom = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Data sent") },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2196F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = {
                            Text(formatBytes(txBytes), color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Data received") },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = {
                            Text(formatBytes(rxBytes), color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Data and Storage Hub screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAndStorageScreen(
    onBack: () -> Unit,
    onStorageClick: () -> Unit = {},
    onDataUsageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    val cacheBytes = remember {
        context.cacheDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
    }
    val dataBytes = remember {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx < 0 || tx < 0) 0L else rx + tx
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
                    // Storage Usage → navigates to StorageUsageScreen
                    ListItem(
                        headlineContent = { Text("Storage Usage") },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2196F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = {
                            Text(formatBytes(cacheBytes), color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        },
                        modifier = Modifier.clickable(onClick = onStorageClick),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    SettingsDivider()
                    // Data Usage → navigates to DataUsageScreen
                    ListItem(
                        headlineContent = { Text("Data Usage") },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = {
                            Text(formatBytes(dataBytes), color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        },
                        modifier = Modifier.clickable(onClick = onDataUsageClick),
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
                            autoWifi = true;   prefs.autoDownloadWifi   = true
                            autoRoaming = false; prefs.autoDownloadRoaming = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Reset Auto-Download Settings") }
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
            }
        }
    }
}
