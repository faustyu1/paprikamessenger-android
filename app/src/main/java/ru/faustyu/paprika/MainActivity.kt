package ru.faustyu.paprika

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.network.AppWebSocketManager
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.ui.auth.AuthScreen
import ru.faustyu.paprika.ui.call.*
import ru.faustyu.paprika.ui.chat.VideoCircleRoute
import ru.faustyu.paprika.ui.chat.VideoCircleScreen
import ru.faustyu.paprika.ui.groups.GroupInfoScreen
import ru.faustyu.paprika.ui.settings.SessionsScreen
import ru.faustyu.paprika.ui.settings.SettingsScreen
import ru.faustyu.paprika.ui.settings.ChatSettingsScreen
import ru.faustyu.paprika.ui.settings.NotificationsSettingsScreen
import ru.faustyu.paprika.ui.settings.PrivacySettingsScreen
import ru.faustyu.paprika.ui.chat.MediaGalleryScreen
import ru.faustyu.paprika.ui.theme.PaprikaTheme
import ru.faustyu.paprika.util.AppNotificationHelper
import ru.faustyu.paprika.util.GitHubRelease
import ru.faustyu.paprika.util.UpdateChecker
import java.security.MessageDigest

class MainActivity : androidx.appcompat.app.AppCompatActivity() {

