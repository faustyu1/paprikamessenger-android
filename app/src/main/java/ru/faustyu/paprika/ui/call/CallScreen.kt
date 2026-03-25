package ru.faustyu.paprika.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.faustyu.paprika.R

@Composable
fun CallScreen(
    peerName: String,
    callType: String,
    isOutgoing: Boolean,
    isMuted: Boolean,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit
) {
    var durationSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(!isOutgoing) {
        if (!isOutgoing) {
            while (true) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        peerName.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = peerName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isOutgoing) stringResource(R.string.call_calling) else formatDuration(durationSeconds),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onToggleMute,
                        containerColor = if (isMuted)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isMuted) stringResource(R.string.call_unmute) else stringResource(R.string.call_mute),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (isMuted) stringResource(R.string.call_unmute) else stringResource(R.string.call_mute), style = MaterialTheme.typography.labelSmall)
                }

                // End call button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = Color(0xFFE53935),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Filled.CallEnd,
                            contentDescription = stringResource(R.string.call_end),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.call_end), style = MaterialTheme.typography.labelLarge)
                }

                // Speaker placeholder (no-op for now)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.call_speaker),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.call_speaker), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
