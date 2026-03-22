package ru.faustyu.paprika.ui.chat

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoCircleScreen(
    onSendVideo: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Bind CameraX
    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    vc
                )
            } catch (_: Exception) {
                // Fall back to back camera
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        vc
                    )
                } catch (_: Exception) {}
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Duration timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            durationSeconds = 0
            while (isRecording) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    // Auto-stop at 60 seconds
    LaunchedEffect(durationSeconds) {
        if (durationSeconds >= 60 && isRecording) {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Circular camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
        )

        // Recording progress ring
        if (isRecording) {
            CircularProgressIndicator(
                progress = { durationSeconds / 60f },
                modifier = Modifier.size(300.dp),
                color = Color.Red,
                strokeWidth = 6.dp,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }

        // Duration label
        if (isRecording) {
            Text(
                text = "● %02d:%02d".format(durationSeconds / 60, durationSeconds % 60),
                color = Color.Red,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel (only when not recording)
            if (!isRecording) {
                FloatingActionButton(
                    onClick = onBack,
                    containerColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Record / Stop button
            FloatingActionButton(
                onClick = {
                    val vc = videoCapture ?: return@FloatingActionButton
                    if (!isRecording) {
                        val outputFile = File(context.cacheDir, "circle_${System.currentTimeMillis()}.mp4")
                        val outputOptions = FileOutputOptions.Builder(outputFile).build()
                        activeRecording = vc.output
                            .prepareRecording(context, outputOptions)
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        if (!event.hasError()) {
                                            onSendVideo(outputFile.absolutePath)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        isRecording = true
                    } else {
                        activeRecording?.stop()
                        activeRecording = null
                    }
                },
                containerColor = if (isRecording) Color(0xFFCC0000) else Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    modifier = if (isRecording)
                        Modifier
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    else
                        Modifier
                            .size(36.dp)
                            .background(Color.Red, CircleShape)
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}
