package ru.faustyu.paprika.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.faustyu.paprika.BuildConfig
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
            error = null
            try {
                val resp = NetworkModule.api.getSessions()
                if (resp.isSuccessful) sessions = resp.body() ?: emptyList()
            } catch (e: Exception) {
                error = "Could not load sessions"
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
                error = "Could not terminate session"
            }
        }
    }

    fun terminateAllSessions() {
        viewModelScope.launch {
            try {
                sessions.filter { !it.is_current }.forEach {
                    NetworkModule.api.deleteSession(it.id.toString())
                }
                sessions = sessions.filter { it.is_current }
            } catch (e: Exception) {
                error = "Could not terminate sessions"
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
                title = { Text("Devices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentSession = viewModel.sessions.firstOrNull { it.is_current }
        val otherSessions = viewModel.sessions.filter { !it.is_current }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Link Desktop Device card ─────────────────────────────────────
            item {
                SettingsSectionCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Link Desktop or Web by scanning a QR code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { /* Coming soon */ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Link Desktop Device")
                        }
                    }
                }
            }

            // ── This device ──────────────────────────────────────────────────
            item {
                Text(
                    "This device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Paprika ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                currentSession?.ip?.let { "online" } ?: "online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    SettingsDivider()

                    // Terminate all other sessions
                    TextButton(
                        onClick = { viewModel.terminateAllSessions() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Terminate All Other Sessions")
                    }
                }
                Text(
                    "Logs out all devices except for this one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            // ── Active sessions ───────────────────────────────────────────────
            if (otherSessions.isNotEmpty()) {
                item {
                    Text(
                        "Active sessions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        otherSessions.forEachIndexed { index, session ->
                            if (index > 0) SettingsDivider()
                            val lastActive = remember(session.last_active) {
                                val instant = java.time.Instant.ofEpochSecond(session.last_active)
                                val formatter = java.time.format.DateTimeFormatter
                                    .ofPattern("d MMM, HH:mm", java.util.Locale.getDefault())
                                    .withZone(java.time.ZoneId.systemDefault())
                                formatter.format(instant)
                            }
                            // Guess device type from name
                            val isDesktop = session.device_name.contains("Mac", ignoreCase = true) ||
                                session.device_name.contains("Windows", ignoreCase = true) ||
                                session.device_name.contains("Desktop", ignoreCase = true) ||
                                session.device_name.contains("Web", ignoreCase = true)
                            val avatarColor = if (isDesktop) Color(0xFF2196F3) else Color(0xFF4CAF50)

                            ListItem(
                                headlineContent = {
                                    Text(session.device_name.ifBlank { "Unknown Device" },
                                        fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    Text("${session.ip} · $lastActive",
                                        style = MaterialTheme.typography.bodySmall)
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isDesktop) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.terminateSession(session.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Terminate",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
