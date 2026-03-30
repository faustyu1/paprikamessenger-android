package ru.faustyu.paprika.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.Story

// Telegram-style gradient for unread story ring
private val storyGradient = listOf(Color(0xFF4CAF50), Color(0xFF00BCD4))
private val storyGradientSeen = listOf(Color(0xFF555555), Color(0xFF555555))

@Composable
fun StoriesBar(
    myAvatarUrl: String? = null,
    myName: String = "My Story",
    onStoryClick: (Story) -> Unit,
    viewModel: StoriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadStories() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var mediaType by remember { mutableStateOf("image") }

    val pickImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            showCreateDialog = true
        }
    }

    if (showCreateDialog && selectedMediaUri != null) {
        CreateStoryDialog(
            uri = selectedMediaUri!!,
            mediaType = mediaType,
            onDismiss = { showCreateDialog = false },
            onPublish = { caption ->
                viewModel.createStory(selectedMediaUri.toString(), mediaType, caption)
                showCreateDialog = false
            }
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // My Story (always first)
        item {
            MyStoryItem(
                avatarUrl = myAvatarUrl,
                name = myName,
                onAddImage = { mediaType = "image"; pickImageLauncher.launch("image/*") },
                onAddVideo = { mediaType = "video"; pickImageLauncher.launch("video/*") }
            )
        }

        items(viewModel.stories) { story ->
            StoryItem(story, onStoryClick)
        }
    }
}

@Composable
private fun MyStoryItem(
    avatarUrl: String?,
    name: String,
    onAddImage: () -> Unit,
    onAddVideo: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable { showMenu = true }
    ) {
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Avatar circle with no story ring (or grey ring)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add button overlay
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "My Story",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Photo") },
                onClick = { showMenu = false; onAddImage() },
                leadingIcon = { Icon(Icons.Default.Image, null) }
            )
            DropdownMenuItem(
                text = { Text("Video") },
                onClick = { showMenu = false; onAddVideo() },
                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
            )
        }
    }
}

@Composable
fun StoryItem(story: Story, onClick: (Story) -> Unit) {
    val imageUrl = remember(story.media_url) {
        if (story.media_url.startsWith("http") || story.media_url.startsWith("content://")) story.media_url
        else NetworkModule.baseUrl.removeSuffix("/") + story.media_url
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable { onClick(story) }
    ) {
        // Story ring with gradient (unread = gradient, seen = grey — using gradient for all here)
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(
                    Brush.linearGradient(storyGradient),
                    CircleShape
                )
                .padding(2.5.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
                .padding(2.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (story.media_type == "video") {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "user_${story.user_id}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CreateStoryDialog(
    uri: android.net.Uri,
    mediaType: String,
    onDismiss: () -> Unit,
    onPublish: (String) -> Unit
) {
    var caption by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (mediaType == "image") {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Add a caption...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                        Button(onClick = { onPublish(caption) }) { Text("Publish") }
                    }
                }
            }
        }
    }
}
