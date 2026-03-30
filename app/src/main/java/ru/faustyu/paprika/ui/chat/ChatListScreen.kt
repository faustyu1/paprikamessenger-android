package ru.faustyu.paprika.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import ru.faustyu.paprika.data.network.Story
import ru.faustyu.paprika.ui.stories.StoriesViewModel

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
    storiesViewModel: StoriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onUrlChanged: (String) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        viewModel.loadChats()
        storiesViewModel.loadStories()
    }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadChats()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val chats = viewModel.chats
    val stories = storiesViewModel.stories
    val archivedChats = remember(chats) { chats.filter { it.is_archived } }
    val regularChats = remember(chats) {
        chats.filter { !it.is_archived }
            .sortedWith(compareByDescending<ChatDto> { it.is_pinned }.thenByDescending { it.last_message_at })
            .filter { it.last_message_at > 0 || it.id == 1L }
    }
    val hasStories = stories.isNotEmpty()
    val hasArchived = archivedChats.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let { snackbarHostState.showSnackbar(it) }
    }

    var debugTaps by remember { mutableIntStateOf(0) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf("") }
    var showDropdownFor by remember { mutableStateOf<Long?>(null) }
    var showArchivedExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Track number of hidden-above rows; once set, don't re-scroll
    var initialScrollApplied by remember { mutableStateOf(false) }
    LaunchedEffect(hasStories, hasArchived) {
        if (!initialScrollApplied) {
            val hiddenRows = (if (hasStories) 1 else 0) + (if (hasArchived) 1 else 0)
            if (hiddenRows > 0) {
                listState.scrollToItem(hiddenRows)
                initialScrollApplied = true
            }
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {

            // ── [hidden above] Stories row — only if stories exist ────────
            if (hasStories) {
                item(key = "stories_row") {
                    CompactStoriesRow(
                        stories = stories,
                        myAvatarUrl = viewModel.currentUser?.avatar?.takeIf { it.isNotBlank() }?.let { av ->
                            if (av.startsWith("http")) av else NetworkModule.baseUrl.removeSuffix("/") + av
                        },
                        myName = viewModel.currentUser?.let { u ->
                            u.first_name?.takeIf { it.isNotBlank() } ?: u.username
                        } ?: "My"
                    )
                    HorizontalDivider()
                }
            }

            // ── [hidden above] Archived chats row ─────────────────────────
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
                                        Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF00BCD4))),
                                        CircleShape
                                    )
                                    .padding(2.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .padding(2.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        trailingContent = {
                            if (totalUnread > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) { Text(totalUnread.toString()) }
                            }
                        },
                        modifier = Modifier.clickable { showArchivedExpanded = !showArchivedExpanded }
                    )
                    HorizontalDivider()

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
                                    showDropdown = showDropdownFor == chat.id,
                                    onDismissDropdown = { showDropdownFor = null },
                                    onClick = { onChatClick(if (chat.id == 1L) "paprika_system" else chat.id.toString()) },
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

            // ── Inline search bar ─────────────────────────────────────────
            item(key = "search_bar") {
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
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search Chats", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Regular chat list ─────────────────────────────────────────
            items(regularChats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    typingUser = viewModel.typingInChat[chat.id],
                    showDropdown = showDropdownFor == chat.id,
                    onDismissDropdown = { showDropdownFor = null },
                    onClick = { onChatClick(if (chat.id == 1L) "paprika_system" else chat.id.toString()) },
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

// ── Compact stories row ───────────────────────────────────────────────────────

private val storyRingBrush = Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF00BCD4)))
private val storyRingSeenColor = Color(0xFF555555)

@Composable
private fun CompactStoriesRow(
    stories: List<Story>,
    myAvatarUrl: String?,
    myName: String
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // My Story — always first
        item {
            CompactStoryCircle(
                imageUrl = myAvatarUrl,
                label = myName,
                showRing = false,
                showAddBadge = true,
                onClick = {}
            )
        }
        items(stories) { story ->
            val url = remember(story.media_url) {
                if (story.media_url.startsWith("http") || story.media_url.startsWith("content://"))
                    story.media_url
                else NetworkModule.baseUrl.removeSuffix("/") + story.media_url
            }
            CompactStoryCircle(
                imageUrl = url,
                label = "user_${story.user_id}",
                showRing = true,
                showAddBadge = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun CompactStoryCircle(
    imageUrl: String?,
    label: String,
    showRing: Boolean,
    showAddBadge: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp).clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.BottomEnd) {
            // Ring + avatar
            val avatarMod = if (showRing) {
                Modifier
                    .size(44.dp)
                    .background(storyRingBrush, CircleShape)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape)
            } else {
                Modifier
                    .size(44.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape)
            }

            Box(
                modifier = avatarMod.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        label.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add badge for My Story
            if (showAddBadge) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Chat row ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: ChatDto,
    typingUser: String?,
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
        if (chat.last_message_at <= 0) return@remember ""
        val instant = java.time.Instant.ofEpochSecond(chat.last_message_at)
        val zone = java.time.ZoneId.systemDefault()
        val nowDate = java.time.LocalDate.now(zone)
        val msgDate = instant.atZone(zone).toLocalDate()
        val daysDiff = msgDate.until(nowDate, java.time.temporal.ChronoUnit.DAYS)
        when {
            daysDiff == 0L -> java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(instant.atZone(zone))
            daysDiff == 1L -> "Yesterday"
            daysDiff < 7L -> java.time.format.DateTimeFormatter.ofPattern("EEE").format(instant.atZone(zone))
            else -> java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy").format(instant.atZone(zone))
        }
    }
    val isMuted = chat.mute_until > System.currentTimeMillis() / 1000

    Box {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (chat.is_pinned) {
                        Icon(Icons.Default.PushPin, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(chat.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            supportingContent = {
                if (typingUser != null) {
                    Text("$typingUser печатает...", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                } else {
                    val preview = chat.last_message_preview ?: ""
                    if (preview.startsWith("/media/") || (preview.startsWith("http") && preview.contains("/media/"))) {
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
                        modifier = Modifier.size(52.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chat.title.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(timeString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (chat.unread_count > 0) {
                        Badge(
                            containerColor = if (isMuted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (isMuted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        ) { Text(chat.unread_count.toString()) }
                    }
                }
            },
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        )

        DropdownMenu(expanded = showDropdown, onDismissRequest = onDismissDropdown) {
            DropdownMenuItem(
                text = { Text(if (chat.is_pinned) "Открепить" else "Закрепить") },
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
