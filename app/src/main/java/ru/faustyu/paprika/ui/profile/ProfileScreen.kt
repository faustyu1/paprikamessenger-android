package ru.faustyu.paprika.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.Calendar

class ProfileViewModel : ViewModel() {
    var username by mutableStateOf("")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var bio by mutableStateOf("")
    var birthday by mutableStateOf("") // "YYYY-MM-DD" or ""
    var avatarUrl by mutableStateOf<String?>(null)
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
                        birthday = user.birthday ?: ""
                        if (!user.avatar.isNullOrBlank()) {
                            val baseUrl = ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/")
                            avatarUrl = if (user.avatar.startsWith("http")) user.avatar
                                        else "$baseUrl${user.avatar}"
                        } else {
                            avatarUrl = null
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

    fun saveProfile(newUsername: String, newFirstName: String, newLastName: String, newBio: String, newBirthday: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = ru.faustyu.paprika.data.network.UpdateProfileRequest(
                    username = newUsername,
                    first_name = newFirstName,
                    last_name = newLastName,
                    bio = newBio,
                    birthday = newBirthday.ifBlank { null }
                )
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.updateProfile(request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        username = it.username
                        firstName = it.first_name ?: ""
                        lastName = it.last_name ?: ""
                        bio = it.bio ?: ""
                        birthday = it.birthday ?: ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

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

/** Format "YYYY-MM-DD" → "12 May" or "12 May 1998" */
fun formatBirthday(raw: String, showYear: Boolean = true): String {
    if (raw.isBlank()) return ""
    return try {
        val parts = raw.split("-")
        if (parts.size < 3) return raw
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val monthName = monthNames.getOrElse(month - 1) { month.toString() }
        if (showYear) "$day $monthName $year" else "$day $monthName"
    } catch (e: Exception) { raw }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current

    var tempUsername by remember(viewModel.username) { mutableStateOf(viewModel.username) }
    var tempFirstName by remember(viewModel.firstName) { mutableStateOf(viewModel.firstName) }
    var tempLastName by remember(viewModel.lastName) { mutableStateOf(viewModel.lastName) }
    var tempBio by remember(viewModel.bio) { mutableStateOf(viewModel.bio) }
    var tempBirthday by remember(viewModel.birthday) { mutableStateOf(viewModel.birthday) }
    var showBirthdayPicker by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }

    // Android DatePickerDialog via effect
    if (showBirthdayPicker) {
        val cal = Calendar.getInstance()
        if (tempBirthday.isNotBlank()) {
            runCatching {
                val parts = tempBirthday.split("-")
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        }
        val datePicker = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                tempBirthday = "%04d-%02d-%02d".format(year, month + 1, day)
                showBirthdayPicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setOnDismissListener { showBirthdayPicker = false }
        // Max date = today (can't be born in future)
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        DisposableEffect(Unit) {
            datePicker.show()
            onDispose { datePicker.dismiss() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.isLoading) {
                        TextButton(onClick = {
                            viewModel.saveProfile(tempUsername, tempFirstName, tempLastName, tempBio, tempBirthday)
                        }) {
                            Text("Done", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(100.dp).clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!viewModel.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = viewModel.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = (tempFirstName.takeIf { it.isNotBlank() } ?: tempUsername).take(1).uppercase()
                                if (initial.isNotEmpty()) {
                                    Text(text = initial, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape).padding(6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // Your Info (username + birthday)
            ProfileSectionHeader(text = "Your Info")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    AccountInfoRow(
                        icon = Icons.Default.AlternateEmail,
                        title = "@$tempUsername",
                        subtitle = "Username"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    // Birthday row — clickable to open picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBirthdayPicker = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (tempBirthday.isNotBlank()) formatBirthday(tempBirthday) else "Set Birthday",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tempBirthday.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(text = "Birthday", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (tempBirthday.isNotBlank()) {
                            IconButton(onClick = { tempBirthday = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
            Text(
                text = "Choose who can see your birthday in Privacy & Security settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Your name
            ProfileSectionHeader(text = "Your name")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BasicEditField(value = tempFirstName, onValueChange = { tempFirstName = it }, placeholder = "First Name")
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicEditField(value = tempLastName, onValueChange = { tempLastName = it }, placeholder = "Last Name (Optional)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Your bio
            ProfileSectionHeader(text = "Your bio")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BasicEditField(value = tempBio, onValueChange = { tempBio = it }, placeholder = "A few lines about yourself...", singleLine = false)
                }
            }
            Text(
                text = "You can add a few lines about yourself. Choose who can see your bio in Privacy & Security settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out")
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    )
}

@Composable
fun AccountInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BasicEditField(value: String, onValueChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = singleLine,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            innerTextField()
        }
    )
}
