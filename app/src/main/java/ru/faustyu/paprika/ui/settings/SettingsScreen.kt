package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSessionsClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var fontSize by remember { mutableStateOf(prefs.fontSize) }
    var appLockEnabled by remember { mutableStateOf(prefs.appLockEnabled) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var notificationSound by remember { mutableStateOf(prefs.notificationSound) }

    if (showPinDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPinDialog = false; pinInput = ""; appLockEnabled = !appLockEnabled }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    ru.faustyu.paprika.ui.components.PinPad(
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
                        title = "Установить PIN",
                        isError = false
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text("Тема оформления", style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary)
                val themes = listOf("Как в системе" to 0, "Светлая" to 1, "Тёмная" to 2)
                themes.forEach { (label, mode) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { themeMode = mode; prefs.themeMode = mode }
                            )
                        },
                        modifier = Modifier.clickable { themeMode = mode; prefs.themeMode = mode }
                    )
                }
                HorizontalDivider()
            }
            item {
                Text("Размер шрифта", style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary)
                val sizes = listOf("Маленький" to 0.85f, "Обычный" to 1.0f, "Большой" to 1.15f, "Очень большой" to 1.3f)
                sizes.forEach { (label, scale) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            RadioButton(
                                selected = fontSize == scale,
                                onClick = { fontSize = scale; prefs.fontSize = scale }
                            )
                        },
                        modifier = Modifier.clickable { fontSize = scale; prefs.fontSize = scale }
                    )
                }
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Звук уведомлений") },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = notificationSound,
                            onCheckedChange = {
                                notificationSound = it
                                prefs.notificationSound = it
                            }
                        )
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Блокировка приложения") },
                    supportingContent = { Text(if (appLockEnabled) "PIN включён" else "Выключена") },
                    leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
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
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Активные сессии") },
                    supportingContent = { Text("Управление устройствами") },
                    leadingContent = { Icon(Icons.Filled.Devices, contentDescription = null) },
                    modifier = Modifier.clickable { onSessionsClick() }
                )
                HorizontalDivider()
            }
        }
    }
}
