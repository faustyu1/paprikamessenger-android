package ru.faustyu.paprika.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import ru.faustyu.paprika.data.network.UserPublic

class UserProfileViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    var user by mutableStateOf<UserPublic?>(null)
    var isLoading by mutableStateOf(false)
    var chatId by mutableStateOf<Long?>(null)

    // DB access
    private val db = ru.faustyu.paprika.data.db.DatabaseModule.provideDatabase(application)
    private val userDao = db.userDao()

    fun startChat(userId: Long, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ru.faustyu.paprika.data.network.NetworkModule.api.createChat(
                    ru.faustyu.paprika.data.network.CreateChatRequest(
                        type = 0,
                        title = "",
                        description = "",
                        recipient_id = userId
                    )
                )
                if (res.isSuccessful) {
                    res.body()?.let { onDone(it.id) }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadUser(userId: String) {
        val uid = userId.toLongOrNull() ?: return

        // 1. Observe Cache (SSoT)
        viewModelScope.launch {
            userDao.getUser(uid).collect { entity ->
                entity?.let {
                    user = UserPublic(
                        id = it.id,
                        username = it.username,
                        first_name = it.firstName,
                        last_name = it.lastName,
                        bio = it.bio,
                        avatar = it.avatar,
                        public_key = "" // Not stored in local DB yet
                    )
                }
            }
        }

        // 2. Refresh from Network
        viewModelScope.launch {
            // Only show loader if we have NO data at all
            if (user == null) isLoading = true
            
            try {
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.getUserProfile(userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Update Cache
                        userDao.insertUser(ru.faustyu.paprika.data.db.UserEntity(
                            id = body.id,
                            username = body.username,
                            firstName = body.first_name,
                            lastName = body.last_name,
                            bio = body.bio,
                            avatar = body.avatar
                        ))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onChatClick: (String) -> Unit = {},
    viewModel: UserProfileViewModel = viewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val user = viewModel.user

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarUrl = user.avatar?.takeIf { it.isNotBlank() }?.let { av ->
                        if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                    }
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initial = (user.first_name?.takeIf { it.isNotBlank() } ?: user.username).take(1).uppercase()
                                    if (initial.isNotEmpty()) {
                                        Text(
                                            text = initial,
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(0.5f),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val displayName = if (!user.first_name.isNullOrBlank()) {
                        "${user.first_name} ${user.last_name ?: ""}".trim()
                    } else {
                        user.username
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val lastSeenText = if (user.is_online) "online" else if (user.last_seen > 0) {
                        val diff = System.currentTimeMillis() / 1000 - user.last_seen
                        when {
                            diff < 60 -> "last seen just now"
                            diff < 3600 -> "last seen ${diff / 60} mins ago"
                            diff < 86400 -> "last seen ${diff / 3600} hours ago"
                            else -> "last seen recently"
                        }
                    } else "last seen recently"

                    Text(
                        text = lastSeenText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "ID: ${user.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            icon = Icons.Default.ChatBubbleOutline,
                            label = "Message",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.startChat(user.id) { cid -> onChatClick(cid.toString()) }
                            }
                        )
                        ActionCard(
                            icon = Icons.Default.NotificationsOff,
                            label = "Mute",
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO */ }
                        )
                        ActionCard(
                            icon = Icons.Default.Call,
                            label = "Call",
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO */ }
                        )
                        ActionCard(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!user.bio.isNullOrBlank()) {
                                InfoRow(
                                    title = user.bio,
                                    subtitle = "Bio",
                                    trailingIcon = null
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            }

                            InfoRow(
                                title = "@${user.username}",
                                subtitle = "Username",
                                trailingIcon = Icons.Default.QrCode
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                            InfoRow(
                                title = "TODO",
                                subtitle = "Birthday",
                                trailingIcon = null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                 Text("User not found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun InfoRow(title: String, subtitle: String, trailingIcon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
