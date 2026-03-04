package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onProfileClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(chatId) {
        viewModel.connectToChat(chatId)
    }

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMessageId by remember { mutableStateOf<Long?>(null) }
    var showMessageActions by remember { mutableStateOf(false) }
    
    val title = viewModel.chatTitle.value
    val subtitle = viewModel.chatSubtitle.value
    val otherId = viewModel.otherUserId.value
    val isGroup = viewModel.isGroup.value

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Member") },
            text = {
                Column {
                    var query by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { 
                            query = it
                            viewModel.searchUsers(it)
                        },
                        label = { Text("Search User") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(viewModel.searchResults) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addMember(user.id)
                                        showAddMemberDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val userAvatar = user.avatar?.let { av ->
                                     if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                                }
                                if (userAvatar != null) {
                                    AsyncImage(
                                        model = userAvatar,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                     Box(modifier = Modifier.size(32.dp).background(Color.Gray, CircleShape))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(user.username)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendImage(chatId, uri, context)
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (otherId != null) {
                                    onProfileClick(otherId.toString())
                                }
                            }
                    ) {
                        val avatarUrl = viewModel.chatAvatar.value?.takeIf { it.isNotBlank() }?.let { av ->
                            if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                        }

                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = title, 
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isGroup) {
                        IconButton(onClick = { /* Call */ }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call")
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                             Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Add Member") },
                                    onClick = { 
                                        showMenu = false 
                                        showAddMemberDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Clear History") },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            ) 
        },
        bottomBar = {
                // Reply Preview
                viewModel.replyingToMessage.value?.let { replyMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                             modifier = Modifier
                                 .width(3.dp)
                                 .height(32.dp)
                                 .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (replyMsg.isMe) "Reply to yourself" else "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyMsg.content,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.replyingToMessage.value = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = { 
                    pickMedia.launch(
                        androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                     Icon(Icons.Default.Add, contentDescription = "Attach")
                }
                
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") }
                )
                
                if (inputText.isEmpty()) {
                    IconButton(onClick = {
                        // TODO: Implement voice recording
                        // For demonstration, we'll just send a mock voice message
                        // viewModel.sendVoiceMessage(chatId, mockFile)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Message")
                    }
                } else {
                    IconButton(onClick = {
                        viewModel.sendMessage(chatId, inputText)
                        inputText = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = true // Chat style
        ) {
            items(viewModel.messages, key = { it.id ?: 0L }) { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                selectedMessageId = message.id
                                showMessageActions = true
                            }
                        ),
                    contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    val timeString = remember(message.timestamp) {
                        try {
                            val instant = java.time.Instant.ofEpochSecond(message.timestamp)
                            val zoneId = java.time.ZoneId.systemDefault()
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                            instant.atZone(zoneId).format(formatter)
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    ImprovedMessageBubble(
                        message = MessageBubbleData(
                            id = message.id ?: 0L,
                            content = message.content,
                            time = timeString,
                            isOwnMessage = message.isMe,
                            isRead = message.status == "read",
                            isEdited = false,
                            type = message.type,
                            isPlaying = viewModel.currentlyPlayingUrl.value == (if (message.content.startsWith("http")) message.content else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + message.content),
                            replyToContent = if (message.replyToId != null) {
                                viewModel.messages.find { it.id == message.replyToId }?.content ?: "Message deleted"
                            } else null
                        ),
                        onLongClick = {
                            selectedMessageId = message.id
                            showMessageActions = true
                        },
                        onPlayClick = {
                            if (message.type == "voice") {
                                viewModel.playVoice(message.content)
                            }
                        }
                    )
                }
            }
        }

        if (showMessageActions && selectedMessageId != null) {
            val selectedMsg = viewModel.messages.find { it.id == selectedMessageId }
            if (selectedMsg != null) {
                MessageActionsBottomSheet(
                    messageId = selectedMessageId!!,
                    isOwnMessage = selectedMsg.isMe,
                    onDismiss = { showMessageActions = false },
                    onReply = {
                        viewModel.replyingToMessage.value = selectedMsg
                        showMessageActions = false
                    },
                    onEdit = {
                        // TODO
                        showMessageActions = false
                    },
                    onDelete = {
                        viewModel.deleteMessage(chatId, selectedMessageId!!)
                        showMessageActions = false
                    },
                    onForward = {
                        // TODO
                        showMessageActions = false
                    },
                    onSelect = {
                        viewModel.toggleSelection(selectedMessageId!!)
                        showMessageActions = false
                    }
                )
            }
        }
    }
}
