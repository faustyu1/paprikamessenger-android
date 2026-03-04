package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import ru.faustyu.paprika.data.network.Story

data class ChatItem(val id: String, val name: String, val lastMessage: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit, 
    onSearchClick: () -> Unit, 
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatListViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onUrlChanged: (String) -> Unit
) {
    // Refresh chats whenever this screen is active/visible
    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }

    val chats = viewModel.chats
    
    var debugTaps by remember { mutableIntStateOf(0) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf("") }
    
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUrlDialog = false 
                debugTaps = 0
            },
            title = { Text("Update Backend URL") },
            text = {
                 Column {
                     Text("Current: " + ru.faustyu.paprika.data.network.NetworkModule.baseUrl)
                     Spacer(modifier = Modifier.height(8.dp))
                     OutlinedTextField(
                         value = tempUrl,
                         onValueChange = { tempUrl = it },
                         label = { Text("New URL") },
                         placeholder = { Text("http://...") }
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
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUrlDialog = false 
                    debugTaps = 0
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Paprika Chats",
                        modifier = Modifier.clickable {
                            debugTaps++
                            if (debugTaps >= 3) {
                                tempUrl = ru.faustyu.paprika.data.network.NetworkModule.baseUrl
                                showUrlDialog = true
                            }
                        }
                    ) 
                },
                navigationIcon = {
                     // Removed AccountCircle from here as we add a prominent header below
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                onSearchClick() 
            }) {
                Icon(Icons.Filled.Edit, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
             // Optional: Button to create group explicitly in the list header
             Button(
                 onClick = onCreateGroupClick,
                 modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
             ) {
                 Text("Create New Group")
             }
            // Stories Section
            ru.faustyu.paprika.ui.stories.StoriesBar(onStoryClick = { 
                // Navigate to view story 
            })
            Divider()
            
            LazyColumn {
                // Prominent Profile Header
                item {
                    viewModel.currentUser?.let { user ->
                        val avatarUrl = user.avatar.takeIf { !it.isNullOrBlank() }?.let { av ->
                             if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { onProfileClick() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (avatarUrl != null) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "My Avatar",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                (user.first_name?.takeIf { it.isNotBlank() } ?: user.username).take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = if (!user.first_name.isNullOrBlank()) "${user.first_name} ${user.last_name ?: ""}" else user.username,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "View my profile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Sorting...
                items(chats.sortedByDescending { it.last_message_at }) { chat ->
                    // Filter: Show only if it has a last message OR is the system chat (ID 1)
                    if (chat.last_message_at > 0 || chat.id == 1L) {
                        val avatarUrl = chat.avatar.takeIf { !it.isNullOrBlank() }?.let { av ->
                             if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                        }

                        val timeString = remember(chat.last_message_at) {
                             if (chat.last_message_at == 0L) return@remember ""
                             val instant = java.time.Instant.ofEpochSecond(chat.last_message_at)
                             val zoneId = java.time.ZoneId.systemDefault()
                             val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                             instant.atZone(zoneId).format(formatter)
                        }

                        ImprovedChatListItem(
                            chat = ImprovedChatItemData(
                                id = chat.id,
                                title = chat.title,
                                lastMessage = chat.last_message_preview ?: "No messages yet",
                                lastMessageTime = timeString,
                                avatarUrl = avatarUrl,
                                unreadCount = chat.unread_count.toInt(),
                                isOnline = false, // TODO: status from websocket
                                isPinned = false,
                                lastMessageType = if (chat.last_message_preview?.startsWith("/media/") == true) "photo" else "text",
                                isVerified = chat.is_official
                            ),
                            onClick = { 
                                val idStr = if (chat.id == 1L) "paprika_system" else chat.id.toString()
                                onChatClick(idStr) 
                            },
                            isBot = chat.is_bot
                        )
                    }
                }
            }
        }
    }
}
