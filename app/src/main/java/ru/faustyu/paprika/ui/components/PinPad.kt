package ru.faustyu.paprika.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinPad(
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    currentPin: String,
    onPinChange: (String) -> Unit,
    showBiometric: Boolean = false,
    onBiometricClick: () -> Unit = {},
    title: String = "Введите PIN",
    isError: Boolean = false,
    errorMessage: String = "Неверный PIN"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isError -> MaterialTheme.colorScheme.error
                                isFilled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }

        if (isError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(36.dp)) // Maintain height to avoid jumping
        }

        // Numpad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (showBiometric) "BIO" else "", "0", "DEL")
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        NumpadKey(
                            key = key,
                            onClick = {
                                when (key) {
                                    "BIO" -> {
                                        onBiometricClick()
                                    }
                                    "DEL" -> {
                                        if (currentPin.isNotEmpty()) {
                                            onPinChange(currentPin.dropLast(1))
                                        }
                                    }
                                    "" -> { }
                                    else -> {
                                        if (currentPin.length < pinLength) {
                                            onPinChange(currentPin + key)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    key: String,
    onClick: () -> Unit
) {
    if (key.isEmpty()) {
        Spacer(modifier = Modifier.size(72.dp))
        return
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "DEL" -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Удалить",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            "BIO" -> Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = "Биометрия",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            else -> Text(
                text = key,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
