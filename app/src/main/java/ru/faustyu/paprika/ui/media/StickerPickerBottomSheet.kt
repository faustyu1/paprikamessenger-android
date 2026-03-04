package ru.faustyu.paprika.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class StickerPack(
    val id: Long,
    val name: String,
    val stickers: List<String> // URLs
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerBottomSheet(
    onDismiss: () -> Unit,
    onStickerSelected: (String) -> Unit
) {
    // Mock sticker packs - in production load from backend
    val stickerPacks = remember {
        listOf(
            StickerPack(1, "Popular", List(12) { "https://placeholder-sticker-$it.png" }),
            StickerPack(2, "Animals", List(12) { "https://placeholder-animal-$it.png" }),
            StickerPack(3, "Emoji", List(12) { "https://placeholder-emoji-$it.png" })
        )
    }

    var selectedPack by remember { mutableStateOf(stickerPacks[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(16.dp)
        ) {
            // Pack tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stickerPacks.forEach { pack ->
                    FilterChip(
                        selected = selectedPack.id == pack.id,
                        onClick = { selectedPack = pack },
                        label = { Text(pack.name) }
                    )
                }
            }

            // Stickers grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedPack.stickers) { stickerUrl ->
                    AsyncImage(
                        model = stickerUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                onStickerSelected(stickerUrl)
                                onDismiss()
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
