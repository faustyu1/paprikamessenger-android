package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MessageBubbleData(
    val id: Long,
    val content: String,
    val time: String,
    val isOwnMessage: Boolean,
    val isRead: Boolean,
    val isEdited: Boolean = false,
    val replyToContent: String? = null,
    val forwardedFromUser: String? = null,
    val showSenderName: Boolean = false,
    val senderName: String? = null,
    val type: String = "text",
    val isPlaying: Boolean = false
)

/**
 * Telegram-style message bubble with improved design
 */
@Composable
fun ImprovedMessageBubble(
    message: MessageBubbleData,
    onLongClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val bubbleColor = if (message.isOwnMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val alignment = if (message.isOwnMessage) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        // Forwarded header
        if (message.forwardedFromUser != null) {
            Text(
                text = "Forwarded from ${message.forwardedFromUser}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        // Sender name (for group chats)
        if (message.showSenderName && message.senderName != null && !message.isOwnMessage) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        // Message bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = if (message.isOwnMessage) 12.dp else 4.dp,
                topEnd = if (message.isOwnMessage) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Reply preview
                if (message.replyToContent != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.replyToContent,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Message content
                val isImage = (message.content.startsWith("/media/") || message.content.startsWith("http")) && !message.content.contains(" ")
                
                when {
                    message.type == "voice" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            IconButton(onClick = onPlayClick) {
                                Icon(
                                    imageVector = if (message.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (message.isPlaying) "Pause" else "Play",
                                    tint = if (message.isOwnMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Simple waveform placeholder
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .background(
                                        Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                        }
                    }
                    message.type == "video_circle" -> {
                        val videoUrl = if (message.content.startsWith("http")) {
                            message.content
                        } else {
                            ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + message.content
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video preview placeholder (first frame or just an icon)
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video Circle",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    message.type == "image" || isImage -> {
                        val imageUrl = if (message.content.startsWith("http")) {
                            message.content
                        } else {
                            ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + message.content
                        }
                        
                        coil.compose.SubcomposeAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        )
                    }
                    else -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isOwnMessage) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Time and status row
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = message.time,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    if (message.isOwnMessage) {
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Delivered",
                            tint = if (message.isRead) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
