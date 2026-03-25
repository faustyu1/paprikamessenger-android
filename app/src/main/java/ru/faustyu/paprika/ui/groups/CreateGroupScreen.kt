package ru.faustyu.paprika.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.CreateChatRequest
import ru.faustyu.paprika.data.network.NetworkModule
import android.net.Uri

class CreateGroupViewModel : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var isChannel by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    fun createChat(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Type 1 = Group, 2 = Channel
                val type = if (isChannel) 2 else 1
                val request = CreateChatRequest(type, title, description)
                val response = NetworkModule.api.createChat(request)
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle Error
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    isChannel: Boolean = false,
    viewModel: CreateGroupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(isChannel) {
        viewModel.isChannel = isChannel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isChannel) "New Channel" else "New Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.title.isNotBlank()) {
                FloatingActionButton(
                    onClick = { viewModel.createChat(onSuccess) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Create")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(bottom = 32.dp)
        ) {
            // Main Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Photo Placeholder
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable { /* TODO: Pick Photo */ }
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Icon(
                                Icons.Default.PhotoCamera, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title Input
                    androidx.compose.foundation.text.BasicTextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.title = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (viewModel.title.isEmpty()) {
                                Text(
                                    text = if (isChannel) "Enter channel name" else "Enter group name",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Options List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO */ }
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Auto-Delete Messages",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Off",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Text(
                text = if (isChannel) "Automatically delete messages in this channel for everyone after a period of time." else "Automatically delete messages in this group for everyone after a period of time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}
