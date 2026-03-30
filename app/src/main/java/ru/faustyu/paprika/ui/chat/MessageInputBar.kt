package ru.faustyu.paprika.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ru.faustyu.paprika.util.VoiceRecorder

// ── Emoji categories ──────────────────────────────────────────────────────

private val EMOJI_SMILEYS = listOf(
    "😀","😁","😂","🤣","😃","😄","😅","😆","😉","😊",
    "😋","😎","😍","😘","🥰","😗","😙","😚","🙂","🤗",
    "🤩","🤔","🤨","😐","😑","😶","🙄","😏","😣","😥",
    "😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜",
    "😝","🤤","😒","😓","😔","😕","🙃","🤑","😲","☹️",
    "🙁","😖","😞","😟","😤","😢","😭","😦","😧","😨",
    "😩","🤯","😬","😰","😱","🥵","🥶","😳","🤪","😵",
    "🤠","🥳","😷","🤒","🤕","🤢","🤮","🤧","😇","🥸",
)

private val EMOJI_GESTURES = listOf(
    "👍","👎","👌","✌️","🤞","🤟","🤘","🤙","👈","👉",
    "👆","🖕","👇","☝️","👋","🤚","🖐","✋","🖖","👏",
    "🙌","🤲","🤝","🙏","✍️","💅","🤳","💪","🦵","🦶",
    "👀","👁","🫦","👄","💋","💯","❤️","🧡","💛","💚",
    "💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓",
    "💗","💖","💘","💝","💟","☮️","✝️","☯️","🔥","💥",
)

private val EMOJI_NATURE = listOf(
    "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯",
    "🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧",
    "🐦","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝",
    "🌵","🎄","🌲","🌳","🌴","🌾","☘️","🍀","🎍","🎋",
    "🍃","🍂","🍁","🍄","🌾","💐","🌷","🌹","🥀","🌺",
    "🌸","🌼","🌻","🌞","🌝","🌛","🌜","🌚","🌕","⭐",
)

private val EMOJI_FOOD = listOf(
    "🍕","🍔","🍟","🌭","🌮","🌯","🥗","🍜","🍝","🍛",
    "🍣","🍱","🍤","🍙","🍚","🍘","🍥","🥟","🦪","🍦",
    "🍧","🍨","🍰","🎂","🍮","🍭","🍬","🍫","🍿","🍩",
    "🎉","🎊","🎈","🎁","🎀","🏆","🥇","🏅","🎖️","🎗️",
)

private val EMOJI_OBJECTS = listOf(
    "📱","💻","⌨️","🖥","🖨","🖱","🖲","💾","💿","📀",
    "📷","📸","📹","🎥","📽","🎞","📞","☎️","📟","📠",
    "🔋","🔌","💡","🔦","🕯","🪔","🧲","💰","💳","💎",
    "⚖️","🔧","🔨","⚒","🛠","⛏","🔩","🔗","🧰","🔑",
)

private val ALL_EMOJI_TABS = listOf(
    "😊" to EMOJI_SMILEYS,
    "👍" to EMOJI_GESTURES,
    "🌿" to EMOJI_NATURE,
    "🍕" to EMOJI_FOOD,
    "📱" to EMOJI_OBJECTS,
)

private val STICKERS = listOf(
    "😀","😂","❤️","👍","🎉","😎","🤔","😢",
    "🔥","✨","💯","🙏","😍","😭","🤣","😊",
    "👀","💪","🎊","🌟","🥺","😤","🤦","🤷",
)

// ── Main composable ───────────────────────────────────────────────────────

