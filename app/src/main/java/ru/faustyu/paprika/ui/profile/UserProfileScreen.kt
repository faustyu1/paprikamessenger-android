package ru.faustyu.paprika.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Person
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
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = user.avatar?.let { av ->
                            if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                        }
                        Box {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(160.dp),
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initial = (user.first_name?.takeIf { it.isNotBlank() } ?: user.username).take(1).uppercase()
                                        Text(
                                            text = initial,
                                            style = MaterialTheme.typography.displayLarge,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            if (user.is_online) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(androidx.compose.ui.graphics.Color(0xFF4CAF50), CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val displayName = if (!user.first_name.isNullOrBlank()) {
                        "${user.first_name} ${user.last_name ?: ""}".trim()
                    } else {
                        user.username
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (user.is_online) "в сети" else if (user.last_seen > 0) {
                            val diff = System.currentTimeMillis() / 1000 - user.last_seen
                            when {
                                diff < 60 -> "был(а) только что"
                                diff < 3600 -> "был(а) ${diff / 60} мин. назад"
                                diff < 86400 -> "был(а) ${diff / 3600} ч. назад"
                                else -> "был(а) давно"
                            }
                        } else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.is_online) androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.startChat(user.id) { cid -> onChatClick(cid.toString()) }
                        },
                        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Написать сообщение")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!user.bio.isNullOrBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "О себе",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = user.bio,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            } else {
                 Text("User not found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
