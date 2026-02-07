package com.example.vietforces.data.manager

import com.example.vietforces.data.model.EloHistoryEntry
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.data.model.GameModeStats
import com.example.vietforces.data.model.UserSession
import com.example.vietforces.data.storage.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Singleton manager for user progress, Elo rating, streaks, and heatmap data.
 * Handles all game result calculations and stat updates.
 * Now persists data to SharedPreferences.
 */
object UserProgressManager {

    private var userSession = UserSession()
    private var isInitialized = false

    // Elo constants
    private const val K_FACTOR = 32 // How much Elo can change per game
    private const val BASE_DIFFICULTY = 1000 // Base expected difficulty

    /**
     * Initialize manager by loading data from SharedPreferences.
     * Call this after PreferencesManager.init()
     */
    fun loadFromPreferences() {
        if (isInitialized) return
        try {
            userSession = PreferencesManager.loadUserSession()
            isInitialized = true
            // Initialize NotificationManager with current Elo and rank
            val currentRank = EloRankUtils.getCurrentRank(userSession.eloRating)
            NotificationManager.initialize(userSession.eloRating, EloRankUtils.getVietnameseRankName(currentRank.name))
        } catch (e: Exception) {
            // PreferencesManager not initialized, use defaults
            val currentRank = EloRankUtils.getCurrentRank(userSession.eloRating)
            NotificationManager.initialize(userSession.eloRating, EloRankUtils.getVietnameseRankName(currentRank.name))
        }
    }