@Composable
fun MessageInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onVideoCircle: () -> Unit,
    isRecording: Boolean,
    recordingDuration: Int,
    onVoiceCancel: () -> Unit,
    onVoiceSend: () -> Unit,
    onVoiceStart: () -> Unit,
    onStickerSend: (String) -> Unit,
    onGifSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showMediaPanel by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    val hasText = inputText.isNotBlank()

    // When user starts typing, collapse media panel
    LaunchedEffect(hasText) {
        if (hasText) showMediaPanel = false
    }

    Column(modifier = modifier) {
        if (isRecording) {
            RecordingBar(
                duration = recordingDuration,
                onCancel = onVoiceCancel,
                onSend = onVoiceSend,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // ── Input pill ────────────────────────────────────────────
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        // Emoji/sticker/gif toggle
                        IconButton(
                            onClick = { showMediaPanel = !showMediaPanel },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Crossfade(showMediaPanel, animationSpec = tween(150)) { open ->
                                Icon(
                                    if (open) Icons.Filled.Keyboard else Icons.Filled.EmojiEmotions,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (open)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Text field
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 6,
                            decorationBox = { inner ->
                                Box {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            "Сообщение",
                                            style = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 16.sp,
                                            ),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )

                        // Attach button
                        Box {
                            IconButton(
                                onClick = { showAttachMenu = true },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    Icons.Filled.AttachFile,
                                    contentDescription = "Прикрепить",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showAttachMenu,
                                onDismissRequest = { showAttachMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Фото / Видео") },
                                    leadingIcon = { Icon(Icons.Filled.Image, null) },
                                    onClick = { showAttachMenu = false; onPickImage() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Файл") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null) },
                                    onClick = { showAttachMenu = false; onPickFile() },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // ── Action FAB ────────────────────────────────────────────
                val fabColor by animateColorAsState(
                    if (hasText) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary,
                    animationSpec = tween(200),
                    label = "fabColor",
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(fabColor)
                        .then(
                            if (hasText) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSend() }
                            } else {
                                Modifier.pointerInput(isVideoMode) {
                                    detectTapGestures(
                                        onTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isVideoMode) {
                                                onVideoCircle()
                                            } else {
                                                // tap mic → switch to video circle mode
                                                isVideoMode = true
                                            }
                                        },
                                        onLongPress = {
                                            if (!isVideoMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPermission) {
                                                    VoiceRecorder.startRecording(context)
                                                    onVoiceStart()
                                                }
                                            } else {
                                                // long press video → switch back to mic
                                                isVideoMode = false
                                            }
                                        },
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(
                        targetState = when {
                            hasText -> "send"
                            isVideoMode -> "video"
                            else -> "mic"
                        },
                        animationSpec = tween(150),
                        label = "fabIcon",
                    ) { state ->
                        when (state) {
                            "send" -> Icon(
                                Icons.AutoMirrored.Filled.Send, null,
                                tint = Color.White, modifier = Modifier.size(22.dp),
                            )
                            "video" -> Icon(
                                Icons.Filled.Videocam, null,
                                tint = Color.White, modifier = Modifier.size(22.dp),
                            )
                            else -> Icon(
                                Icons.Filled.Mic, null,
                                tint = Color.White, modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Media panel (emoji / stickers / GIF) ─────────────────────────
        if (showMediaPanel && !isRecording) {
            MediaPanel(
                onEmojiClick = { emoji ->
                    onInputChange(inputText + emoji)
                },
                onStickerClick = { sticker ->
                    onStickerSend(sticker)
                    showMediaPanel = false
                },
                onGifClick = { gifUrl ->
                    onGifSend(gifUrl)
                    showMediaPanel = false
                },
            )
        }
    }
}

// ── Recording bar ─────────────────────────────────────────────────────────

@Composable
fun RecordingBar(
    duration: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Delete, contentDescription = "Отмена", tint = MaterialTheme.colorScheme.error)
        }
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            RecordingWaveform()
        }
        Text(
            text = "%02d:%02d".format(duration / 60, duration % 60),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RecordingWaveform() {
    val bars = 20
    val heights = remember { List(bars) { (4..20).random().dp } }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            )
        }
    }
}

// ── Media panel ───────────────────────────────────────────────────────────

@Composable
fun MediaPanel(
    onEmojiClick: (String) -> Unit,
    onStickerClick: (String) -> Unit,
    onGifClick: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.height(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("😊", fontSize = 18.sp)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("🎭", fontSize = 18.sp)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("GIF", style = MaterialTheme.typography.labelMedium)
                }
            }

            when (selectedTab) {
                0 -> EmojiTab(onEmojiClick)
                1 -> StickerTab(onStickerClick)
                2 -> GifTab(onGifClick)
            }
        }
    }
}

// ── Emoji tab ─────────────────────────────────────────────────────────────

@Composable
private fun EmojiTab(onEmojiClick: (String) -> Unit) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    Column {
        // Category tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            modifier = Modifier.height(36.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 8.dp,
            indicator = {},
            divider = {},
        ) {
            ALL_EMOJI_TABS.forEachIndexed { index, (icon, _) ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(
                        icon, fontSize = 18.sp,
                        color = if (selectedCategory == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val emojis = ALL_EMOJI_TABS[selectedCategory].second
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

// ── Sticker tab ───────────────────────────────────────────────────────────

@Composable
private fun StickerTab(onStickerClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(STICKERS) { sticker ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStickerClick(sticker) },
                contentAlignment = Alignment.Center,
            ) {
                Text(sticker, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

// ── GIF tab ───────────────────────────────────────────────────────────────

@Composable
private fun GifTab(onGifClick: (String) -> Unit) {
    // Reuse existing GifPickerSheet content inline
    GifPickerInline(onGifSelected = onGifClick)
}
