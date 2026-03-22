package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.MessageDto
import ru.faustyu.paprika.data.network.NetworkModule

class MediaGalleryViewModel : ViewModel() {
    var mediaMessages by mutableStateOf<List<MessageDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun load(chatId: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val resp = NetworkModule.api.getChatMessages(chatId)
                if (resp.isSuccessful) {
                    mediaMessages = (resp.body() ?: emptyList())
                        .filter { it.type in listOf("image", "video", "video_circle") }
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: MediaGalleryViewModel = viewModel()
) {
    LaunchedEffect(chatId) { viewModel.load(chatId) }

    var selectedUrl by remember { mutableStateOf<String?>(null) }

    if (selectedUrl != null) {
        AlertDialog(
            onDismissRequest = { selectedUrl = null },
            text = {
                AsyncImage(
                    model = selectedUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedUrl = null }) { Text("Закрыть") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Медиафайлы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.mediaMessages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет медиафайлов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.mediaMessages) { msg ->
                    val url = if (msg.content.startsWith("http")) msg.content
                              else NetworkModule.baseUrl.removeSuffix("/") + msg.content
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .clickable { selectedUrl = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (msg.type == "video" || msg.type == "video_circle") {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
