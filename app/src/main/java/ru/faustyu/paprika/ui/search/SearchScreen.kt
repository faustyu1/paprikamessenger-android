package ru.faustyu.paprika.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.db.DatabaseModule
import ru.faustyu.paprika.data.db.SearchHistoryEntity
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.UserPublic

class SearchViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val db = DatabaseModule.provideDatabase(application)
    private val historyDao = db.searchHistoryDao()

    var query by mutableStateOf("")
    var users by mutableStateOf<List<UserPublic>>(emptyList())
    var isLoading by mutableStateOf(false)
    
    private val _history = mutableStateListOf<SearchHistoryEntity>()
    val history: List<SearchHistoryEntity> = _history

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            historyDao.getHistory().collect {
                _history.clear()
                _history.addAll(it)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel()
        if (newQuery.length > 2) {
            searchJob = viewModelScope.launch {
                delay(500) // Debounce
                performSearch(newQuery)
            }
        } else {
            users = emptyList()
        }
    }

    private suspend fun performSearch(q: String) {
        isLoading = true
        try {
            val response = NetworkModule.api.searchUsers(q)
            if (response.isSuccessful) {
                users = response.body() ?: emptyList()
                if (users.isNotEmpty()) {
                    saveToHistory(q)
                }
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    private fun saveToHistory(q: String) {
        viewModelScope.launch {
            historyDao.insert(SearchHistoryEntity(query = q, timestamp = System.currentTimeMillis()))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clear()
        }
    }

    fun startChat(user: UserPublic, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Type 0 = Private
                val request = ru.faustyu.paprika.data.network.CreateChatRequest(
                    type = 0,
                    title = user.username,
                    description = "",
                    recipient_id = user.id
                )
                val response = NetworkModule.api.createChat(request)
                if (response.isSuccessful) {
                    val chat = response.body()
                    chat?.let {
                        onChatReady(it.id.toString())
                    }
                }
            } catch (e: Exception) {
                // error
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onChatJoined: (String) -> Unit,
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = viewModel.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search users...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.onQueryChange(viewModel.query) // Force search or just hide keyboard
                                // default behavior logic
                            }
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (viewModel.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            LazyColumn {
                if (viewModel.query.isEmpty() && viewModel.history.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Clear", 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { viewModel.clearHistory() }
                            )
                        }
                    }
                    items(viewModel.history) { item ->
                        ListItem(
                            headlineContent = { Text(item.query) },
                            leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.onQueryChange(item.query) }
                        )
                    }
                }

                items(viewModel.users) { user ->
                    val displayName = if (!user.first_name.isNullOrBlank()) {
                        "${user.first_name} ${user.last_name ?: ""}".trim()
                    } else {
                        user.username
                    }
                    
                    ListItem(
                        headlineContent = { Text(displayName) },
                        supportingContent = { 
                            Column {
                                Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                                if (!user.bio.isNullOrBlank()) {
                                    Text(user.bio)
                                }
                            }
                        },
                        leadingContent = {
                            val avatarUrl = user.avatar?.let { av ->
                                if (av.startsWith("http")) av else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + av
                            }
                            
                            if (avatarUrl != null) {
                                coil.compose.AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initial = (user.first_name?.takeIf { it.isNotBlank() } ?: user.username).take(1).uppercase()
                                        Text(initial)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable { 
                            if (!viewModel.isLoading) {
                                viewModel.startChat(user, onChatJoined) 
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
