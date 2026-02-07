package com.example.vietforces.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    // Bottom navigation screens
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Performance : Screen("performance")

    // Other screens
    object Notification : Screen("notification")

    // Game mode screens
    object ImageToWord : Screen("game/image_to_word")
    object WordToImage : Screen("game/word_to_image")
    object SyllableMatch : Screen("game/syllable_match")
    object SentenceOrder : Screen("game/sentence_order")
    object WordSearch : Screen("game/word_search")
    object FillBlank : Screen("game/fill_blank")
    object WordChain : Screen("game/word_chain")

    companion object {
        fun getGameRoute(gameModeId: String): String = "game/$gameModeId"
    }
}

