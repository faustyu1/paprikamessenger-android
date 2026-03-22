package ru.faustyu.paprika.ui.chat

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VideoCircleRoute(
    chatId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    VideoCircleScreen(
        onSendVideo = { filePath ->
            viewModel.sendVideoCircle(chatId, filePath)
            onBack()
        },
        onBack = onBack
    )
}
