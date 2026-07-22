package com.example.vietforces.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.vietforces.data.manager.AppNotification
import com.example.vietforces.data.manager.EncounteredItem
import com.example.vietforces.data.manager.NotificationType
import com.example.vietforces.data.model.EloHistoryEntry
import com.example.vietforces.data.model.GameModeStats
import com.example.vietforces.data.model.UserSession
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences manager for persisting app data.
 * Handles all data persistence for the app including:
 * - User session (Elo, streaks, stats)
 * - Settings (mascot size, text size)
 * - Notifications
 * - Profile information
 */
object PreferencesManager {

    private const val PREFS_NAME = "vietforces_prefs"

    // Keys for Settings
    private const val KEY_MASCOT_SIZE = "mascot_size_multiplier"
    private const val KEY_MASCOT_TEXT_SIZE = "mascot_text_size_multiplier"

    // Keys for User Session
    private const val KEY_ELO_RATING = "elo_rating"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_CORRECT = "total_correct_answers"
    private const val KEY_TOTAL_WRONG = "total_wrong_answers"
    private const val KEY_TOTAL_EXERCISES = "total_exercises_completed"
    private const val KEY_LAST_PRACTICE_DATE = "last_practice_date"
    private const val KEY_LEARNED_WORD_IDS = "learned_word_ids"
    private const val KEY_DAILY_PRACTICE_HISTORY = "daily_practice_history"
    private const val KEY_ELO_HISTORY = "elo_history"
    private const val KEY_GAME_MODE_STATS = "game_mode_stats"

    // Keys for Profile
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_PHONE = "profile_phone"
    private const val KEY_PROFILE_ADDRESS = "profile_address"

    // Keys for Notifications
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_LAST_ELO_MILESTONE = "last_elo_milestone"
    private const val KEY_LAST_RANK_NAME = "last_rank_name"

    // Keys for Mascot Position
    private const val KEY_MASCOT_POSITION_X = "mascot_position_x"
    private const val KEY_MASCOT_POSITION_Y = "mascot_position_y"
    private const val KEY_MASCOT_IS_LEFT = "mascot_is_left"

    // Keys for Encountered Items (spaced repetition)
    private const val KEY_ENCOUNTERED_ITEMS_PREFIX = "encountered_items_"

    // Keys for AI settings
    private const val KEY_AI_FEEDBACK_ENABLED = "ai_feedback_enabled"
    private const val KEY_AI_MASCOT_ENABLED = "ai_mascot_enabled"

    // Keys for Push Notification preferences (NOTIF-01)
    private const val KEY_NOTIF_STREAK_ENABLED = "notif_streak_enabled"
    private const val KEY_NOTIF_DAILY_ENABLED = "notif_daily_enabled"

    // Key prefix for saved roleplay conversations (one entry per scenario id).
    private const val KEY_ROLEPLAY_PREFIX = "roleplay_session_"

    // Keys for Onboarding / Guest mode
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_SELECTED_LEVEL = "selected_level"
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_GUEST_PROMPT_SHOWN = "guest_game_prompt_shown"

    // Key for one-time guest → cloud migration (ONBOARD-03)
    private const val KEY_MIGRATION_COMPLETED = "migration_completed"

    private var prefs: SharedPreferences? = null

