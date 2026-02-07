package com.example.vietforces.data.model

/**
 * User session data containing all progress information.
 * This will be the single source of truth for user stats.
 */
data class UserSession(
    val odlId: String = "default_user",
    var eloRating: Int = 1000,
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var totalCorrectAnswers: Int = 0,
    var totalWrongAnswers: Int = 0,
    var totalExercisesCompleted: Int = 0,
    var lastPracticeDate: String = "", // Format: "yyyy-MM-dd"
    var learnedWordIds: MutableSet<String> = mutableSetOf(),
    var dailyPracticeHistory: MutableMap<String, Int> = mutableMapOf(), // date -> exercises count
    var eloHistory: MutableList<EloHistoryEntry> = mutableListOf(), // Elo history for chart
    var gameModeStats: MutableMap<String, GameModeStats> = mutableMapOf() // Stats per game mode
) {
    val totalAnswers: Int
        get() = totalCorrectAnswers + totalWrongAnswers

    val accuracyRate: Float
        get() = if (totalAnswers > 0) totalCorrectAnswers.toFloat() / totalAnswers else 0f
}

/**
 * Elo history entry for tracking rating changes over time
 */
data class EloHistoryEntry(
    val date: String,      // Format: "yyyy-MM-dd"
    val elo: Int,          // Elo rating at this point
    val change: Int = 0    // Change from previous
)

/**
 * Stats for each game mode
 */
data class GameModeStats(
    var gamesPlayed: Int = 0,
    var correctAnswers: Int = 0,
    var wrongAnswers: Int = 0,
    var bestScore: Int = 0,
    var totalTimePlayed: Long = 0 // in milliseconds
) {
    val totalAnswers: Int
        get() = correctAnswers + wrongAnswers

    val accuracyRate: Float
        get() = if (totalAnswers > 0) correctAnswers.toFloat() / totalAnswers else 0f
}

