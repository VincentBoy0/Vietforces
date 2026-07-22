package com.example.vietforces

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.NotificationManager
import com.example.vietforces.data.manager.ProfileManager
import com.example.vietforces.data.manager.SettingsManager
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.GameMode
import com.example.vietforces.data.storage.PreferencesManager
import com.example.vietforces.navigation.Screen
import com.example.vietforces.ui.components.DraggableMascot
import com.example.vietforces.ui.components.VietforcesBottomNavigation
import com.example.vietforces.ui.screens.*
import com.example.vietforces.ui.screens.game.ImageToWordScreen
import com.example.vietforces.ui.screens.game.WordToImageScreen
import com.example.vietforces.ui.screens.game.SentenceOrderScreen
import com.example.vietforces.ui.screens.game.FillBlankScreen
import com.example.vietforces.ui.screens.game.WordChainScreen
import com.example.vietforces.ui.screens.game.WordSearchScreen
import com.example.vietforces.ui.screens.game.SyllableMatchScreen
import com.example.vietforces.ui.theme.VietforcesTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences and load saved data
        PreferencesManager.init(this)
        SettingsManager.loadFromPreferences()
        UserProgressManager.loadFromPreferences()
        NotificationManager.loadFromPreferences()
        ProfileManager.loadFromPreferences()
        EncounteredItemsManager.loadFromPreferences()
        AiManager.loadFromPreferences()

        enableEdgeToEdge()
        setContent {
            VietforcesTheme {
                VietforcesApp()
            }
        }
    }
}

@Composable
fun VietforcesApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Main.route
    val context = LocalContext.current

    // Determine if we should show bottom navigation
    val showBottomNav = currentRoute in listOf(
        Screen.Main.route,
        Screen.Profile.route,
        Screen.Settings.route,
        Screen.Performance.route
    )

    // Wrap everything in a Box so DraggableMascot can be on top of everything
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomNav) {
                    VietforcesBottomNavigation(
                        currentRoute = currentRoute,
                        onItemClick = { item ->
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(Screen.Main.route) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Main.route,
                modifier = Modifier.padding(innerPadding)
            ) {
            // Main Screen
            composable(Screen.Main.route) {
                MainScreen(
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Main.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNotificationClick = {
                        navController.navigate(Screen.Notification.route)
                    },
                    onWritingPracticeClick = {
                        navController.navigate(Screen.WritingPractice.route)
                    },
                    onLearningPathClick = {
                        navController.navigate(Screen.LearningPath.route)
                    },
                    onRoleplayClick = {
                        navController.navigate(Screen.Roleplay.route)
                    },
                    onGameModeClick = { gameMode ->
                        when (gameMode) {
                            GameMode.IMAGE_TO_WORD -> {
                                navController.navigate(Screen.ImageToWord.route)
                            }
                            GameMode.WORD_TO_IMAGE -> {
                                navController.navigate(Screen.WordToImage.route)
                            }
                            GameMode.SYLLABLE_MATCH -> {
                                navController.navigate(Screen.SyllableMatch.route)
                            }
                            GameMode.SENTENCE_ORDER -> {
                                navController.navigate(Screen.SentenceOrder.route)
                            }
                            GameMode.FILL_BLANK -> {
                                navController.navigate(Screen.FillBlank.route)
                            }
                            GameMode.WORD_CHAIN -> {
                                navController.navigate(Screen.WordChain.route)
                            }
                            GameMode.WORD_SEARCH -> {
                                navController.navigate(Screen.WordSearch.route)
                            }
                            else -> {
                                Toast.makeText(
                                    context,
                                    "Đã chọn: ${gameMode.title}\nĐang phát triển...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Performance Screen
            composable(Screen.Performance.route) {
                PerformanceScreen()
            }

            // Notification Screen
            composable(Screen.Notification.route) {
                NotificationScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Writing Practice (AI-graded)
            composable(Screen.WritingPractice.route) {
                WritingPracticeScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Roleplay conversation tutor (AI chat)
            composable(Screen.Roleplay.route) {
                RoleplayScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Learning Path (AI-generated)
            composable(Screen.LearningPath.route) {
                LearningPathScreen(
                    onBackClick = { navController.popBackStack() },
                    onWritingClick = { navController.navigate(Screen.WritingPractice.route) },
                    onModeClick = { gameMode ->
                        val route = when (gameMode) {
                            GameMode.IMAGE_TO_WORD -> Screen.ImageToWord.route
                            GameMode.WORD_TO_IMAGE -> Screen.WordToImage.route
                            GameMode.SYLLABLE_MATCH -> Screen.SyllableMatch.route
                            GameMode.SENTENCE_ORDER -> Screen.SentenceOrder.route
                            GameMode.FILL_BLANK -> Screen.FillBlank.route
                            GameMode.WORD_CHAIN -> Screen.WordChain.route
                            GameMode.WORD_SEARCH -> Screen.WordSearch.route
                        }
                        navController.navigate(route)
                    }
                )
            }

            // Game: Image to Word
            composable(Screen.ImageToWord.route) {
                ImageToWordScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Word to Image
            composable(Screen.WordToImage.route) {
                WordToImageScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Sentence Order
            composable(Screen.SentenceOrder.route) {
                SentenceOrderScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Fill Blank
            composable(Screen.FillBlank.route) {
                FillBlankScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Word Chain
            composable(Screen.WordChain.route) {
                WordChainScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Word Search
            composable(Screen.WordSearch.route) {
                WordSearchScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Game: Syllable Match
            composable(Screen.SyllableMatch.route) {
                SyllableMatchScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
        }

        // Draggable Mascot - appears on all screens, on top of everything including bottom nav
        DraggableMascot()
    }
}