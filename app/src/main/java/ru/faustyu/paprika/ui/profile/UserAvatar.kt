package ru.faustyu.paprika.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * User avatar with fallback placeholder
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    username: String,
    size: Dp = 48.dp,
    isBot: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        val fullUrl = if (avatarUrl.startsWith("http")) {
            avatarUrl
        } else {
            ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + avatarUrl
        }

        AsyncImage(
            model = fullUrl,
            contentDescription = username,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder with first letter
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isBot) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isBot) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
