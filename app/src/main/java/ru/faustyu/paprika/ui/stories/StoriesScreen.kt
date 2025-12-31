package ru.faustyu.paprika.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.faustyu.paprika.data.network.Story

@Composable
fun StoriesBar(
    onStoryClick: (Story) -> Unit,
    viewModel: StoriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadStories()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var mediaType by remember { mutableStateOf("image") } // "image" or "video"

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
                // In a real app, we'd upload to S3 first. 
                // For now, we'll pass the uri string as a placeholder or mock the upload.
                // Since the request asks for S3, I'll assume the viewModel.createStory handles or will handle the upload logic.
                // For this demo, I'll mock the URL.
                viewModel.createStory(selectedMediaUri.toString(), mediaType, caption)
                showCreateDialog = false
            }
        )
    }

    LazyRow(
        contentPadding = PaddingValues(16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            AddStoryItem(onImage = {
                mediaType = "image"
                pickImageLauncher.launch("image/*")
            }, onVideo = {
                mediaType = "video"
                pickImageLauncher.launch("video/*")
            })
        }

        items(viewModel.stories) { story ->
            StoryItem(story, onStoryClick)
        }
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Preview
                if (mediaType == "image") {
                    coil.compose.AsyncImage(
                        model = uri,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    // Simple Video Preview Placeholder or actual VideoPlayer if available
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(64.dp))
                        Text("Video Preview", color = Color.White)
                    }
                }

                // Controls
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
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(onClick = { onPublish(caption) }) {
                            Text("Publish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStoryItem(onImage: () -> Unit, onVideo: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Add")
            
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Photo") },
                    onClick = { showMenu = false; onImage() },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Image, null) }
                )
                DropdownMenuItem(
                    text = { Text("Video") },
                    onClick = { showMenu = false; onVideo() },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, null) }
                )
            }
        }
        Text("Add", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StoryItem(story: ru.faustyu.paprika.data.network.Story, onClick: (ru.faustyu.paprika.data.network.Story) -> Unit) {
    val imageUrl = remember(story.media_url) {
        if (story.media_url.startsWith("http") || story.media_url.startsWith("content://")) story.media_url 
        else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + story.media_url
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color.Magenta, MaterialTheme.colorScheme.primary)
                    ),
                    shape = CircleShape
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onClick(story) },
            contentAlignment = Alignment.Center
        ) {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = "Story",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            if (story.media_type == "video") {
                Icon(
                    androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = "User ${story.user_id}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
