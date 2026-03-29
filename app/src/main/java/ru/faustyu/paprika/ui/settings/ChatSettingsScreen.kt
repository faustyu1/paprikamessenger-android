package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var fontSize by remember { mutableStateOf(prefs.fontSize) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings") },
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
                    "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("System Default" to 0, "Light" to 1, "Dark" to 2).forEachIndexed { index, (label, mode) ->
                        if (index > 0) SettingsDivider()
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = { themeMode = mode; prefs.themeMode = mode }
                                )
                            },
                            modifier = Modifier.clickable { themeMode = mode; prefs.themeMode = mode },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Font Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("Small" to 0.85f, "Normal" to 1.0f, "Large" to 1.15f, "Very Large" to 1.3f).forEachIndexed { index, (label, scale) ->
                        if (index > 0) SettingsDivider()
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                RadioButton(
                                    selected = fontSize == scale,
                                    onClick = { fontSize = scale; prefs.fontSize = scale }
                                )
                            },
                            modifier = Modifier.clickable { fontSize = scale; prefs.fontSize = scale },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