    /**
     * Initialize PreferencesManager with context.
     * Must be called before any other method, typically in Application or MainActivity.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("PreferencesManager not initialized. Call init(context) first.")
    }

    // ==================== SETTINGS ====================

    fun saveMascotSizeMultiplier(multiplier: Float) {
        getPrefs().edit().putFloat(KEY_MASCOT_SIZE, multiplier).apply()
    }

    fun getMascotSizeMultiplier(): Float {
        return getPrefs().getFloat(KEY_MASCOT_SIZE, 1.0f)
    }

    fun saveMascotTextSizeMultiplier(multiplier: Float) {
        getPrefs().edit().putFloat(KEY_MASCOT_TEXT_SIZE, multiplier).apply()
    }

    fun getMascotTextSizeMultiplier(): Float {
        return getPrefs().getFloat(KEY_MASCOT_TEXT_SIZE, 1.0f)
    }

    // ==================== AI SETTINGS ====================

    fun saveAiFeedbackEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_AI_FEEDBACK_ENABLED, enabled).apply()
    }

    fun getAiFeedbackEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_AI_FEEDBACK_ENABLED, true)
    }

    fun saveAiMascotEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_AI_MASCOT_ENABLED, enabled).apply()
    }

    fun getAiMascotEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_AI_MASCOT_ENABLED, true)
    }

    // ==================== PUSH NOTIFICATION PREFERENCES ====================

    fun setNotifStreakEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_NOTIF_STREAK_ENABLED, enabled).apply()
    }

    fun getNotifStreakEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_NOTIF_STREAK_ENABLED, true)
    }

    fun setNotifDailyEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_NOTIF_DAILY_ENABLED, enabled).apply()
    }

    fun getNotifDailyEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_NOTIF_DAILY_ENABLED, true)
    }

    // ==================== ONBOARDING / GUEST ====================

    fun setOnboardingCompleted(completed: Boolean) {
        getPrefs().edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getOnboardingCompleted(): Boolean = getPrefs().getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setIsGuest(guest: Boolean) {
        getPrefs().edit().putBoolean(KEY_IS_GUEST, guest).apply()
    }

    fun getIsGuest(): Boolean = getPrefs().getBoolean(KEY_IS_GUEST, false)

    fun setSelectedLevel(level: String) {
        getPrefs().edit().putString(KEY_SELECTED_LEVEL, level).apply()
    }

    fun getSelectedLevel(): String = getPrefs().getString(KEY_SELECTED_LEVEL, "beginner") ?: "beginner"

    fun setDailyGoal(goal: Int) {
        getPrefs().edit().putInt(KEY_DAILY_GOAL, goal).apply()
    }

    fun getDailyGoal(): Int = getPrefs().getInt(KEY_DAILY_GOAL, 10)

    fun setGuestPromptShown(shown: Boolean) {
        getPrefs().edit().putBoolean(KEY_GUEST_PROMPT_SHOWN, shown).apply()
    }

    fun getGuestPromptShown(): Boolean = getPrefs().getBoolean(KEY_GUEST_PROMPT_SHOWN, false)

    // ==================== MIGRATION FLAG ====================

    /**
     * Marks the one-time guest→cloud migration as completed (ONBOARD-03).
     * After this returns true, [MigrationService.migrateIfNeeded] becomes a no-op.
     */
    fun setMigrationCompleted(done: Boolean) {
        getPrefs().edit().putBoolean(KEY_MIGRATION_COMPLETED, done).apply()
    }

    fun getMigrationCompleted(): Boolean = getPrefs().getBoolean(KEY_MIGRATION_COMPLETED, false)

    // ==================== ROLEPLAY SESSIONS ====================

    /** Persist a roleplay conversation (serialized JSON) for one scenario. */
    fun saveRoleplaySession(scenarioId: String, json: String) {
        getPrefs().edit().putString(KEY_ROLEPLAY_PREFIX + scenarioId, json).apply()
    }

    /** Restore a saved roleplay conversation, or null if none was saved. */
    fun getRoleplaySession(scenarioId: String): String? {
        return getPrefs().getString(KEY_ROLEPLAY_PREFIX + scenarioId, null)
    }

    /** Forget the saved conversation for a scenario ("start over"). */
    fun clearRoleplaySession(scenarioId: String) {
        getPrefs().edit().remove(KEY_ROLEPLAY_PREFIX + scenarioId).apply()
    }

    // ==================== USER SESSION ====================

