package ru.faustyu.paprika.ui.encryption

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * Encryption fingerprint verification screen similar to Telegram
 * Shows 5 emoji based on the combined hash of both users' public keys
 */
@Composable
fun EncryptionVerificationScreen(
    myPublicKey: String,
    otherUserPublicKey: String,
    otherUsername: String,
    onDismiss: () -> Unit
) {
    val emojis = remember(myPublicKey, otherUserPublicKey) {
        generateVerificationEmojis(myPublicKey, otherUserPublicKey)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Encryption Key Verification",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Compare these emoji with $otherUsername",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Display 5 emoji in a row
            Row(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                emojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "If the emoji match, your conversation is secured with end-to-end encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

/**
 * Generate 5 emoji based on the fingerprint of two public keys
 * Similar to Telegram's verification mechanism
 */
private fun generateVerificationEmojis(key1: String, key2: String): List<String> {
    // Combine and hash both keys
    val combined = if (key1 < key2) key1 + key2 else key2 + key1
    val hash = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())

    // Emoji set (same as Telegram uses for verification)
    val emojiSet = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
        "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺",
        "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞",
        "🐜", "🦗", "🕷", "🦂", "🐢", "🐍", "🦎", "🦖",
        "🦕", "🐙", "🦑", "🦐", "🦀", "🐡", "🐠", "🐟",
        "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓",
        "🦍", "🐘", "🦏", "🦛", "🐪", "🐫", "🦒", "🐃"
    )

    // Convert hash to indices and select 5 emoji
    return (0 until 5).map { i ->
        val index = hash[i * 2].toInt().and(0xFF)
        emojiSet[index % emojiSet.size]
    }
}
