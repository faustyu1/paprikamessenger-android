package ru.faustyu.paprika.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ProfileViewModel : ViewModel() {
    var username by mutableStateOf("")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var bio by mutableStateOf("")
    var avatarUrl by mutableStateOf<String?>(null) // Remote URL
    
    // For local preview before upload, if we want immediate feedback, but simpler to upload on save or separate.
    // Let's keep it simple: Select -> URI, Save -> Upload Image -> Update Profile.
    
    var isLoading by mutableStateOf(false)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.getMyProfile()
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        username = user.username
                        firstName = user.first_name ?: ""
                        lastName = user.last_name ?: ""
                        bio = user.bio ?: ""
                        
                        // Construct full URL if needed
                        if (user.avatar != null) {
                            val baseUrl = ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/")
                             // If it starts with http, use it, else prepend base
                             if (user.avatar.startsWith("http")) {
                                 avatarUrl = user.avatar
                             } else {
                                 avatarUrl = "$baseUrl${user.avatar}"
                             }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun saveProfile(newUsername: String, newFirstName: String, newLastName: String, newBio: String, newAvatarUri: Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                // 1. Upload Avatar if Changed (not null)
                if (newAvatarUri != null) {
                   // Need ContentResolver to get bytes or file
                   // For brevity using a helper or assuming context access via separate util or passed in.
                   // Since ViewModel doesn't have easy context, we might require the UI to pass a MultipartBody.Part or file.
                   // Let's do a workaround: Use a repo or util that has context, OR just do it cleanly.
                   // Ideally: saveProfile(..., avatarPart: MultipartBody.Part?)
                }
                
                // Since we can't easily get File from Uri inside ViewModel without context,
                // we will skip the "how to get file" implementation detail here and assume we update text fields first.
                // *To fix the user request properly regarding avatar:*
                // The user says "when I upload avatar... it doesn't update". 
                // We'll focus on text fields first and handle avatar if provided (requires context).
                
                val request = ru.faustyu.paprika.data.network.UpdateProfileRequest(
                    username = newUsername,
                    first_name = newFirstName,
                    last_name = newLastName,
                    bio = newBio
                )
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.updateProfile(request)
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        username = it.username
                        firstName = it.first_name ?: ""
                        lastName = it.last_name ?: ""
                        bio = it.bio ?: ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Quick helper for avatar upload using Context from Composable
    fun uploadAvatar(context: android.content.Context, uri: Uri) {
         viewModelScope.launch {
             isLoading = true
             try {
                 val inputStream = context.contentResolver.openInputStream(uri)
                 val bytes = inputStream?.readBytes()
                 inputStream?.close()
                 
                 if (bytes != null) {
                     val mediaType = "image/*".toMediaTypeOrNull()
                     val requestFile = okhttp3.RequestBody.create(mediaType, bytes)
                     val body = okhttp3.MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestFile)
                     
                     val response = ru.faustyu.paprika.data.network.NetworkModule.api.uploadAvatar(body)
                     if (response.isSuccessful) {
                         val user = response.body()
                         // Force URL refreshing by appending timestamp
                         if (user?.avatar != null) {
                             val baseUrl = ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/")
                             val rawUrl = if (user.avatar.startsWith("http")) user.avatar else "$baseUrl${user.avatar}"
                             avatarUrl = "$rawUrl?t=${System.currentTimeMillis()}"
                         }
                     }
                 }
             } catch (e: Exception) {
                e.printStackTrace()
             } finally {
                 isLoading = false
             }
         }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Local state for editing
    var tempUsername by remember(viewModel.username) { mutableStateOf(viewModel.username) }
    var tempFirstName by remember(viewModel.firstName) { mutableStateOf(viewModel.firstName) }
    var tempLastName by remember(viewModel.lastName) { mutableStateOf(viewModel.lastName) }
    var tempBio by remember(viewModel.bio) { mutableStateOf(viewModel.bio) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
             // Upload immediately for better UX
             viewModel.uploadAvatar(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.avatarUrl != null) {
                    AsyncImage(
                        model = viewModel.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tempUsername.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Edit overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Avatar",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = tempUsername,
                onValueChange = { tempUsername = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = tempFirstName,
                onValueChange = { tempFirstName = it },
                label = { Text("First Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = tempLastName,
                onValueChange = { tempLastName = it },
                label = { Text("Last Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = tempBio,
                onValueChange = { tempBio = it },
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    viewModel.saveProfile(tempUsername, tempFirstName, tempLastName, tempBio, null) 
                },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Changes")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out")
            }
        }
    }
}
