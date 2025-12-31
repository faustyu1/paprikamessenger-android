package ru.faustyu.paprika.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.CreateChatRequest
import ru.faustyu.paprika.data.network.NetworkModule

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
    viewModel: CreateGroupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Group / Channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Icon would be ArrowBack
                        Text("<") 
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.isChannel,
                    onCheckedChange = { viewModel.isChannel = it }
                )
                Text("Is Channel?")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.createChat(onSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && viewModel.title.isNotBlank()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Create")
                }
            }
        }
    }
}
