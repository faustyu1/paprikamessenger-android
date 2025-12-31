package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onProfileClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ru.faustyu.paprika.data.PrefsManager(context) }

    LaunchedEffect(Unit) {
         prefs.token?.let { token ->
             viewModel.connect(token, chatId)
         }
    }

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    
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
                IconButton(onClick = {
                    viewModel.sendMessage(chatId, inputText)
                    inputText = ""
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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
            items(viewModel.messages) { message ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                   Surface(
                       shape = RoundedCornerShape(8.dp),
                       color = if (message.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                       modifier = Modifier.padding(4.dp)
                   ) {
                       Row(
                           modifier = Modifier.padding(8.dp),
                           verticalAlignment = Alignment.Bottom
                       ) {
                           // Message Content logic
                           val isImage = (message.content.startsWith("/media/") || message.content.startsWith("http")) && !message.content.contains(" ") // Rough check if type not available, but VM has 'type' in Message? No, we need to add type to Message UI model if we rely on it.
                           // Actually Message data class in VM has content, isMe, status, timestamp. NO TYPE.
                           // Need to add type to Message UI class in VM.
                           // For now let's hack it: if starts with /media/, it's image.
                           
                           if (message.content.startsWith("/media/")) {
                               val imageUrl = ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + message.content
                               
                               coil.compose.SubcomposeAsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                               )
                           } else {
                               Text(
                                   text = message.content,
                                   color = MaterialTheme.colorScheme.onSurface,
                                   modifier = Modifier.weight(1f, fill = false)
                               )
                           }

                           Spacer(modifier = Modifier.width(8.dp))

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

                           Text(
                               text = timeString,
                               style = MaterialTheme.typography.labelSmall,
                               color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                           )
                           
                           if (message.isMe) {
                               Spacer(modifier = Modifier.width(4.dp))
                               
                               if (message.status == "uploading") {
                                   CircularProgressIndicator(
                                       modifier = Modifier.size(12.dp),
                                       strokeWidth = 2.dp,
                                       color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                   )
                               } else {
                                   val icon = when(message.status) {
                                       "sent" -> androidx.compose.material.icons.Icons.Filled.Done
                                       "delivered" -> androidx.compose.material.icons.Icons.Filled.DoneAll
                                       "read" -> androidx.compose.material.icons.Icons.Filled.DoneAll
                                       else -> androidx.compose.material.icons.Icons.Filled.Done
                                   }
                                   val tint = if (message.status == "read") Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                   
                                   Icon(
                                       imageVector = icon,
                                       contentDescription = message.status,
                                       modifier = Modifier.size(16.dp),
                                       tint = tint
                                   )
                               }
                           }
                       }
                   }
                }
            }
        }
    }
}
