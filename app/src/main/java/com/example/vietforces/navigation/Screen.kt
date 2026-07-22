package com.example.vietforces.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    // Bottom navigation screens
    object Main : Screen("main")
    object Leaderboard : Screen("leaderboard")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Performance : Screen("performance")

    // Other screens
    object Notification : Screen("notification")
    object WritingPractice : Screen("writing_practice")
    object LearningPath : Screen("learning_path")
    object Roleplay : Screen("roleplay")

    // Game mode screens
    object ImageToWord : Screen("game/image_to_word")
    object WordToImage : Screen("game/word_to_image")
    object SyllableMatch : Screen("game/syllable_match")
    object SentenceOrder : Screen("game/sentence_order")
    object WordSearch : Screen("game/word_search")
    object FillBlank : Screen("game/fill_blank")
    object WordChain : Screen("game/word_chain")

    // Auth screens
    object Login : Screen("auth/login")
    object Register : Screen("auth/register")

    // Onboarding
    object Onboarding : Screen("onboarding")

    // Daily Challenge
    object DailyChallenge : Screen("daily_challenge")

    // Social screens (Phase 5)
    object SearchUsers : Screen("social/search")

    object PublicProfile : Screen("social/profile/{userId}") {
        fun createRoute(userId: String): String = "social/profile/$userId"
    }

    // Activity feed — friends' daily completions and ELO events (SOCIAL-04)
    object ActivityFeed : Screen("social/feed")

    companion object {
        fun getGameRoute(gameModeId: String): String = "game/$gameModeId"
    }
}