    /**
     * Save current session to SharedPreferences
     */
    private fun saveToPreferences() {
        try {
            PreferencesManager.saveUserSession(userSession)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Get current user session
     */
    fun getUserSession(): UserSession = userSession

    /**
     * Get current Elo rating
     */
    fun getEloRating(): Int = userSession.eloRating

    /**
     * Get current streak
     */
    fun getCurrentStreak(): Int = userSession.currentStreak

    /**
     * Get longest streak
     */
    fun getLongestStreak(): Int = userSession.longestStreak

    /**
     * Get daily practice history for heatmap
     */
    fun getDailyPracticeHistory(): Map<String, Int> = userSession.dailyPracticeHistory

    /**
     * Get today's date string
     */
    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Check if user practiced today
     */
    fun hasPracticedToday(): Boolean {
        return userSession.lastPracticeDate == getTodayDateString()
    }

    /**
     * Record a correct answer
     * @param wordDifficulty Difficulty of the word (1-5)
     * @param timeTaken Time taken to answer in milliseconds
     * @return Elo change (positive)
     */
    fun recordCorrectAnswer(wordDifficulty: Int = 1, timeTaken: Long = 0): Int {
        val today = getTodayDateString()

        // Update streak
        updateStreak(today)

        // Calculate Elo change
        val expectedDifficulty = BASE_DIFFICULTY + (wordDifficulty - 1) * 100
        val expectedScore = 1.0 / (1.0 + Math.pow(10.0, (expectedDifficulty - userSession.eloRating) / 400.0))
        var eloChange = (K_FACTOR * (1 - expectedScore)).toInt()

        // Bonus for fast answers (under 3 seconds)
        if (timeTaken in 1..3000) {
            eloChange = (eloChange * 1.2).toInt()
        }

        // Apply Elo change
        userSession.eloRating += eloChange
        userSession.totalCorrectAnswers++
        userSession.totalExercisesCompleted++

        // Update heatmap
        updateHeatmap(today)

        // Update Elo history
        updateEloHistory(today, eloChange)

        // Check for notifications (Elo milestone, rank up)
        val newRank = EloRankUtils.getCurrentRank(userSession.eloRating)
        NotificationManager.checkEloMilestone(userSession.eloRating, EloRankUtils.getVietnameseRankName(newRank.name))

        // Save to SharedPreferences
        saveToPreferences()

        return eloChange
    }

    /**
     * Record a wrong answer
     * @param wordDifficulty Difficulty of the word (1-5)
     * @return Elo change (negative)
     */
    fun recordWrongAnswer(wordDifficulty: Int = 1): Int {
        val today = getTodayDateString()

        // Update streak (still counts as practice)
        updateStreak(today)

        // Calculate Elo change
        val expectedDifficulty = BASE_DIFFICULTY + (wordDifficulty - 1) * 100
        val expectedScore = 1.0 / (1.0 + Math.pow(10.0, (expectedDifficulty - userSession.eloRating) / 400.0))
        val eloChange = (K_FACTOR * (0 - expectedScore)).toInt()

        // Apply Elo change (minimum Elo is 100)
        userSession.eloRating = maxOf(100, userSession.eloRating + eloChange)
        userSession.totalWrongAnswers++
        userSession.totalExercisesCompleted++

        // Update heatmap
        updateHeatmap(today)

        // Update Elo history
        updateEloHistory(today, eloChange)

        // Check for notifications (in case rank changed due to Elo drop)
        val newRank = EloRankUtils.getCurrentRank(userSession.eloRating)
        NotificationManager.checkEloMilestone(userSession.eloRating, EloRankUtils.getVietnameseRankName(newRank.name))

        // Save to SharedPreferences
        saveToPreferences()

        return eloChange
    }

    /**
     * Update streak based on practice date
     */
    private fun updateStreak(today: String) {
        val lastDate = userSession.lastPracticeDate

        if (lastDate.isEmpty()) {
            // First time practicing
            userSession.currentStreak = 1
        } else if (lastDate == today) {
            // Already practiced today, streak unchanged
            return
        } else {
            // Check if yesterday
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val lastPractice = dateFormat.parse(lastDate)
                val todayDate = dateFormat.parse(today)

                if (lastPractice != null && todayDate != null) {
                    val diffInDays = ((todayDate.time - lastPractice.time) / (1000 * 60 * 60 * 24)).toInt()

                    if (diffInDays == 1) {
                        // Practiced yesterday, increment streak
                        userSession.currentStreak++
                    } else {
                        // Missed days, reset streak
                        userSession.currentStreak = 1
                    }
                }
            } catch (e: Exception) {
                userSession.currentStreak = 1
            }
        }

        // Update longest streak
        if (userSession.currentStreak > userSession.longestStreak) {
            userSession.longestStreak = userSession.currentStreak
        }

        userSession.lastPracticeDate = today
    }

    /**
     * Update heatmap data
     */
    private fun updateHeatmap(today: String) {
        val currentCount = userSession.dailyPracticeHistory[today] ?: 0
        userSession.dailyPracticeHistory[today] = currentCount + 1
    }

    /**
     * Update Elo history
     */
    private fun updateEloHistory(date: String, change: Int) {
        userSession.eloHistory.add(
            EloHistoryEntry(
                date = date,
                elo = userSession.eloRating,
                change = change
            )
        )
        // Keep only last 100 entries to save memory
        if (userSession.eloHistory.size > 100) {
            userSession.eloHistory = userSession.eloHistory.takeLast(100).toMutableList()
        }
    }

    /**
     * Get Elo history
     */
    fun getEloHistory(): List<EloHistoryEntry> = userSession.eloHistory

    /**
     * Mark a word as learned
     */
    fun markWordAsLearned(wordId: String) {
        userSession.learnedWordIds.add(wordId)
        saveToPreferences()
    }

    /**
     * Check if a word has been learned
     */
    fun isWordLearned(wordId: String): Boolean {
        return wordId in userSession.learnedWordIds
    }

    /**
     * Get number of words learned
     */
    fun getLearnedWordsCount(): Int = userSession.learnedWordIds.size

    /**
     * Get notification count (achievements, milestones)
     */
    fun getNotificationCount(): Int {
        var count = 0

        // Check for streak milestones
        val streakMilestones = listOf(3, 7, 14, 30, 60, 100)
        if (userSession.currentStreak in streakMilestones) {
            count++
        }

        // Check for Elo milestones
        val eloMilestones = listOf(1100, 1200, 1300, 1500, 1800, 2000)
        if (userSession.eloRating in eloMilestones.map { it..(it + 50) }.flatten()) {
            count++
        }

        return count
    }

    /**
     * Reset all progress (for testing)
     */
    fun resetProgress() {
        userSession = UserSession()
        try {
            PreferencesManager.clearUserSession()
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Record game mode result
     * @param gameModeId ID of the game mode
     * @param correct Number of correct answers
     * @param wrong Number of wrong answers
     * @param score Score achieved
     * @param timePlayed Time played in milliseconds
     */
    fun recordGameModeResult(
        gameModeId: String,
        correct: Int,
        wrong: Int,
        score: Int = 0,
        timePlayed: Long = 0
    ) {
        val stats = userSession.gameModeStats.getOrPut(gameModeId) { GameModeStats() }
        stats.gamesPlayed++
        stats.correctAnswers += correct
        stats.wrongAnswers += wrong
        if (score > stats.bestScore) {
            stats.bestScore = score
        }
        stats.totalTimePlayed += timePlayed

        // Save to SharedPreferences
        saveToPreferences()
    }

    /**
     * Get stats for a specific game mode
     */
    fun getGameModeStats(gameModeId: String): GameModeStats {
        return userSession.gameModeStats[gameModeId] ?: GameModeStats()
    }

    /**
     * Get all game mode stats
     */
    fun getAllGameModeStats(): Map<String, GameModeStats> = userSession.gameModeStats
}

