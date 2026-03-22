package ru.faustyu.paprika.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.util.VoiceRecorder

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DateSeparator(val dateStr: String) : ChatListItem()
    object UnreadSeparator : ChatListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onProfileClick: (String) -> Unit,
    onBack: () -> Unit,
    onCallClick: (Long, String, String) -> Unit = { _, _, _ -> },
    onVideoCircleClick: () -> Unit = {},
    onGroupInfoClick: (String) -> Unit = {},
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    LaunchedEffect(Unit) {
        prefs.token?.let { viewModel.connect(it, chatId) }
    }

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showForwardSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    // Load draft
    LaunchedEffect(chatId) {
        inputText = prefs.getDraft(chatId) ?: ""
    }
    // Save draft on leave
    DisposableEffect(chatId) {
        onDispose { prefs.setDraft(chatId, inputText) }
    }

    val replyTo = viewModel.replyToMessage.value
    val pinnedMsg = viewModel.pinnedMessage.value
    val typingText = viewModel.typingText.value
    val isChannel = viewModel.isChannel.value
    val canWrite = !isChannel || viewModel.isAdmin.value

    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) viewModel.sendTyping(chatId)
    }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showGifSheet by remember { mutableStateOf(false) }
    val stickers = remember { listOf("😀","😂","❤️","👍","🎉","😎","🤔","😢","🔥","✨","💯","🙏","😍","😭","🤣","😊","👀","💪","🎊","🌟") }

    val title = viewModel.chatTitle.value
    val subtitle = viewModel.chatSubtitle.value
    val otherId = viewModel.otherUserId.value
    val isGroup = viewModel.isGroup.value

    // Voice recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDuration = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDuration++
                if (recordingDuration >= 120) {
                    val path = VoiceRecorder.stopRecording()
                    isRecordingVoice = false
                    path?.let { viewModel.sendVoice(chatId, it, context) }
                }
            }
        }
    }

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
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
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
                                val av = user.avatar?.let { a ->
                                    if (a.startsWith("http")) a else NetworkModule.baseUrl.removeSuffix("/") + a
                                }
                                if (av != null) {
                                    AsyncImage(
                                        model = av,
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
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Close") }
            }
        )
    }

    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.sendImage(chatId, uri, context)
    }
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.sendFile(chatId, uri, context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg = viewModel.snackbarMessage.value
    LaunchedEffect(snackbarMsg) {
        if (snackbarMsg != null) {
            snackbarHostState.showSnackbar(snackbarMsg)
            viewModel.snackbarMessage.value = null
        }
    }

    // Long-press context menu
    selectedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = null,
            text = null,
            confirmButton = {},
            dismissButton = {},
            tonalElevation = 0.dp
        )
    }

    if (selectedMessage != null) {
        val msg = selectedMessage!!
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { selectedMessage = null }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Ответить") },
                    leadingContent = { Icon(Icons.Filled.Reply, null) },
                    modifier = Modifier.clickable { viewModel.setReply(msg); selectedMessage = null }
                )
                ListItem(
                    headlineContent = { Text("Переслать") },
                    leadingContent = { Icon(Icons.Filled.Forward, null) },
                    modifier = Modifier.clickable { showForwardSheet = true; selectedMessage = null }
                )
                ListItem(
                    headlineContent = { Text("Реакция") },
                    leadingContent = { Icon(Icons.Filled.EmojiEmotions, null) },
                    modifier = Modifier.clickable { showEmojiPicker = true }
                )
                if (msg.isMe) {
                    ListItem(
                        headlineContent = { Text("Редактировать") },
                        leadingContent = { Icon(Icons.Filled.Edit, null) },
                        modifier = Modifier.clickable {
                            editText = msg.content
                            showEditDialog = true
                            selectedMessage = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deleteMessage(msg.id)
                            selectedMessage = null
                        }
                    )
                }
                if (viewModel.isAdmin.value && isGroup) {
                    ListItem(
                        headlineContent = { Text("Закрепить") },
                        leadingContent = { Icon(Icons.Filled.PushPin, null) },
                        modifier = Modifier.clickable {
                            viewModel.pinMessage(chatId, msg.id)
                            selectedMessage = null
                        }
                    )
                }
            }
        }
    }

    // Emoji picker
    if (showEmojiPicker && selectedMessage != null) {
        val msg = selectedMessage!!
        val emojis = listOf("👍","❤️","😂","😮","😢","🔥","🎉","👎")
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false; selectedMessage = null },
            title = { Text("Выберите реакцию") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.clickable {
                                viewModel.reactToMessage(msg.id, emoji)
                                showEmojiPicker = false
                                selectedMessage = null
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Edit dialog
    if (showEditDialog && selectedMessage != null) {
        val msg = selectedMessage!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать сообщение") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editMessage(msg.id, editText)
                    showEditDialog = false
                    selectedMessage = null
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Forward sheet
    if (showForwardSheet && selectedMessage != null) {
        val msg = selectedMessage!!
        ForwardMessageSheet(
            chats = viewModel.forwardChats,
            onSelect = { chatDtoId ->
                viewModel.forwardMessage(msg.id, chatDtoId)
                showForwardSheet = false
                selectedMessage = null
            },
            onDismiss = { showForwardSheet = false }
        )
    }

    // GIF picker sheet
    if (showGifSheet) {
        GifPickerSheet(
            onGifSelected = { gifUrl ->
                viewModel.sendGif(chatId, gifUrl)
                showGifSheet = false
            },
            onDismiss = { showGifSheet = false }
        )
    }

    // Sticker picker sheet
    if (showStickerSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showStickerSheet = false }
        ) {
            Text(
                "Стикеры",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                modifier = Modifier.padding(horizontal = 8.dp).heightIn(max = 300.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(stickers.size) { index ->
                    val sticker = stickers[index]
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.sendMessage(chatId, sticker)
                                showStickerSheet = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sticker, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (searchMode) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Поиск в чате...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { otherId?.let { onProfileClick(it.toString()) } }
                        ) {
                            val avatarUrl = viewModel.chatAvatar.value?.takeIf { it.isNotBlank() }?.let { av ->
                                if (av.startsWith("http")) av else NetworkModule.baseUrl.removeSuffix("/") + av
                            }
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
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
                                        title.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    title, style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                val displaySubtitle = if (typingText.isNotBlank()) typingText else subtitle
                                if (displaySubtitle.isNotBlank()) {
                                    Text(
                                        displaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (typingText.isNotBlank()) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                    IconButton(onClick = { searchMode = !searchMode; if (!searchMode) searchText = "" }) {
                        Icon(if (searchMode) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search")
                    }
                    if (!searchMode) {
                        if (!isGroup && otherId != null) {
                            IconButton(onClick = { onCallClick(otherId, title, "audio") }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call")
                            }
                        }
                        if (isGroup) {
                            IconButton(onClick = { onGroupInfoClick(chatId) }) {
                                Icon(Icons.Filled.Info, contentDescription = "Group Info")
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (isGroup) {
                                    DropdownMenuItem(
                                        text = { Text("Добавить участника") },
                                        onClick = { showMenu = false; showAddMemberDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
            // Pinned message banner
            if (pinnedMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Закреплённое сообщение", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(
                                pinnedMsg.content.take(60),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        if (viewModel.isAdmin.value) {
                            IconButton(onClick = { viewModel.unpinMessage(chatId) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Reply bar
            if (replyTo != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ответ", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(replyTo.content.take(60), style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.clearReply() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isRecordingVoice) {
                // Voice recording bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        VoiceRecorder.cancelRecording()
                        isRecordingVoice = false
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Cancel", tint = Color.Red)
                    }
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        RecordingWaveform()
                    }
                    Text(
                        text = "%02d:%02d".format(recordingDuration / 60, recordingDuration % 60),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        val path = VoiceRecorder.stopRecording()
                        isRecordingVoice = false
                        path?.let { viewModel.sendVoice(chatId, it, context) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice")
                    }
                }
            } else if (!canWrite) {
                // Channel read-only banner
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Только администраторы могут писать",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Normal input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Disappearing timer toggle
                    val timerSec = viewModel.disappearingTimerSec.value
                    val timerLabel = when (timerSec) {
                        3600L -> "1ч"; 86400L -> "1д"; 604800L -> "7д"; 2592000L -> "30д"; else -> null
                    }
                    BadgedBox(
                        badge = { if (timerLabel != null) Badge { Text(timerLabel, style = MaterialTheme.typography.labelSmall) } },
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.disappearingTimerSec.value = when (timerSec) {
                                0L -> 3600L; 3600L -> 86400L; 86400L -> 604800L; 604800L -> 2592000L; else -> 0L
                            }
                        }) {
                            Icon(Icons.Filled.Timer, contentDescription = "Таймер исчезновения", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Attach: image or file
                    Box {
                        var showAttachMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showAttachMenu = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Прикрепить")
                        }
                        DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Фото") },
                                leadingIcon = { Icon(Icons.Filled.Image, null) },
                                onClick = {
                                    showAttachMenu = false
                                    pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    ))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Файл") },
                                leadingIcon = { Icon(Icons.Filled.InsertDriveFile, null) },
                                onClick = { showAttachMenu = false; pickFile.launch("*/*") }
                            )
                        }
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение") }
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.sendMessage(chatId, inputText)
                            inputText = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                        }
                    } else {
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                VoiceRecorder.startRecording(context)
                                isRecordingVoice = true
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Голосовое")
                        }
                        IconButton(onClick = onVideoCircleClick) {
                            Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Видеосообщение")
                        }
                        IconButton(onClick = { showStickerSheet = true }) {
                            Icon(Icons.Filled.EmojiEmotions, contentDescription = "Стикеры")
                        }
                        IconButton(onClick = { showGifSheet = true }) {
                            Text("GIF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            } // end Column
        }
    ) { padding ->
        val displayMessages = if (searchText.isBlank()) viewModel.messages
            else viewModel.messages.filter { it.content.contains(searchText, ignoreCase = true) }

        val unreadCount = viewModel.initialUnreadCount.value
        val chatItems = remember(displayMessages, unreadCount) {
            if (displayMessages.isEmpty()) return@remember emptyList<ChatListItem>()
            val result = mutableListOf<ChatListItem>()
            // messages are in reverse order (newest first), so unread separator goes before index = unreadCount
            val unreadSepIndex = if (unreadCount > 0 && unreadCount < displayMessages.size) unreadCount.toInt() else -1
            displayMessages.forEachIndexed { index, msg ->
                result.add(ChatListItem.MessageItem(msg))
                if (index == unreadSepIndex - 1) {
                    result.add(ChatListItem.UnreadSeparator)
                }
                val msgDate = run {
                    val inst = java.time.Instant.ofEpochSecond(msg.timestamp)
                    val today = java.time.LocalDate.now()
                    val ld = inst.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    when {
                        ld == today -> "Сегодня"
                        ld == today.minusDays(1) -> "Вчера"
                        else -> ld.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru")))
                    }
                }
                val nextDate = displayMessages.getOrNull(index + 1)?.let { nm ->
                    val inst = java.time.Instant.ofEpochSecond(nm.timestamp)
                    val today = java.time.LocalDate.now()
                    val ld = inst.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    when {
                        ld == today -> "Сегодня"
                        ld == today.minusDays(1) -> "Вчера"
                        else -> ld.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru")))
                    }
                }
                if (nextDate == null || nextDate != msgDate) {
                    result.add(ChatListItem.DateSeparator(msgDate))
                }
            }
            result
        }

        val listState = rememberLazyListState()
        val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }

        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                reverseLayout = true
            ) {
                items(
                    chatItems,
                    key = { item ->
                        when (item) {
                            is ChatListItem.MessageItem -> "msg_${item.message.id}"
                            is ChatListItem.DateSeparator -> "sep_${item.dateStr}"
                            is ChatListItem.UnreadSeparator -> "unread_sep"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatListItem.UnreadSeparator -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Непрочитанные сообщения",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        is ChatListItem.DateSeparator -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        item.dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        is ChatListItem.MessageItem -> {
                            MessageBubble(
                                message = item.message,
                                chatId = chatId,
                                onLongPress = { selectedMessage = item.message },
                                onReply = { viewModel.setReply(item.message) },
                                onMentionClick = { username ->
                                    viewModel.searchUsers(username)
                                    // Navigate to user profile if found
                                    val user = viewModel.searchResults.firstOrNull { it.username == username }
                                    if (user != null) onProfileClick(user.id.toString())
                                }
                            )
                        }
                    }
                }
            }
            if (showScrollToBottom) {
                val scope = rememberCoroutineScope()
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 8.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to bottom", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    chatId: String,
    onLongPress: () -> Unit = {},
    onReply: () -> Unit = {},
    onMentionClick: ((String) -> Unit)? = null
) {
    val baseUrl = NetworkModule.baseUrl.trimEnd('/')
    val uriHandler = LocalUriHandler.current
    fun mediaUrl(path: String) = if (path.startsWith("http")) path else "$baseUrl$path"

    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var hapticFired by remember { mutableStateOf(false) }
    val draggableState = rememberDraggableState { delta ->
        if (delta > 0) {
            val newOffset = (swipeOffset + delta).coerceAtMost(72f)
            if (newOffset >= 48f && swipeOffset < 48f && !hapticFired) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                hapticFired = true
            }
            swipeOffset = newOffset
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(swipeOffset.roundToInt(), 0) }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (swipeOffset >= 48f) onReply()
                    hapticFired = false
                    coroutineScope.launch {
                        animate(swipeOffset, 0f) { v, _ -> swipeOffset = v }
                    }
                }
            )
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        when (message.type) {
            "video_circle" -> {
                // Circular video — no background bubble
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
                ) {
                    VideoCircleBubble(url = mediaUrl(message.content))
                    StatusLine(message = message)
                }
            }

            else -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (message.isMe) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        when (message.type) {
                            "image" -> {
                                SubcomposeAsyncImage(
                                    model = mediaUrl(message.content),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .heightIn(max = 300.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    loading = {
                                        Box(
                                            modifier = Modifier.size(80.dp),
                                            contentAlignment = Alignment.Center
                                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                                    }
                                )
                            }

                            "gif" -> {
                                val gifLoader = remember {
                                    ImageLoader.Builder(LocalContext.current)
                                        .components { add(GifDecoder.Factory()) }
                                        .build()
                                }
                                val gifUrl = if (message.content.startsWith("http")) message.content
                                             else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + message.content
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(gifUrl).crossfade(false).build(),
                                    imageLoader = gifLoader,
                                    contentDescription = "GIF",
                                    modifier = Modifier
                                        .widthIn(max = 250.dp)
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            "voice" -> {
                                VoiceMessagePlayer(url = mediaUrl(message.content))
                            }

                            "file" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.widthIn(min = 160.dp, max = 240.dp)
                                ) {
                                    Icon(Icons.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        message.content.substringAfterLast("/").substringBefore("?"),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = {
                                        try { uriHandler.openUri(mediaUrl(message.content)) } catch (_: Exception) {}
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Download, contentDescription = "Скачать", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            "poll" -> {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Poll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Опрос", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    try {
                                        val pollData = com.google.gson.Gson().fromJson(message.content, com.google.gson.JsonObject::class.java)
                                        Text(pollData.get("question")?.asString ?: message.content, style = MaterialTheme.typography.bodyMedium)
                                        val options = pollData.getAsJsonArray("options")
                                        options?.forEach { opt ->
                                            val optObj = opt.asJsonObject
                                            val optText = optObj.get("text")?.asString ?: opt.asString
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(optText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                val votes = optObj.get("votes_count")?.asLong ?: 0L
                                                if (votes > 0) Text("$votes", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Text(message.content)
                                    }
                                }
                            }

                            else -> {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    // Forwarded label
                                    if (message.forwardedFromName != null) {
                                        Text(
                                            "Переслано от ${message.forwardedFromName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    // Reply preview
                                    if (message.replyToContent != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(4.dp)) {
                                                Box(modifier = Modifier.width(3.dp).height(32.dp)
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    message.replyToContent.take(60),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    // Message text with clickable @mentions and links
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        ClickableMessageText(
                                            text = message.content,
                                            modifier = Modifier.weight(1f, fill = false),
                                            onMentionClick = onMentionClick
                                        )
                                        if (message.edited) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("изм.", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    // Link preview card
                                    val urlRegex = remember { Regex("https?://[^\\s]+") }
                                    val firstUrl = remember(message.content) { urlRegex.find(message.content)?.value }
                                    if (firstUrl != null) {
                                        LinkPreviewCard(url = firstUrl)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        StatusLine(message = message)
                    }
                    // Reactions row
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            message.reactions.filter { it.count > 0 }.forEach { r ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (r.isMine) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "${r.emoji} ${r.count}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
private fun ClickableMessageText(
    text: String,
    modifier: Modifier = Modifier,
    onMentionClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val baseStyle = MaterialTheme.typography.bodyMedium

    val annotated = remember(text) {
        buildAnnotatedString {
            // Combined regex: formatting markers + URLs + mentions
            val combinedRegex = Regex("""\*\*(.*?)\*\*|_(.*?)_|`(.*?)`|\*(.*?)\*|(https?://[^\s]+|@\w+)""")
            var cursor = 0
            combinedRegex.findAll(text).forEach { match ->
                if (match.range.first > cursor) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(cursor, match.range.first))
                    }
                }
                when {
                    match.groups[1] != null -> {
                        // **bold**
                        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) {
                            append(match.groups[1]!!.value)
                        }
                    }
                    match.groups[2] != null -> {
                        // _italic_
                        withStyle(SpanStyle(color = textColor, fontStyle = FontStyle.Italic)) {
                            append(match.groups[2]!!.value)
                        }
                    }
                    match.groups[3] != null -> {
                        // `code`
                        withStyle(SpanStyle(color = textColor, fontFamily = FontFamily.Monospace, background = Color.Gray.copy(alpha = 0.2f))) {
                            append(match.groups[3]!!.value)
                        }
                    }
                    match.groups[4] != null -> {
                        // *italic*
                        withStyle(SpanStyle(color = textColor, fontStyle = FontStyle.Italic)) {
                            append(match.groups[4]!!.value)
                        }
                    }
                    match.groups[5] != null -> {
                        // URL or @mention
                        val value = match.groups[5]!!.value
                        val isMention = value.startsWith("@")
                        pushStringAnnotation(if (isMention) "MENTION" else "URL", value)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(value)
                        }
                        pop()
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                withStyle(SpanStyle(color = textColor)) { append(text.substring(cursor)) }
            }
        }
    }

    ClickableText(
        text = annotated,
        style = baseStyle,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()
                ?.let { try { uriHandler.openUri(it.item) } catch (_: Exception) {} }
            annotated.getStringAnnotations("MENTION", offset, offset).firstOrNull()
                ?.let { onMentionClick?.invoke(it.item.removePrefix("@")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardMessageSheet(
    chats: List<ru.faustyu.paprika.data.network.ChatDto>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Переслать в...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            items(chats) { chat ->
                val avatarUrl = chat.avatar.takeIf { it.isNotBlank() }?.let { av ->
                    if (av.startsWith("http")) av else NetworkModule.baseUrl.removeSuffix("/") + av
                }
                ListItem(
                    headlineContent = { Text(chat.title) },
                    leadingContent = {
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
                                    chat.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onSelect(chat.id) }
                )
            }
        }
    }
}

@Composable
private fun LinkPreviewCard(url: String) {
    var preview by remember(url) { mutableStateOf<ru.faustyu.paprika.util.LinkPreviewData?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(url) {
        preview = ru.faustyu.paprika.util.LinkPreviewFetcher.fetch(url)
        loaded = true
    }
    val p = preview ?: return
    Spacer(Modifier.height(4.dp))
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { try { uriHandler.openUri(url) } catch (_: Exception) {} }
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
            if (p.imageUrl != null) {
                AsyncImage(
                    model = p.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(p.title, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (p.description != null) {
                    Text(p.description, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(message: Message) {
    Column(horizontalAlignment = Alignment.End) {
        val timeString = remember(message.timestamp) {
            try {
                val instant = java.time.Instant.ofEpochSecond(message.timestamp)
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(instant)
            } catch (_: Exception) { "" }
        }
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (message.isMe) {
            if (message.status == "uploading") {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
            } else {
                val icon = when (message.status) {
                    "read" -> Icons.Filled.DoneAll
                    "delivered" -> Icons.Filled.DoneAll
                    else -> Icons.Filled.Done
                }
                val tint = if (message.status == "read") Color(0xFF2196F3)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
            }
        }
    }
}

@Composable
private fun VoiceMessagePlayer(url: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableIntStateOf(0) }
    val mediaPlayer = remember { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        mediaPlayer.setOnPreparedListener {
            durationSec = it.duration / 1000
            prepared = true
        }
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            progress = 0f
        }
        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {}
        onDispose { mediaPlayer.release() }
    }

    // Progress tracking
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(200)
            if (mediaPlayer.isPlaying) {
                progress = mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 160.dp, max = 240.dp)) {
        IconButton(
            onClick = {
                if (!prepared) return@IconButton
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else {
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        VoiceWaveform(progress = progress, modifier = Modifier.weight(1f).height(32.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (durationSec > 0) "%d:%02d".format(durationSec / 60, durationSec % 60) else "…",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun VoiceWaveform(progress: Float, modifier: Modifier = Modifier) {
    val barCount = 28
    val heights = remember {
        val rng = Random(42)
        (0 until barCount).map { rng.nextFloat() * 0.75f + 0.25f }
    }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val barW = size.width / barCount
        heights.forEachIndexed { i, h ->
            val barH = size.height * h
            val filled = i.toFloat() / barCount <= progress
            drawRoundRect(
                color = if (filled) primary else outline.copy(alpha = 0.4f),
                topLeft = Offset(i * barW + 1f, (size.height - barH) / 2),
                size = Size(barW - 2f, barH),
                cornerRadius = CornerRadius(2f)
            )
        }
    }
}

@Composable
private fun RecordingWaveform() {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            tick++
        }
    }
    val barCount = 28
    val primary = Color.Red

    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        val barW = size.width / barCount
        val rng = Random(tick / 3)
        repeat(barCount) { i ->
            val h = size.height * (rng.nextFloat() * 0.75f + 0.25f)
            drawRoundRect(
                color = primary.copy(alpha = 0.8f),
                topLeft = Offset(i * barW + 1f, (size.height - h) / 2),
                size = Size(barW - 2f, h),
                cornerRadius = CornerRadius(2f)
            )
        }
    }
}

@Composable
private fun VideoCircleBubble(url: String) {
    var isPlaying by remember { mutableStateOf(false) }
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }

    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .clickable {
                val vv = videoViewRef.value ?: return@clickable
                if (isPlaying) {
                    vv.pause()
                    isPlaying = false
                } else {
                    vv.start()
                    isPlaying = true
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).also { vv ->
                    videoViewRef.value = vv
                    vv.setVideoURI(Uri.parse(url))
                    vv.setOnPreparedListener { mp -> mp.isLooping = true }
                    vv.setOnErrorListener { _, _, _ -> true }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
