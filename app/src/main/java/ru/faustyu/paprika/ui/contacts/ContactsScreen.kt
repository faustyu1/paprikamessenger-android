package ru.faustyu.paprika.ui.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ru.faustyu.paprika.data.network.ContactEntry
import ru.faustyu.paprika.data.network.NetworkModule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    onContactClick: (Long) -> Unit = {},
    viewModel: ContactsViewModel = viewModel()
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ContactEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.error) {
        viewModel.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Search bar ────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Search Contacts") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            // ── Quick actions ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Invite Friends") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        ListItem(
                            headlineContent = { Text("Recent Calls") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {}
                        )
                    }
                }
            }

            // ── Section header ────────────────────────────────────────────
            if (viewModel.filteredContacts.isNotEmpty()) {
                item {
                    Text(
                        "Sorted by last seen time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
            }

            // ── Contact list ──────────────────────────────────────────────
            items(viewModel.filteredContacts) { contact ->
                ContactListItem(
                    contact = contact,
                    onClick = { onContactClick(contact.user_id) },
                    onLongClick = { showDeleteDialog = contact }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            }

            // Empty state
            if (!viewModel.isLoading && viewModel.contacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No contacts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Tap + to add a contact",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (viewModel.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Add contact bottom sheet
    if (showAddSheet) {
        AddContactSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { username ->
                viewModel.addContact(
                    username = username,
                    onSuccess = { showAddSheet = false },
                    onError = { msg ->
                        viewModel.showError(msg)
                    }
                )
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { contact ->
        val name = buildContactName(contact)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Remove Contact") },
            text = { Text("Remove $name from your contacts?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contact.user_id)
                    showDeleteDialog = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactListItem(
    contact: ContactEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val avatarUrl = contact.avatar?.takeIf { it.isNotBlank() }?.let { av ->
        if (av.startsWith("http")) av
        else NetworkModule.baseUrl.removeSuffix("/") + av
    }
    val displayName = buildContactName(contact)
    val lastSeenText = formatLastSeen(contact.is_online, contact.last_seen)

    ListItem(
        headlineContent = {
            Text(displayName, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                lastSeenText,
                color = if (contact.is_online) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "New Contact",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (required)") },
                placeholder = { Text("@username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) }
            )

            // QR code option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {}
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add via QR Code",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        isAdding = true
                        onAdd(username.trimStart('@'))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = username.isNotBlank() && !isAdding
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Contact", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun buildContactName(contact: ContactEntry): String {
    val first = contact.first_name?.takeIf { it.isNotBlank() }
    val last = contact.last_name?.takeIf { it.isNotBlank() }
    return when {
        first != null && last != null -> "$first $last"
        first != null -> first
        else -> contact.username
    }
}

private fun formatLastSeen(isOnline: Boolean, lastSeen: Long): String {
    if (isOnline) return "online"
    if (lastSeen <= 0) return "last seen recently"
    val diff = System.currentTimeMillis() / 1000 - lastSeen
    return when {
        diff < 60 -> "last seen just now"
        diff < 3600 -> "last seen at ${formatTime(lastSeen)}"
        diff < 86400 -> "last seen at ${formatTime(lastSeen)}"
        else -> "last seen recently"
    }
}

private fun formatTime(unixSec: Long): String {
    val instant = java.time.Instant.ofEpochSecond(unixSec)
    val zoneId = java.time.ZoneId.systemDefault()
    return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .format(instant.atZone(zoneId))
}