    fun saveUserSession(session: UserSession) {
        getPrefs().edit().apply {
            putInt(KEY_ELO_RATING, session.eloRating)
            putInt(KEY_CURRENT_STREAK, session.currentStreak)
            putInt(KEY_LONGEST_STREAK, session.longestStreak)
            putInt(KEY_TOTAL_CORRECT, session.totalCorrectAnswers)
            putInt(KEY_TOTAL_WRONG, session.totalWrongAnswers)
            putInt(KEY_TOTAL_EXERCISES, session.totalExercisesCompleted)
            putString(KEY_LAST_PRACTICE_DATE, session.lastPracticeDate)
            putStringSet(KEY_LEARNED_WORD_IDS, session.learnedWordIds)
            putString(KEY_DAILY_PRACTICE_HISTORY, dailyPracticeHistoryToJson(session.dailyPracticeHistory))
            putString(KEY_ELO_HISTORY, eloHistoryToJson(session.eloHistory))
            putString(KEY_GAME_MODE_STATS, gameModeStatsToJson(session.gameModeStats))
            apply()
        }
    }

    fun loadUserSession(): UserSession {
        val prefs = getPrefs()
        return UserSession(
            eloRating = prefs.getInt(KEY_ELO_RATING, 1000),
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0),
            totalCorrectAnswers = prefs.getInt(KEY_TOTAL_CORRECT, 0),
            totalWrongAnswers = prefs.getInt(KEY_TOTAL_WRONG, 0),
            totalExercisesCompleted = prefs.getInt(KEY_TOTAL_EXERCISES, 0),
            lastPracticeDate = prefs.getString(KEY_LAST_PRACTICE_DATE, "") ?: "",
            learnedWordIds = prefs.getStringSet(KEY_LEARNED_WORD_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf(),
            dailyPracticeHistory = jsonToDailyPracticeHistory(prefs.getString(KEY_DAILY_PRACTICE_HISTORY, null)),
            eloHistory = jsonToEloHistory(prefs.getString(KEY_ELO_HISTORY, null)),
            gameModeStats = jsonToGameModeStats(prefs.getString(KEY_GAME_MODE_STATS, null))
        )
    }

    // Quick save methods for individual fields (for performance)
    fun saveEloRating(elo: Int) {
        getPrefs().edit().putInt(KEY_ELO_RATING, elo).apply()
    }

    fun saveCurrentStreak(streak: Int) {
        getPrefs().edit().putInt(KEY_CURRENT_STREAK, streak).apply()
    }

    fun saveLongestStreak(streak: Int) {
        getPrefs().edit().putInt(KEY_LONGEST_STREAK, streak).apply()
    }

    fun saveLastPracticeDate(date: String) {
        getPrefs().edit().putString(KEY_LAST_PRACTICE_DATE, date).apply()
    }

    fun saveDailyPracticeHistory(history: Map<String, Int>) {
        getPrefs().edit().putString(KEY_DAILY_PRACTICE_HISTORY, dailyPracticeHistoryToJson(history)).apply()
    }

    fun saveEloHistory(history: List<EloHistoryEntry>) {
        getPrefs().edit().putString(KEY_ELO_HISTORY, eloHistoryToJson(history.toMutableList())).apply()
    }

    fun saveGameModeStats(stats: Map<String, GameModeStats>) {
        getPrefs().edit().putString(KEY_GAME_MODE_STATS, gameModeStatsToJson(stats.toMutableMap())).apply()
    }

    fun saveTotalAnswers(correct: Int, wrong: Int, total: Int) {
        getPrefs().edit()
            .putInt(KEY_TOTAL_CORRECT, correct)
            .putInt(KEY_TOTAL_WRONG, wrong)
            .putInt(KEY_TOTAL_EXERCISES, total)
            .apply()
    }

    // ==================== PROFILE ====================

    fun saveProfileName(name: String) {
        getPrefs().edit().putString(KEY_PROFILE_NAME, name).apply()
    }

    fun getProfileName(): String {
        return getPrefs().getString(KEY_PROFILE_NAME, "") ?: ""
    }

    fun saveProfilePhone(phone: String) {
        getPrefs().edit().putString(KEY_PROFILE_PHONE, phone).apply()
    }

    fun getProfilePhone(): String {
        return getPrefs().getString(KEY_PROFILE_PHONE, "") ?: ""
    }

    fun saveProfileAddress(address: String) {
        getPrefs().edit().putString(KEY_PROFILE_ADDRESS, address).apply()
    }

