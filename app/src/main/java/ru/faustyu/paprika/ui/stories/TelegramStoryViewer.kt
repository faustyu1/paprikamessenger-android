package ru.faustyu.paprika.ui.stories

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import ru.faustyu.paprika.data.network.Story

/**
 * Telegram-style story viewer with progress indicators and swipe navigation
 */
@Composable
fun TelegramStoryViewer(
    stories: List<Story>,
    initialStoryIndex: Int = 0,
    onDismiss: () -> Unit,
    onSendReply: (Story, String) -> Unit = { _, _ -> }
) {
    var currentIndex by remember { mutableStateOf(initialStoryIndex) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    // Auto-pause when typing
    LaunchedEffect(replyText) {
        if (replyText.isNotEmpty()) {
            isPaused = true
        }
    }

    val currentStory = stories.getOrNull(currentIndex)

    if (currentStory == null) {
        onDismiss()
        return
    }

    // Auto-advance timer (5 seconds per story)
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused && currentIndex < stories.lastIndex) {
            delay(5000)
            currentIndex++
        } else if (currentIndex >= stories.lastIndex) {
            delay(5000)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 50) onDismiss()
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            when {
                                offset.x < size.width / 3 && currentIndex > 0 -> currentIndex--
                                offset.x > size.width * 2 / 3 && currentIndex < stories.lastIndex -> currentIndex++
                                else -> isPaused = !isPaused
                            }
                        },
                        onLongPress = { isPaused = true },
                        onDoubleTap = { /* Like/React */ }
                    )
                }
        ) {
            // Story image/video
            val imageUrl = remember(currentStory.media_url) {
                if (currentStory.media_url.startsWith("http")) currentStory.media_url
                else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + currentStory.media_url
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Progress indicators at top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.6f), Color.Transparent)
                        )
                    )
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Progress bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        StoryProgressIndicator(
                            modifier = Modifier.weight(1f),
                            isActive = index == currentIndex,
                            isCompleted = index < currentIndex,
                            isPaused = isPaused
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Header (user info)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStory.user?.username ?: "User ${currentStory.user_id}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "3h ago", // TODO: calculate actual time
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.7f)
                        )
                    }

                    // Mute button
                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Caption
            if (!currentStory.caption.isNullOrBlank()) {
                Text(
                    text = currentStory.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
                        .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }

            // Reply input
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to story...", color = Color.White.copy(0.6f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (replyText.isNotBlank()) {
                    IconButton(onClick = {
                        onSendReply(currentStory, replyText)
                        replyText = ""
                        isPaused = false
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun StoryProgressIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    isCompleted: Boolean,
    isPaused: Boolean
) {
    val progress = remember { Animatable(if (isCompleted) 1f else 0f) }

    LaunchedEffect(isActive, isPaused) {
        if (isActive && !isPaused) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White.copy(0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(if (isCompleted) 1f else if (isActive) progress.value else 0f)
                .background(Color.White)
        )
    }
}
