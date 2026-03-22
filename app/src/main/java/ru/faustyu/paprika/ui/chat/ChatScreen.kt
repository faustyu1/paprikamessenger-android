package ru.faustyu.paprika.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.util.VoiceRecorder
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onProfileClick: (String) -> Unit,
    onBack: () -> Unit,
    onCallClick: (Long, String, String) -> Unit = { _, _, _ -> },
    onVideoCircleClick: () -> Unit = {},
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { ru.faustyu.paprika.data.PrefsManager(context) }

    LaunchedEffect(Unit) {
        prefs.token?.let { viewModel.connect(it, chatId) }
    }

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }

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

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg = viewModel.snackbarMessage.value
    LaunchedEffect(snackbarMsg) {
        if (snackbarMsg != null) {
            snackbarHostState.showSnackbar(snackbarMsg)
            viewModel.snackbarMessage.value = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
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
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle,
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
                    if (!isGroup && otherId != null) {
                        IconButton(onClick = { onCallClick(otherId, title, "audio") }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call")
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Add Member") },
                                    onClick = { showMenu = false; showAddMemberDialog = true }
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
            } else {
                // Normal input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        pickMedia.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
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

                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.sendMessage(chatId, inputText)
                            inputText = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    } else {
                        // Voice button
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                VoiceRecorder.startRecording(context)
                                isRecordingVoice = true
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice message")
                        }
                        // Video circle button
                        IconButton(onClick = onVideoCircleClick) {
                            Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Video circle")
                        }
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
            reverseLayout = true
        ) {
            items(viewModel.messages) { message ->
                MessageBubble(message = message, chatId = chatId)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, chatId: String) {
    val baseUrl = NetworkModule.baseUrl.trimEnd('/')

    fun mediaUrl(path: String) =
        if (path.startsWith("http")) path else "$baseUrl$path"

    Box(
        modifier = Modifier.fillMaxWidth(),
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

                            "voice" -> {
                                VoiceMessagePlayer(url = mediaUrl(message.content))
                            }

                            else -> {
                                // text (and fallback)
                                Text(
                                    text = message.content,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        StatusLine(message = message)
                    }
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