    fun getProfileAddress(): String {
        return getPrefs().getString(KEY_PROFILE_ADDRESS, "") ?: ""
    }

    // ==================== NOTIFICATIONS ====================

    fun saveNotifications(notifications: List<AppNotification>) {
        getPrefs().edit().putString(KEY_NOTIFICATIONS, notificationsToJson(notifications)).apply()
    }

    fun loadNotifications(): List<AppNotification> {
        val json = getPrefs().getString(KEY_NOTIFICATIONS, null)
        return jsonToNotifications(json)
    }

    fun saveLastEloMilestone(milestone: Int) {
        getPrefs().edit().putInt(KEY_LAST_ELO_MILESTONE, milestone).apply()
    }

    fun getLastEloMilestone(): Int {
        return getPrefs().getInt(KEY_LAST_ELO_MILESTONE, 0)
    }

    fun saveLastRankName(rankName: String) {
        getPrefs().edit().putString(KEY_LAST_RANK_NAME, rankName).apply()
    }

    fun getLastRankName(): String {
        return getPrefs().getString(KEY_LAST_RANK_NAME, "") ?: ""
    }

    // ==================== MASCOT POSITION ====================

    fun saveMascotPosition(x: Float, y: Float, isLeft: Boolean) {
        getPrefs().edit()
            .putFloat(KEY_MASCOT_POSITION_X, x)
            .putFloat(KEY_MASCOT_POSITION_Y, y)
            .putBoolean(KEY_MASCOT_IS_LEFT, isLeft)
            .apply()
    }

    fun getMascotPositionX(): Float = getPrefs().getFloat(KEY_MASCOT_POSITION_X, -1f)
    fun getMascotPositionY(): Float = getPrefs().getFloat(KEY_MASCOT_POSITION_Y, -1f)
    fun getMascotIsLeft(): Boolean = getPrefs().getBoolean(KEY_MASCOT_IS_LEFT, false)

    // ==================== CLEAR DATA ====================

    fun clearAllData() {
        getPrefs().edit().clear().apply()
    }

    fun clearUserSession() {
        getPrefs().edit().apply {
            remove(KEY_ELO_RATING)
            remove(KEY_CURRENT_STREAK)
            remove(KEY_LONGEST_STREAK)
            remove(KEY_TOTAL_CORRECT)
            remove(KEY_TOTAL_WRONG)
            remove(KEY_TOTAL_EXERCISES)
            remove(KEY_LAST_PRACTICE_DATE)
            remove(KEY_LEARNED_WORD_IDS)
            remove(KEY_DAILY_PRACTICE_HISTORY)
            remove(KEY_ELO_HISTORY)
            remove(KEY_GAME_MODE_STATS)
            apply()
        }
    }

    // ==================== JSON HELPERS ====================

    private fun dailyPracticeHistoryToJson(history: Map<String, Int>): String {
        val jsonObject = JSONObject()
        history.forEach { (date, count) ->
            jsonObject.put(date, count)
        }
        return jsonObject.toString()
    }

