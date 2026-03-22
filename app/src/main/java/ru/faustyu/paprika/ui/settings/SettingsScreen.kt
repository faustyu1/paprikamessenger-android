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

    var darkTheme by remember { mutableStateOf(prefs.darkTheme) }
    var fontSize by remember { mutableStateOf(prefs.fontSize) }
    var appLockEnabled by remember { mutableStateOf(prefs.appLockEnabled) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var notificationSound by remember { mutableStateOf(prefs.notificationSound) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinInput = "" },
            title = { Text(if (appLockEnabled) "Установить PIN" else "Отключить PIN") },
            text = {
                Column {
                    Text("Введите 4-значный PIN:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput.length == 4) {
                        prefs.pinHash = pinInput.hashCode().toString()
                        prefs.appLockEnabled = appLockEnabled
                        showPinDialog = false
                        pinInput = ""
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinInput = ""; appLockEnabled = !appLockEnabled }) {
                    Text("Отмена")
                }
            }
        )
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
                ListItem(
                    headlineContent = { Text("Тёмная тема") },
                    supportingContent = { Text(if (darkTheme) "Включена" else "Выключена") },
                    leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = {
                                darkTheme = it
                                prefs.darkTheme = it
                            }
                        )
                    }
                )
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
