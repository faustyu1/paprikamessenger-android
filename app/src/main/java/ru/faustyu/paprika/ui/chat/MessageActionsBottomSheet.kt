package ru.faustyu.paprika.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsBottomSheet(
    messageId: Long,
    isOwnMessage: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onForward: () -> Unit,
    onSelect: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            MessageActionItem(
                icon = Icons.Default.Reply,
                text = "Reply",
                onClick = {
                    onReply()
                    onDismiss()
                }
            )

            if (isOwnMessage) {
                MessageActionItem(
                    icon = Icons.Default.Edit,
                    text = "Edit",
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
            }

            MessageActionItem(
                icon = Icons.Default.Forward,
                text = "Forward",
                onClick = {
                    onForward()
                    onDismiss()
                }
            )

            MessageActionItem(
                icon = Icons.Default.CheckCircle,
                text = "Select",
                onClick = {
                    onSelect()
                    onDismiss()
                }
            )

            if (isOwnMessage) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                MessageActionItem(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = textColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}
