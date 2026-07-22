package com.example.vietforces

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.NotificationManager
import com.example.vietforces.data.manager.ProfileManager
import com.example.vietforces.data.manager.SettingsManager
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.GameMode
import com.example.vietforces.data.repository.AuthState
import com.example.vietforces.data.repository.ProgressRepository
import com.example.vietforces.data.service.MigrationService
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
import com.example.vietforces.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var migrationService: MigrationService

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

        // Request POST_NOTIFICATIONS permission on Android 13+ (NOTIF-05)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Handle deep-link extras from FCM notification taps (NOTIF-04)
        handleFcmNavigationIntent(intent)

        // Handle deep links for Google OAuth callback
        lifecycleScope.launch { supabase.handleDeeplinks(intent) }

        setContent {
            VietforcesTheme {
                VietforcesApp(migrationService = migrationService)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch { supabase.handleDeeplinks(intent) }
        handleFcmNavigationIntent(intent)
    }

    /**
     * Reads the FCM "navigate_to" extra set by [VietForcesFirebaseMessagingService]
     * and stores it so the Compose NavHost can consume it once navigation is ready.
     * Currently supported values: "daily_challenge".
     */
    private fun handleFcmNavigationIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigate_to") ?: return
        // Store destination so VietforcesApp can consume it via NavController.
        pendingNavigationDestination = navigateTo
    }

    companion object {
        /** Navigation destination requested via FCM notification tap, consumed by VietforcesApp. */
        var pendingNavigationDestination: String? = null
    }

    /**
     * SYNC-01 + SYNC-02: On every foreground resume, pull cloud progress (if newer) then push local.
     * Both are silent no-ops when the user is not logged in.
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            progressRepository.loadFromCloud()   // SYNC-02: overwrite local if cloud is newer
            progressRepository.syncIfLoggedIn()  // SYNC-01: push local state up
        }
    }
}

@Composable
fun VietforcesApp(migrationService: MigrationService) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Main.route
    val context = LocalContext.current

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Determine start destination based on onboarding completion
    val onboardingCompleted = remember { PreferencesManager.getOnboardingCompleted() }
    val startDestination = if (!onboardingCompleted) Screen.Onboarding.route else Screen.Main.route

    // If user is authenticated and currently on an auth screen, navigate to Main
    LaunchedEffect(authState, currentRoute) {
        if (authState is AuthState.Authenticated &&
            currentRoute in listOf(Screen.Login.route, Screen.Register.route)
        ) {
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // ONBOARD-03: On first login, migrate local guest progress to the cloud once.
    // rememberCoroutineScope ties the coroutine lifetime to this composable.
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            coroutineScope.launch { migrationService.migrateIfNeeded() }
        }
    }

    // Determine if we should show bottom navigation
    val showBottomNav = currentRoute in listOf(
        Screen.Main.route,
        Screen.Leaderboard.route,
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
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
            // Onboarding Screen (shown on first launch)
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

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
                    onDailyChallengeClick = {
                        navController.navigate(Screen.DailyChallenge.route)
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
                    onSearchFriendsClick = {
                        navController.navigate(Screen.SearchUsers.route)
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
                ProfileScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Performance Screen
            composable(Screen.Performance.route) {
                PerformanceScreen()
            }

            // Leaderboard Screen
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen()
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

            // Auth: Login
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            // Auth: Register
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // Daily Challenge — full-screen overlay (no bottom nav)
            composable(Screen.DailyChallenge.route) {
                DailyChallengeScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Social: Public Profile — push screen, no bottom nav (SOCIAL-03)
            composable(
                route = Screen.PublicProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                PublicProfileScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Social: Search Users (SOCIAL-01, SOCIAL-02)
            composable(Screen.SearchUsers.route) {
                SearchUsersScreen(
                    onUserClick = { userId ->
                        navController.navigate(Screen.PublicProfile.createRoute(userId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        }

        // Handle FCM deep-link navigation once NavController is ready (NOTIF-04)
        LaunchedEffect(Unit) {
            val dest = MainActivity.pendingNavigationDestination
            if (dest == "daily_challenge") {
                MainActivity.pendingNavigationDestination = null
                navController.navigate(Screen.DailyChallenge.route)
            }
        }

        // Draggable Mascot - appears on all screens, on top of everything including bottom nav
        DraggableMascot()
    }
}