    private var downloadId = -1L

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != downloadId) return
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val apkUri = dm.getUriForDownloadedFile(id) ?: return
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, call will proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val prefs = PrefsManager(this)
        prefs.backendUrl?.let { NetworkModule.setCustomUrl(it) }
        AppNotificationHelper.init(this)

        val startDestination = if (prefs.token != null) {
            NetworkModule.authToken = prefs.token
            AppWebSocketManager.connect(prefs.token!!, NetworkModule.getCurrentUrl())
            AppWebSocketManager.onNewMessage = { senderId ->
                if (prefs.notificationSound && senderId != prefs.myUserId) {
                    AppNotificationHelper.playMessageSound(this, true)
                }
            }
            "chat_list"
        } else {
            "auth"
        }

        setContent {
            val themeMode by PrefsManager.themeModeFlow.collectAsState()
            val fontScale by PrefsManager.fontSizeFlow.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }
            PaprikaTheme(darkTheme = darkTheme, fontScale = fontScale) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val callViewModel: CallViewModel = viewModel()
                    val callState by callViewModel.callState.collectAsState()
                    val isMuted by callViewModel.isMuted.collectAsState()

                    // App lock
                    var isUnlocked by remember { mutableStateOf(!prefs.appLockEnabled) }
                    var pinInput by remember { mutableStateOf("") }
                    var pinError by remember { mutableStateOf(false) }
                    val launchBiometric = {
                        val biometricManager = BiometricManager.from(this@MainActivity)
                        val canUseBiometric = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                        ) == BiometricManager.BIOMETRIC_SUCCESS
                        if (canUseBiometric) {
                            val executor = ContextCompat.getMainExecutor(this@MainActivity)
                            val prompt = BiometricPrompt(
                                this@MainActivity, executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        isUnlocked = true
                                    }
                                }
                            )
                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Разблокировать Paprika")
                                    .setSubtitle("Используйте биометрию для входа")
                                    .setNegativeButtonText("PIN")
                                    .build()
                            )
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (prefs.appLockEnabled) {
                            launchBiometric()
                        }
                    }

                    if (!isUnlocked) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                ru.faustyu.paprika.ui.components.PinPad(
                                    currentPin = pinInput,
                                    onPinChange = { it ->
                                        pinInput = it
                                        pinError = false
                                        if (it.length == 4) {
                                            val hash = MessageDigest.getInstance("SHA-256")
                                                .digest(it.toByteArray())
                                                .joinToString("") { "%02x".format(it) }
                                            if (hash == prefs.pinHash) {
                                                isUnlocked = true
                                            } else {
                                                pinError = true
                                                pinInput = ""
                                            }
                                        }
                                    },
                                    showBiometric = BiometricManager.from(this@MainActivity)
                                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS,
                                    onBiometricClick = { launchBiometric() },
                                    isError = pinError,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        return@Surface
                    }

                    var pendingUpdate by remember { mutableStateOf<GitHubRelease?>(null) }
                    LaunchedEffect(Unit) {
                        pendingUpdate = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                    }

                    pendingUpdate?.let { release ->
                        val apk = UpdateChecker.apkAsset(release)
                        AlertDialog(
                            onDismissRequest = { pendingUpdate = null },
                            title = { Text("Update available: ${release.name}") },
                            text = { Text(release.body?.take(300)?.trim() ?: "A new version is available.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (apk != null) downloadApk(apk.downloadUrl, release.tagName)
                                    pendingUpdate = null
                                }) { Text(if (apk != null) "Update" else "Open") }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingUpdate = null }) { Text("Later") }
                            }
                        )
                    }

                    val navController = rememberNavController()

                    // Navigate based on call state
                    LaunchedEffect(callState) {
                        when (callState) {
                            is CallState.Incoming -> {
                                if (navController.currentDestination?.route != "incoming_call") {
                                    navController.navigate("incoming_call")
                                }
                            }
                            is CallState.Outgoing,
                            is CallState.Active -> {
                                if (navController.currentDestination?.route != "active_call") {
                                    navController.navigate("active_call")
                                }
                            }
                            is CallState.Ended -> {
                                val route = navController.currentDestination?.route
                                if (route == "incoming_call" || route == "active_call") {
                                    navController.popBackStack()
                                }
                                callViewModel.resetState()
                            }
                            else -> {}
                        }
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("auth") {
                            AuthScreen(
                                onLoginSuccess = { token ->
                                    prefs.token = token
                                    NetworkModule.authToken = token
                                    AppWebSocketManager.connect(token, NetworkModule.getCurrentUrl())
                                    AppWebSocketManager.onNewMessage = { senderId ->
                                        if (prefs.notificationSound && senderId != prefs.myUserId) {
                                            AppNotificationHelper.playMessageSound(this@MainActivity, true)
                                        }
                                    }
                                    navController.navigate("chat_list") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onUrlChanged = { newUrl ->
                                    prefs.backendUrl = newUrl
                                }
                            )
                        }

                        composable("chat_list") {
                            ru.faustyu.paprika.ui.chat.ChatListScreen(
                                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                                onSearchClick = { navController.navigate("search") },
                                onCreateGroupClick = { navController.navigate("create_group") },
                                onProfileClick = { navController.navigate("profile") },
                                onUrlChanged = { newUrl ->
                                    prefs.backendUrl = newUrl
                                    NetworkModule.setCustomUrl(newUrl)
                                }
                            )
                        }

                        composable("profile") {
                            ru.faustyu.paprika.ui.profile.ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = {
                                    PrefsManager(this@MainActivity).clear()
                                    NetworkModule.authToken = null
                                    AppWebSocketManager.disconnect()
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onSessionsClick = { navController.navigate("sessions") },
                                onChatSettingsClick = { navController.navigate("chat_settings") },
                                onNotificationsClick = { navController.navigate("notifications_settings") },
                                onPrivacyClick = { navController.navigate("privacy_settings") }
                            )
                        }

                        composable("sessions") {
                            SessionsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("chat_settings") {
                            ChatSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("notifications_settings") {
                            NotificationsSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("privacy_settings") {
                            PrivacySettingsScreen(
                                onBack = { navController.popBackStack() },
                                onSessionsClick = { navController.navigate("sessions") }
                            )
                        }

                        composable("media_gallery/{chatId}") { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                            MediaGalleryScreen(
                                chatId = chatId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            ru.faustyu.paprika.ui.search.SearchScreen(
                                onBack = { navController.popBackStack() },
                                onChatJoined = { chatId -> navController.navigate("chat/$chatId") },
                                onNewGroupClick = { navController.navigate("create_group?isChannel=false") },
                                onNewChannelClick = { navController.navigate("create_group?isChannel=true") }
                            )
                        }

                        composable(
                            "create_group?isChannel={isChannel}",
                            arguments = listOf(androidx.navigation.navArgument("isChannel") { 
                                type = androidx.navigation.NavType.BoolType
                                defaultValue = false 
                            })
                        ) { backStackEntry ->
                            val isChannel = backStackEntry.arguments?.getBoolean("isChannel") ?: false
                            ru.faustyu.paprika.ui.groups.CreateGroupScreen(
                                onBack = { navController.popBackStack() },
                                onSuccess = { chatId ->
                                    navController.navigate("chat/$chatId") {
                                        popUpTo("search") { inclusive = true }
                                    }
                                },
                                isChannel = isChannel
                            )
                        }

                        composable("chat/{chatId}") { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: "0"
                            ru.faustyu.paprika.ui.chat.ChatScreen(
                                chatId = chatId,
                                onProfileClick = { userId -> navController.navigate("user_profile/$userId") },
                                onBack = { navController.popBackStack() },
                                onCallClick = { userId, calleeName, callType ->
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    callViewModel.initiateCall(userId, calleeName, callType)
                                },
                                onVideoCircleClick = {
                                    navController.navigate("video_circle/$chatId")
                                },
                                onGroupInfoClick = { gid ->
                                    navController.navigate("group_info/$gid")
                                }
                            )
                        }

                        composable("group_info/{chatId}") { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                            GroupInfoScreen(
                                chatId = chatId,
                                onBack = { navController.popBackStack() },
                                onGroupDeleted = {
                                    navController.navigate("chat_list") {
                                        popUpTo("chat_list") { inclusive = true }
                                    }
                                },
                                onMediaClick = { cid -> navController.navigate("media_gallery/$cid") }
                            )
                        }

                        composable(
                            "user_profile/{userId}",
                            arguments = listOf(androidx.navigation.navArgument("userId") {
                                type = androidx.navigation.NavType.StringType
                            })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: "0"
                            ru.faustyu.paprika.ui.profile.UserProfileScreen(
                                userId = userId,
                                onBack = { navController.popBackStack() },
                                onChatClick = { chatId ->
                                    navController.navigate("chat/$chatId") {
                                        popUpTo("user_profile/$userId") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("video_circle/{chatId}") { backStackEntry ->
                            val cid = backStackEntry.arguments?.getString("chatId") ?: return@composable
                            // We need a reference to the ChatViewModel for this chat
                            // Use a simple wrapper that gets the right ViewModel
                            VideoCircleRoute(
                                chatId = cid,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("incoming_call") {
                            val state = callState as? CallState.Incoming
                            if (state != null) {
                                IncomingCallScreen(
                                    callerName = state.callerName,
                                    callType = state.callType,
                                    callId = state.callId,
                                    callerId = state.callerId,
                                    onAccept = { callId, callerId ->
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        callViewModel.acceptCall(callId, callerId)
                                        navController.navigate("active_call") {
                                            popUpTo("incoming_call") { inclusive = true }
                                        }
                                    },
                                    onReject = { callId ->
                                        callViewModel.rejectCall(callId)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        composable("active_call") {
                            when (val state = callState) {
                                is CallState.Outgoing -> {
                                    CallScreen(
                                        peerName = state.calleeName,
                                        callType = state.callType,
                                        isOutgoing = true,
                                        isMuted = isMuted,
                                        onEndCall = { callViewModel.endCall() },
                                        onToggleMute = { callViewModel.toggleMute() }
                                    )
                                }
                                is CallState.Active -> {
                                    CallScreen(
                                        peerName = state.peerName.ifBlank { "Call" },
                                        callType = state.callType,
                                        isOutgoing = false,
                                        isMuted = isMuted,
                                        onEndCall = { callViewModel.endCall() },
                                        onToggleMute = { callViewModel.toggleMute() }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    private fun downloadApk(url: String, version: String) {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Paprika $version")
            .setDescription("Downloading update...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "paprika-$version.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")
        downloadId = dm.enqueue(request)
    }
}
