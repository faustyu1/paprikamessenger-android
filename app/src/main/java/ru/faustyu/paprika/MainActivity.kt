package ru.faustyu.paprika

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.faustyu.paprika.ui.auth.AuthScreen
import ru.faustyu.paprika.ui.theme.PaprikaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = ru.faustyu.paprika.data.PrefsManager(this)
        
        // Restore Backend URL
        prefs.backendUrl?.let { url ->
            ru.faustyu.paprika.data.network.NetworkModule.setCustomUrl(url)
        }

        // Check for existing session
        val startDestination = if (prefs.token != null) {
            ru.faustyu.paprika.data.network.NetworkModule.authToken = prefs.token
            "chat_list" 
        } else {
            "auth"
        }

        setContent {
            PaprikaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("auth") {
                            AuthScreen(
                                onLoginSuccess = { token ->
                                    prefs.token = token // Save token
                                    ru.faustyu.paprika.data.network.NetworkModule.authToken = token // Set for session
                                    navController.navigate("chat_list") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onUrlChanged = { newUrl ->
                                    prefs.backendUrl = newUrl // Save URL
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
                                    prefs.backendUrl = newUrl
                                    ru.faustyu.paprika.data.network.NetworkModule.setCustomUrl(newUrl)
                                }
                            )
                        }
                        composable("profile") {
                            ru.faustyu.paprika.ui.profile.ProfileScreen(
                                 onBack = { navController.popBackStack() },
                                 onLogout = {
                                     val prefs = ru.faustyu.paprika.data.PrefsManager(this@MainActivity)
                                     prefs.clear()
                                     ru.faustyu.paprika.data.network.NetworkModule.authToken = null
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
                                    // ideally refresh list
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
}
