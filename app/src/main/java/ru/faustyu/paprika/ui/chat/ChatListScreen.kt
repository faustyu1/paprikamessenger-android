package ru.faustyu.paprika.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ru.faustyu.paprika.R
import ru.faustyu.paprika.data.network.ChatDto
import ru.faustyu.paprika.data.network.NetworkModule

data class ChatItem(val id: String, val name: String, val lastMessage: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: ChatListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onUrlChanged: (String) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) { viewModel.loadChats() }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadChats()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val chats = viewModel.chats
    val archivedChats = remember(chats) { chats.filter { it.is_archived } }
    val regularChats = remember(chats) {
        chats.filter { !it.is_archived }
            .sortedWith(compareByDescending<ChatDto> { it.is_pinned }.thenByDescending { it.last_message_at })
            .filter { it.last_message_at > 0 || it.id == 1L }
    }
    val hasArchived = archivedChats.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg = viewModel.error
    LaunchedEffect(errorMsg) {
        if (errorMsg != null) snackbarHostState.showSnackbar(errorMsg)
    }

    var debugTaps by remember { mutableIntStateOf(0) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf("") }
    var showDropdownFor by remember { mutableStateOf<Long?>(null) }
    var showArchivedExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // After chats first load, if there are archived chats, hide the archived row by starting below it
    LaunchedEffect(hasArchived) {
        if (hasArchived && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            listState.scrollToItem(1)
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false; debugTaps = 0 },
            title = { Text(stringResource(R.string.chatlist_update_url)) },
            text = {
                Column {
                    Text(stringResource(R.string.chatlist_current_url) + NetworkModule.baseUrl)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text(stringResource(R.string.chatlist_new_url)) },
                        placeholder = { Text(stringResource(R.string.chatlist_url_placeholder)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempUrl.isNotEmpty()) {
                        onUrlChanged(tempUrl)
                        showUrlDialog = false
                        debugTaps = 0
                    }
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false; debugTaps = 0 }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.chatlist_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            debugTaps++
                            if (debugTaps >= 3) {
                                tempUrl = NetworkModule.baseUrl
                                showUrlDialog = true
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.chatlist_new_chat))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ── Stories bar ───────────────────────────────────────────────
            ru.faustyu.paprika.ui.stories.StoriesBar(
                myAvatarUrl = viewModel.currentUser?.avatar?.takeIf { it.isNotBlank() }?.let { av ->
                    if (av.startsWith("http")) av else NetworkModule.baseUrl.removeSuffix("/") + av
                },
                myName = viewModel.currentUser?.let { u ->
                    u.first_name?.takeIf { it.isNotBlank() } ?: u.username
                } ?: "My Story",
                onStoryClick = {}
            )

            // ── Inline search bar ─────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Search Chats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Chat list ─────────────────────────────────────────────────
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {

                // ── Archived chats row (hidden above by default) ──────────
                if (hasArchived) {
                    item(key = "archived_row") {
                        val totalUnread = archivedChats.sumOf { it.unread_count }
                        val subtitle = archivedChats.take(4).joinToString(", ") { it.title }

                        ListItem(
                            headlineContent = {
                                Text("Archived Chats", fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Text(
                                    subtitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF4CAF50), Color(0xFF00BCD4))
                                            ),
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                if (totalUnread > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(totalUnread.toString())
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                showArchivedExpanded = !showArchivedExpanded
                            }
                        )
                        HorizontalDivider()

                        // Expandable archived chats list
                        AnimatedVisibility(
                            visible = showArchivedExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                archivedChats.sortedByDescending { it.last_message_at }.forEach { chat ->
                                    ChatRow(
                                        chat = chat,
                                        typingUser = viewModel.typingInChat[chat.id],
                                        isPinned = false,
                                        showDropdown = showDropdownFor == chat.id,
                                        onDismissDropdown = { showDropdownFor = null },
                                        onClick = {
                                            val idStr = if (chat.id == 1L) "paprika_system" else chat.id.toString()
                                            onChatClick(idStr)
                                        },
                                        onLongClick = { showDropdownFor = chat.id },
                                        onPin = { viewModel.pinChat(chat.id.toString()); showDropdownFor = null },
                                        onArchive = { viewModel.archiveChat(chat.id.toString()); showDropdownFor = null },
                                        onMute = { viewModel.muteChat(chat.id.toString(), System.currentTimeMillis() / 1000 + 28800); showDropdownFor = null }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                // ── Regular chats ─────────────────────────────────────────
                items(regularChats, key = { it.id }) { chat ->
                    ChatRow(
                        chat = chat,
                        typingUser = viewModel.typingInChat[chat.id],
                        isPinned = chat.is_pinned,
                        showDropdown = showDropdownFor == chat.id,
                        onDismissDropdown = { showDropdownFor = null },
                        onClick = {
                            val idStr = if (chat.id == 1L) "paprika_system" else chat.id.toString()
                            onChatClick(idStr)
                        },
                        onLongClick = { showDropdownFor = chat.id },
                        onPin = { viewModel.pinChat(chat.id.toString()); showDropdownFor = null },
                        onArchive = { viewModel.archiveChat(chat.id.toString()); showDropdownFor = null },
                        onMute = { viewModel.muteChat(chat.id.toString(), System.currentTimeMillis() / 1000 + 28800); showDropdownFor = null }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: ChatDto,
    typingUser: String?,
    isPinned: Boolean,
    showDropdown: Boolean,
    onDismissDropdown: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onMute: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl = remember(chat.avatar) {
        chat.avatar.takeIf { it.isNotBlank() }?.let { av ->
            if (av.startsWith("http")) av else NetworkModule.baseUrl.removeSuffix("/") + av
        }
    }
    val timeString = remember(chat.last_message_at) {
        if (chat.last_message_at <= 0) ""
        else {
            val instant = java.time.Instant.ofEpochSecond(chat.last_message_at)
            val now = java.time.Instant.now()
            val zoneId = java.time.ZoneId.systemDefault()
            val daysDiff = java.time.Duration.between(
                instant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant(),
                now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
            ).toDays()
            when {
                daysDiff == 0L -> java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(instant.atZone(zoneId))
                daysDiff == 1L -> "Yesterday"
                daysDiff < 7L -> java.time.format.DateTimeFormatter.ofPattern("EEE").format(instant.atZone(zoneId))
                else -> java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy").format(instant.atZone(zoneId))
            }
        }
    }

    Box {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(end = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(chat.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            supportingContent = {
                if (typingUser != null) {
                    Text(
                        "$typingUser печатает...",
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val preview = chat.last_message_preview ?: ""
                    if (preview.startsWith("/media/") || (preview.startsWith("http") && !preview.startsWith("http://localhost"))) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(3.dp))
                            Text(stringResource(R.string.chatlist_photo), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            leadingContent = {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            chat.title.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(timeString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (chat.unread_count > 0) {
                        val isMuted = chat.mute_until > System.currentTimeMillis() / 1000
                        Badge(
                            containerColor = if (isMuted) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.primary,
                            contentColor = if (isMuted) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onPrimary
                        ) { Text(chat.unread_count.toString()) }
                    }
                }
            },
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        )

        DropdownMenu(expanded = showDropdown, onDismissRequest = onDismissDropdown) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Открепить" else "Закрепить") },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = onPin
            )
            DropdownMenuItem(
                text = { Text(if (chat.is_archived) "Разархивировать" else "Архивировать") },
                leadingIcon = { Icon(Icons.Default.Archive, null) },
                onClick = onArchive
            )
            DropdownMenuItem(
                text = { Text("Отключить уведомления") },
                leadingIcon = { Icon(Icons.Default.NotificationsOff, null) },
                onClick = onMute
            )
        }
    }
}
