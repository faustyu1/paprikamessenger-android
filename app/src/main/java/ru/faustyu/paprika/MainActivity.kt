package ru.faustyu.paprika

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.repository.AuthRepository
import ru.faustyu.paprika.notifications.NotificationHelper
import ru.faustyu.paprika.ui.auth.AuthScreen
import ru.faustyu.paprika.ui.theme.PaprikaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var prefsManager: PrefsManager
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    // Request notification permission for Android 13+
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied - notifications won't work
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Check for deep link from notification
        val openChat = intent.getBooleanExtra("openChat", false)
        val chatId = intent.getStringExtra("chatId")
        
        // Check for existing session
        val startDestination = if (authRepository.isLoggedIn()) {
            if (openChat && chatId != null) {
                "chat/$chatId"  // Open specific chat from notification
            } else {
                "chat_list"
            }
        } else {
            "auth"
        }

        setContent {
            PaprikaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    
                    // Navigate to deep link if opened from notification
                    LaunchedEffect(openChat, chatId) {
                        if (openChat && chatId != null && authRepository.isLoggedIn()) {
                            navController.navigate("chat/$chatId") {
                                popUpTo("chat_list") { inclusive = false }
                            }
                            
                            // Cancel notification for this chat
                            notificationHelper.cancelChatNotification(chatId.toLongOrNull() ?: 0L)
                        }
                    }
                    
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("auth") {
                            AuthScreen(
                                onLoginSuccess = { token ->
                                    navController.navigate("chat_list") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onUrlChanged = { newUrl ->
                                    authRepository.setBackendUrl(newUrl)
                                }
                            )
                        }
                        composable("chat_list") {
                            ru.faustyu.paprika.ui.chat.ChatListScreen(
                                onChatClick = { chatId ->
                                    navController.navigate("chat/$chatId")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onCreateGroupClick = {
                                    navController.navigate("create_group")
                                },
                                onProfileClick = {
                                    navController.navigate("profile")
                                },
                                onUrlChanged = { newUrl ->
                                    authRepository.setBackendUrl(newUrl)
                                }
                            )
                        }
                        composable("profile") {
                            ru.faustyu.paprika.ui.profile.ProfileScreen(
                                 onBack = { navController.popBackStack() },
                                 onLogout = {
                                     authRepository.logout()
                                     navController.navigate("auth") {
                                         popUpTo(0) { inclusive = true }
                                     }
                                 }
                            )
                        }
                        composable("search") {
                            ru.faustyu.paprika.ui.search.SearchScreen(
                                onBack = { navController.popBackStack() },
                                onChatJoined = { chatId ->
                                    navController.navigate("chat/$chatId")
                                }
                            )
                        }
                        composable("create_group") {
                            ru.faustyu.paprika.ui.groups.CreateGroupScreen(
                                onBack = { navController.popBackStack() },
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("chat/{chatId}") { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: "0"
                            ru.faustyu.paprika.ui.chat.ChatScreen(
                                chatId = chatId,
                                onProfileClick = { userId ->
                                    navController.navigate("user_profile/$userId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(
                            "user_profile/{userId}",
                            arguments = listOf(androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                             val userId = backStackEntry.arguments?.getString("userId") ?: "0"
                             ru.faustyu.paprika.ui.profile.UserProfileScreen(
                                 userId = userId,
                                 onBack = { navController.popBackStack() }
                             )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Request notification permission for Android 13+ (API 33+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
