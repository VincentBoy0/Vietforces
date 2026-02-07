package com.example.vietforces.data.manager

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.vietforces.data.storage.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Notification types
 */
enum class NotificationType {
    ELO_MILESTONE,      // Reached 100 Elo milestone
    RANK_UP,            // Promoted to higher rank
    ACHIEVEMENT,        // General achievement
    STREAK              // Streak milestone
}

/**
 * Notification data class
 */
data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val icon: String = "🔔"
)

/**
 * Manager for app notifications
 * Now persists data to SharedPreferences.
 */
object NotificationManager {
    // List of notifications
    private val _notifications = mutableStateListOf<AppNotification>()
    val notifications: List<AppNotification> get() = _notifications.toList()

    // Unread count
    val unreadCount: Int get() = _notifications.count { !it.isRead }

    // Track last Elo milestone notified (e.g., 1000, 1100, 1200...)
    private var lastEloMilestone by mutableStateOf(0)

    // Track last rank notified
    private var lastRankName by mutableStateOf("")

    private var isInitialized = false

    /**
     * Load notifications from SharedPreferences
     * Call this after PreferencesManager.init()
     */
    fun loadFromPreferences() {
        if (isInitialized) return
        try {
            val savedNotifications = PreferencesManager.loadNotifications()
            _notifications.clear()
            _notifications.addAll(savedNotifications)
            lastEloMilestone = PreferencesManager.getLastEloMilestone()
            lastRankName = PreferencesManager.getLastRankName()
            isInitialized = true
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Save notifications to SharedPreferences
     */
    private fun saveToPreferences() {
        try {
            PreferencesManager.saveNotifications(_notifications.toList())
            PreferencesManager.saveLastEloMilestone(lastEloMilestone)
            PreferencesManager.saveLastRankName(lastRankName)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Initialize with current Elo
     */
    fun initialize(currentElo: Int, currentRankName: String) {
        // Only set defaults if not loaded from preferences
        if (lastEloMilestone == 0) {
            lastEloMilestone = (currentElo / 100) * 100
        }
        if (lastRankName.isEmpty()) {
            lastRankName = currentRankName
        }
    }

    /**
     * Check and create notifications based on Elo change
     */
    fun checkEloMilestone(newElo: Int, newRankName: String) {
        val newMilestone = (newElo / 100) * 100

        // Check for 100 Elo milestone
        if (newMilestone > lastEloMilestone) {
            addNotification(
                AppNotification(
                    type = NotificationType.ELO_MILESTONE,
                    title = "🎉 Cột mốc Elo mới!",
                    message = "Chúc mừng! Bạn đã đạt $newMilestone điểm Elo!",
                    icon = "🏆"
                )
            )
            lastEloMilestone = newMilestone
            saveToPreferences()
        }

        // Check for rank up
        if (newRankName != lastRankName && lastRankName.isNotEmpty()) {
            addNotification(
                AppNotification(
                    type = NotificationType.RANK_UP,
                    title = "⬆️ Thăng hạng!",
                    message = "Xuất sắc! Bạn đã được thăng lên hạng $newRankName!",
                    icon = "👑"
                )
            )
            lastRankName = newRankName
            saveToPreferences()
        }
    }

    /**
     * Add a notification
     */
    fun addNotification(notification: AppNotification) {
        _notifications.add(0, notification) // Add to beginning
        saveToPreferences()
    }

    /**
     * Add streak milestone notification
     */
    fun addStreakNotification(streakDays: Int) {
        if (streakDays > 0 && streakDays % 7 == 0) {
            addNotification(
                AppNotification(
                    type = NotificationType.STREAK,
                    title = "🔥 Chuỗi ngày ấn tượng!",
                    message = "Tuyệt vời! Bạn đã học liên tục $streakDays ngày!",
                    icon = "🔥"
                )
            )
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        val updatedList = _notifications.map { it.copy(isRead = true) }
        _notifications.clear()
        _notifications.addAll(updatedList)
        saveToPreferences()
    }

    /**
     * Clear all read notifications
     */
    fun clearReadNotifications() {
        _notifications.removeAll { it.isRead }
        saveToPreferences()
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        _notifications.clear()
        saveToPreferences()
    }

    /**
     * Get formatted time for notification
     */
    fun getFormattedTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Vừa xong"
            diff < 3600_000 -> "${diff / 60_000} phút trước"
            diff < 86400_000 -> "${diff / 3600_000} giờ trước"
            diff < 604800_000 -> "${diff / 86400_000} ngày trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

