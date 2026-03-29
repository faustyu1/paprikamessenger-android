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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSessionsClick: () -> Unit,
    onChatSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // Section 1: Main settings
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsNavItem(
                        icon = Icons.Default.Person,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Account",
                        subtitle = "Username, Bio",
                        onClick = onBack // Back to ProfileScreen which IS the Account screen
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        iconTint = androidx.compose.ui.graphics.Color(0xFF2196F3),
                        title = "Chat Settings",
                        subtitle = "Theme, Font Size",
                        onClick = onChatSettingsClick
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Default.Lock,
                        iconTint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        title = "Privacy & Security",
                        subtitle = "App Lock, Sessions",
                        onClick = onPrivacyClick
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Default.Notifications,
                        iconTint = androidx.compose.ui.graphics.Color(0xFFFF5722),
                        title = "Notifications",
                        subtitle = "Sounds, Badges",
                        onClick = onNotificationsClick
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Default.Devices,
                        iconTint = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                        title = "Devices",
                        subtitle = "Manage connected devices",
                        onClick = onSessionsClick
                    )
                }
            }

            // Section 2: App info
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsNavItem(
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "About Paprika",
                        subtitle = "Version, Licenses",
                        showArrow = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    showArrow: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    )
}
