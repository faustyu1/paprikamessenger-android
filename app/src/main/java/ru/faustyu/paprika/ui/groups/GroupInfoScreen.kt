package ru.faustyu.paprika.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.network.GroupMemberDto
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.UpdateChatRequest

class GroupInfoViewModel : ViewModel() {
    var title = androidx.compose.runtime.mutableStateOf("")
    var description = androidx.compose.runtime.mutableStateOf("")
    var avatar = androidx.compose.runtime.mutableStateOf("")
    var ownerId = androidx.compose.runtime.mutableStateOf(0L)
    var myId = androidx.compose.runtime.mutableStateOf(0L)
    var myRole = androidx.compose.runtime.mutableStateOf("member")
    var members = androidx.compose.runtime.mutableStateListOf<GroupMemberDto>()
    var snackbar = androidx.compose.runtime.mutableStateOf<String?>(null)
    var isLoading = androidx.compose.runtime.mutableStateOf(true)

    fun load(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val me = NetworkModule.api.getMyProfile().body()
                if (me != null) myId.value = me.id

                val chatRes = NetworkModule.api.getChat(chatId)
                if (chatRes.isSuccessful) {
                    chatRes.body()?.let {
                        title.value = it.title
                        description.value = it.description ?: ""
                        avatar.value = it.avatar ?: ""
                        ownerId.value = it.owner_id
                        inviteLink.value = it.invite_link
                    }
                }

                val membersRes = NetworkModule.api.getChatMembers(chatId)
                if (membersRes.isSuccessful) {
                    members.clear()
                    membersRes.body()?.let { list ->
                        members.addAll(list.sortedWith(compareBy(
                            { if (it.role == "admin") 0 else 1 },
                            { it.username }
                        )))
                        myRole.value = list.find { it.user_id == myId.value }?.role ?: "member"
                    }
                }
            } catch (_: Exception) {
                snackbar.value = "Не удалось загрузить данные"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateGroup(chatId: String, newTitle: String, newDesc: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.updateChat(chatId, UpdateChatRequest(newTitle, newDesc))
                if (res.isSuccessful) {
                    title.value = newTitle
                    description.value = newDesc
                    snackbar.value = "Группа обновлена"
                    onDone()
                } else {
                    snackbar.value = "Ошибка обновления"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }

    fun kickMember(chatId: String, userId: Long) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.removeChatMember(chatId, userId)
                if (res.isSuccessful) {
                    members.removeAll { it.user_id == userId }
                    snackbar.value = "Участник удалён"
                } else {
                    snackbar.value = "Не удалось удалить участника"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }

    fun promoteAdmin(chatId: String, userId: Long) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.promoteChatMember(chatId, userId)
                if (res.isSuccessful) {
                    val idx = members.indexOfFirst { it.user_id == userId }
                    if (idx >= 0) members[idx] = members[idx].copy(role = "admin")
                    snackbar.value = "Назначен администратором"
                }
            } catch (_: Exception) {}
        }
    }

    fun demoteAdmin(chatId: String, userId: Long) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.demoteChatMember(chatId, userId)
                if (res.isSuccessful) {
                    val idx = members.indexOfFirst { it.user_id == userId }
                    if (idx >= 0) members[idx] = members[idx].copy(role = "member")
                    snackbar.value = "Права администратора сняты"
                }
            } catch (_: Exception) {}
        }
    }

    fun leaveGroup(chatId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.leaveChat(chatId)
                if (res.isSuccessful) {
                    onDone()
                } else {
                    snackbar.value = "Не удалось выйти из группы"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }

    fun deleteGroup(chatId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.deleteChat(chatId)
                if (res.isSuccessful) {
                    onDone()
                } else {
                    snackbar.value = "Не удалось удалить группу"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }

    var inviteLink = androidx.compose.runtime.mutableStateOf<String?>(null)

    fun generateInvite(chatId: String) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.generateInvite(chatId)
                if (res.isSuccessful) {
                    inviteLink.value = res.body()?.get("invite_link")
                    snackbar.value = "Ссылка создана"
                } else {
                    snackbar.value = "Ошибка создания ссылки"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }

    fun revokeInvite(chatId: String) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.revokeInvite(chatId)
                if (res.isSuccessful) {
                    inviteLink.value = null
                    snackbar.value = "Ссылка отозвана"
                } else {
                    snackbar.value = "Ошибка отзыва ссылки"
                }
            } catch (_: Exception) {
                snackbar.value = "Ошибка сети"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatId: String,
    onBack: () -> Unit,
    onGroupDeleted: () -> Unit,
    onMediaClick: (String) -> Unit = {},
    viewModel: GroupInfoViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg = viewModel.snackbar.value
    LaunchedEffect(snackbarMsg) {
        if (snackbarMsg != null) {
            snackbarHostState.showSnackbar(snackbarMsg)
            viewModel.snackbar.value = null
        }
    }

    LaunchedEffect(chatId) { viewModel.load(chatId) }

    val isOwner = viewModel.myId.value == viewModel.ownerId.value
    val isAdmin = viewModel.myRole.value == "admin" || isOwner
    val clipboardManager = LocalClipboardManager.current

    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<GroupMemberDto?>(null) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать группу") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateGroup(chatId, editTitle, editDesc) { showEditDialog = false }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Выйти из группы?") },
            text = { Text("Вы больше не сможете видеть сообщения этой группы.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveGroup(chatId) { onGroupDeleted() }
                    showLeaveDialog = false
                }) { Text("Выйти", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить группу?") },
            text = { Text("Группа и все сообщения будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(chatId) { onGroupDeleted() }
                    showDeleteDialog = false
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    memberToKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Удалить участника?") },
            text = { Text("Удалить @${member.username} из группы?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.kickMember(chatId, member.user_id)
                    memberToKick = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Информация о группе") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = {
                            editTitle = viewModel.title.value
                            editDesc = viewModel.description.value
                            showEditDialog = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            // Group avatar + name header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarUrl = viewModel.avatar.value.takeIf { it.isNotBlank() }?.let {
                        if (it.startsWith("http")) it else NetworkModule.baseUrl.removeSuffix("/") + it
                    }
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                viewModel.title.value.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(viewModel.title.value, style = MaterialTheme.typography.titleLarge)
                    if (viewModel.description.value.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            viewModel.description.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${viewModel.members.size} участников",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                }
            }

            // Members section header
            item {
                Text(
                    "Участники",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Members list
            items(viewModel.members) { member ->
                MemberRow(
                    member = member,
                    isOwner = member.user_id == viewModel.ownerId.value,
                    canManage = isOwner && member.user_id != viewModel.myId.value,
                    onKick = { memberToKick = member },
                    onPromote = { viewModel.promoteAdmin(chatId, member.user_id) },
                    onDemote = { viewModel.demoteAdmin(chatId, member.user_id) }
                )
            }

            // Actions section
            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                if (isAdmin) {
                    val link = viewModel.inviteLink.value
                    if (link != null) {
                        ListItem(
                            headlineContent = { Text("Пригласительная ссылка", style = MaterialTheme.typography.bodySmall) },
                            supportingContent = { Text(link, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { Icon(Icons.Filled.Link, contentDescription = null) },
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(link))
                                viewModel.snackbar.value = "Ссылка скопирована"
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(link))
                                        viewModel.snackbar.value = "Ссылка скопирована"
                                    }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
                                    if (isOwner) {
                                        IconButton(onClick = { viewModel.revokeInvite(chatId) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Создать пригласительную ссылку") },
                            leadingContent = { Icon(Icons.Filled.Link, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.generateInvite(chatId) }
                        )
                    }
                    HorizontalDivider()
                }

                ListItem(
                    headlineContent = { Text("Медиафайлы") },
                    supportingContent = { Text("Фото и видео") },
                    leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable { onMediaClick(chatId) }
                )
                HorizontalDivider()

                if (!isOwner) {
                    ListItem(
                        headlineContent = { Text("Выйти из группы", color = MaterialTheme.colorScheme.error) },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable { showLeaveDialog = true }
                    )
                }

                if (isOwner) {
                    ListItem(
                        headlineContent = { Text("Удалить группу", color = MaterialTheme.colorScheme.error) },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable { showDeleteDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMemberDto,
    isOwner: Boolean,
    canManage: Boolean,
    onKick: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val name = if (member.first_name.isNotBlank() || member.last_name.isNotBlank())
                    "${member.first_name} ${member.last_name}".trim()
                else member.username
                Text(name)
                if (isOwner) {
                    Spacer(Modifier.width(6.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text("owner", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (member.role == "admin") {
                    Spacer(Modifier.width(6.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("admin", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        supportingContent = { Text("@${member.username}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            val avatarUrl = member.avatar.takeIf { it.isNotBlank() }?.let {
                if (it.startsWith("http")) it else NetworkModule.baseUrl.removeSuffix("/") + it
            }
            Box {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
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
                            member.username.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                if (member.is_online) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        },
        trailingContent = {
            if (canManage) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (member.role != "admin") {
                            DropdownMenuItem(
                                text = { Text("Назначить админом") },
                                onClick = { onPromote(); showMenu = false }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Снять права админа") },
                                onClick = { onDemote(); showMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Удалить из группы", color = MaterialTheme.colorScheme.error) },
                            onClick = { onKick(); showMenu = false }
                        )
                    }
                }
            }
        }
    )
}
