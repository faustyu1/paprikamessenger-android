package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.ImageLoader
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class GifItem(val id: String, val previewUrl: String, val originalUrl: String)

suspend fun searchGiphy(query: String): List<GifItem> = withContext(Dispatchers.IO) {
    try {
        val apiKey = "dc6zaTOxFJmzC"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.giphy.com/v1/gifs/search?api_key=$apiKey&q=$encodedQuery&limit=20&rating=g"
        val json = URL(url).readText()
        val arr = JSONObject(json).getJSONArray("data")
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val images = item.getJSONObject("images")
            val preview = images.getJSONObject("fixed_height_small").getString("url")
            val original = images.getJSONObject("original").getString("url")
            GifItem(item.getString("id"), preview, original)
        }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerSheet(
    onGifSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }
    var query by remember { mutableStateOf("funny") }
    var gifs by remember { mutableStateOf<List<GifItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        isLoading = true
        gifs = searchGiphy("trending")
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск GIF...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    isLoading = true
                })
            )

            LaunchedEffect(isLoading) {
                if (isLoading && query.isNotBlank()) {
                    gifs = searchGiphy(query)
                    isLoading = false
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(gifs) { gif ->
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(gif.previewUrl)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onGifSelected(gif.originalUrl) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
