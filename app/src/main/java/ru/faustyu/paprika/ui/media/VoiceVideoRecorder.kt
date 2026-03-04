package ru.faustyu.paprika.ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderBottomSheet(
    onDismiss: () -> Unit,
    onVoiceSent: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Voice Message",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isRecording) {
                Text(
                    "Recording: ${recordingDuration}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = {
                        isRecording = false
                        recordingDuration = 0
                    }) {
                        Icon(Icons.Default.Close, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    Button(onClick = {
                        // Stop recording and send
                        isRecording = false
                        onVoiceSent("voice_message_url.ogg")
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Send, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send")
                    }
                }
            } else {
                FilledIconButton(
                    onClick = {
                        isRecording = true
                        // TODO: Start audio recording
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Tap to record",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCircleRecorderBottomSheet(
    onDismiss: () -> Unit,
    onVideoSent: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Video Message (Circle)",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (!isRecording) {
                FilledIconButton(
                    onClick = {
                        isRecording = true
                        // TODO: Start video recording
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.Videocam, null, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Tap to record video circle",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Recording...", color = MaterialTheme.colorScheme.error)

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    isRecording = false
                    onVideoSent("video_circle_url.mp4")
                    onDismiss()
                }) {
                    Text("Stop & Send")
                }
            }
        }
    }
}
