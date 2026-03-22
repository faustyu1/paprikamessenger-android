package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.NetworkModule

data class SessionDto(
    val id: Long,
    val device_name: String,
    val ip: String,
    val last_active: Long,
    val created_at: Long,
    val is_current: Boolean = false
)

class SessionsViewModel : ViewModel() {
    var sessions by mutableStateOf<List<SessionDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { loadSessions() }

    fun loadSessions() {
        viewModelScope.launch {
            isLoading = true
            try {
                val resp = NetworkModule.api.getSessions()
                if (resp.isSuccessful) sessions = resp.body() ?: emptyList()
            } catch (e: Exception) {
                error = "Ошибка загрузки сессий"
            } finally {
                isLoading = false
            }
        }
    }

    fun terminateSession(id: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.deleteSession(id.toString())
                sessions = sessions.filter { it.id != id }
            } catch (e: Exception) {
                error = "Не удалось завершить сессию"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onBack: () -> Unit,
    viewModel: SessionsViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Активные сессии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(viewModel.sessions) { session ->
                    val lastActive = remember(session.last_active) {
                        val instant = java.time.Instant.ofEpochSecond(session.last_active)
                        val formatter = java.time.format.DateTimeFormatter
                            .ofPattern("d MMM, HH:mm", java.util.Locale("ru"))
                            .withZone(java.time.ZoneId.systemDefault())
                        formatter.format(instant)
                    }
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(session.device_name.ifBlank { "Неизвестное устройство" })
                                if (session.is_current) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("текущая", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        },
                        supportingContent = { Text("${session.ip} · $lastActive") },
                        leadingContent = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                        trailingContent = {
                            if (!session.is_current) {
                                IconButton(onClick = { viewModel.terminateSession(session.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Завершить",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