    private fun jsonToDailyPracticeHistory(json: String?): MutableMap<String, Int> {
        if (json.isNullOrEmpty()) return mutableMapOf()
        return try {
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getInt(key)
            }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun eloHistoryToJson(history: MutableList<EloHistoryEntry>): String {
        val jsonArray = JSONArray()
        history.forEach { entry ->
            val jsonObject = JSONObject().apply {
                put("date", entry.date)
                put("elo", entry.elo)
                put("change", entry.change)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun jsonToEloHistory(json: String?): MutableList<EloHistoryEntry> {
        if (json.isNullOrEmpty()) return mutableListOf()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<EloHistoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    EloHistoryEntry(
                        date = obj.getString("date"),
                        elo = obj.getInt("elo"),
                        change = obj.optInt("change", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun gameModeStatsToJson(stats: MutableMap<String, GameModeStats>): String {
        val jsonObject = JSONObject()
        stats.forEach { (mode, stat) ->
            val statJson = JSONObject().apply {
                put("gamesPlayed", stat.gamesPlayed)
                put("correctAnswers", stat.correctAnswers)
                put("wrongAnswers", stat.wrongAnswers)
                put("bestScore", stat.bestScore)
                put("totalTimePlayed", stat.totalTimePlayed)
            }
            jsonObject.put(mode, statJson)
        }
        return jsonObject.toString()
    }

    private fun jsonToGameModeStats(json: String?): MutableMap<String, GameModeStats> {
        if (json.isNullOrEmpty()) return mutableMapOf()
        return try {
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, GameModeStats>()
            jsonObject.keys().forEach { mode ->
                val statJson = jsonObject.getJSONObject(mode)
                map[mode] = GameModeStats(
                    gamesPlayed = statJson.optInt("gamesPlayed", 0),
                    correctAnswers = statJson.optInt("correctAnswers", 0),
                    wrongAnswers = statJson.optInt("wrongAnswers", 0),
                    bestScore = statJson.optInt("bestScore", 0),
                    totalTimePlayed = statJson.optLong("totalTimePlayed", 0)
                )
            }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun notificationsToJson(notifications: List<AppNotification>): String {
        val jsonArray = JSONArray()
        notifications.forEach { notification ->
            val jsonObject = JSONObject().apply {
                put("id", notification.id)
                put("type", notification.type.name)
                put("title", notification.title)
                put("message", notification.message)
                put("timestamp", notification.timestamp)
                put("isRead", notification.isRead)
                put("icon", notification.icon)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun jsonToNotifications(json: String?): List<AppNotification> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = obj.getString("id"),
                        type = NotificationType.valueOf(obj.getString("type")),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.getLong("timestamp"),
                        isRead = obj.getBoolean("isRead"),
                        icon = obj.optString("icon", "🔔")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== ENCOUNTERED ITEMS (Spaced Repetition) ====================

    fun saveEncounteredItems(gameModeKey: String, items: Map<String, EncounteredItem>) {
        val json = encounteredItemsToJson(items)
        getPrefs().edit().putString(KEY_ENCOUNTERED_ITEMS_PREFIX + gameModeKey, json).apply()
    }

    fun loadEncounteredItems(gameModeKey: String): Map<String, EncounteredItem> {
        val json = getPrefs().getString(KEY_ENCOUNTERED_ITEMS_PREFIX + gameModeKey, null)
        return jsonToEncounteredItems(json)
    }

    fun clearEncounteredItems(gameModeKey: String) {
        getPrefs().edit().remove(KEY_ENCOUNTERED_ITEMS_PREFIX + gameModeKey).apply()
    }

    fun clearAllEncounteredItems() {
        val editor = getPrefs().edit()
        getPrefs().all.keys
            .filter { it.startsWith(KEY_ENCOUNTERED_ITEMS_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private fun encounteredItemsToJson(items: Map<String, EncounteredItem>): String {
        val jsonObject = JSONObject()
        items.forEach { (id, item) ->
            val itemJson = JSONObject().apply {
                put("itemId", item.itemId)
                put("encounterCount", item.encounterCount)
                put("lastEncounteredTime", item.lastEncounteredTime)
                put("correctCount", item.correctCount)
                put("wrongCount", item.wrongCount)
            }
            jsonObject.put(id, itemJson)
        }
        return jsonObject.toString()
    }

    private fun jsonToEncounteredItems(json: String?): Map<String, EncounteredItem> {
        if (json.isNullOrEmpty()) return emptyMap()
        return try {
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, EncounteredItem>()
            jsonObject.keys().forEach { id ->
                val itemJson = jsonObject.getJSONObject(id)
                map[id] = EncounteredItem(
                    itemId = itemJson.getString("itemId"),
                    encounterCount = itemJson.optInt("encounterCount", 0),
                    lastEncounteredTime = itemJson.optLong("lastEncounteredTime", 0),
                    correctCount = itemJson.optInt("correctCount", 0),
                    wrongCount = itemJson.optInt("wrongCount", 0)
                )
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

