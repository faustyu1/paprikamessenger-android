package ru.faustyu.paprika.ui.bot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotChatScreen(
    botName: String,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf(
            BotMessage(
                "🤖 Hello! I'm $botName",
                isBot = true
            )
        ))
    }

    // Bot commands
    val commands = listOf(
        "/start" to "Start the bot",
        "/help" to "Show help",
        "/settings" to "Bot settings"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(botName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Command buttons (shown above input)
                if (messages.size <= 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commands.forEach { (command, _) ->
                            AssistChip(
                                onClick = {
                                    inputText = command
                                    messages = messages + BotMessage(command, isBot = false)
                                    // Simulate bot response
                                    messages = messages + BotMessage(
                                        "Processing command: $command",
                                        isBot = true
                                    )
                                },
                                label = { Text(command) }
                            )
                        }
                    }
                }

                // Input field
                Surface(
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message $botName...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    messages = messages + BotMessage(inputText, isBot = false)
                                    inputText = ""
                                    // Simulate bot response
                                    messages = messages + BotMessage(
                                        "I received: ${messages.last().text}",
                                        isBot = true
                                    )
                                }
                            }
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                BotMessageBubble(message)
            }
        }
    }
}

data class BotMessage(
    val text: String,
    val isBot: Boolean
)

@Composable
fun BotMessageBubble(message: BotMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isBot) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isBot) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isBot) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}
