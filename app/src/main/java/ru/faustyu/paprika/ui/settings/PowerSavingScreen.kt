package ru.faustyu.paprika.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.faustyu.paprika.data.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerSavingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    // Slider encodes mode + threshold in one value:
    //   0f          → Off
    //   1f .. 99f   → Auto at that battery %
    //   100f        → Always On
    val initSlider = when (prefs.powerSavingMode) {
        0 -> 0f
        2 -> 100f
        else -> prefs.powerSavingThreshold.toFloat().coerceIn(1f, 99f)
    }
    var sliderValue by remember { mutableFloatStateOf(initSlider) }

    // Individual option toggles
    var animStickers   by remember { mutableStateOf(prefs.psSaveAnimatedStickers) }
    var animEmoji      by remember { mutableStateOf(prefs.psSaveAnimatedEmoji) }
    var effectsChats   by remember { mutableStateOf(prefs.psSaveEffectsChats) }
    var animCalls      by remember { mutableStateOf(prefs.psSaveAnimationsInCalls) }
    var autoVideos     by remember { mutableStateOf(prefs.psSaveAutoplayVideos) }
    var autoGifs       by remember { mutableStateOf(prefs.psSaveAutoplayGifs) }
    var particles      by remember { mutableStateOf(prefs.psSaveParticles) }
    var smoothTrans    by remember { mutableStateOf(prefs.smoothTransitions) }

    // Derive label from slider
    val modeLabel = when {
        sliderValue <= 0f  -> "OFF"
        sliderValue >= 100f -> "ON"
        else -> "AUTO"
    }
    val descLabel = when {
        sliderValue <= 0f   -> "Power saving is disabled."
        sliderValue >= 100f -> "Power saving is always active."
        else -> "Automatically reduce power usage and animations when your battery is below ${sliderValue.toInt()}%."
    }

    fun saveSlider(v: Float) {
        sliderValue = v
        when {
            v <= 0f   -> { prefs.powerSavingMode = 0 }
            v >= 100f -> { prefs.powerSavingMode = 2 }
            else      -> { prefs.powerSavingMode = 1; prefs.powerSavingThreshold = v.toInt() }
        }
        prefs.computePowerSaving(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Power Saving") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Mode card ────────────────────────────────────────────────────
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // Title + badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Power Saving Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    modeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Labels row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Off", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            if (sliderValue > 0f && sliderValue < 100f) {
                                Text(
                                    "When below ${sliderValue.toInt()}% 🔋",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.weight(1f))
                            }
                            Text("On", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { saveSlider(it) },
                            valueRange = 0f..100f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text(
                    descLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Options ──────────────────────────────────────────────────────
            item {
                Text(
                    "Power saving options",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PsToggleItem(
                        icon = Icons.Default.EmojiEmotions,
                        title = "Animated Stickers",
                        checked = animStickers,
                        onCheckedChange = { animStickers = it; prefs.psSaveAnimatedStickers = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.SentimentSatisfied,
                        title = "Animated Emoji",
                        checked = animEmoji,
                        onCheckedChange = { animEmoji = it; prefs.psSaveAnimatedEmoji = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.Chat,
                        title = "Effects in Chats",
                        checked = effectsChats,
                        onCheckedChange = { effectsChats = it; prefs.psSaveEffectsChats = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.Call,
                        title = "Animations in Calls",
                        checked = animCalls,
                        onCheckedChange = { animCalls = it; prefs.psSaveAnimationsInCalls = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.VideoCall,
                        title = "Autoplay Videos",
                        checked = autoVideos,
                        onCheckedChange = { autoVideos = it; prefs.psSaveAutoplayVideos = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.Gif,
                        title = "Autoplay GIFs",
                        checked = autoGifs,
                        onCheckedChange = { autoGifs = it; prefs.psSaveAutoplayGifs = it }
                    )
                    SettingsDivider()
                    PsToggleItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Particles",
                        checked = particles,
                        onCheckedChange = { particles = it; prefs.psSaveParticles = it }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Smooth transitions ────────────────────────────────────────────
            item {
                SettingsSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = {
                            Text("Enable Smooth Transitions", fontWeight = FontWeight.Medium)
                        },
                        trailingContent = {
                            Switch(
                                checked = smoothTrans,
                                onCheckedChange = {
                                    smoothTrans = it
                                    prefs.smoothTransitions = it
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
                Text(
                    "You can disable animated transitions between different sections of the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}
