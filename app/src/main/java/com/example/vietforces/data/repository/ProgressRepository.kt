package com.example.vietforces.data.repository

import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.UserProgressDto
import com.example.vietforces.data.remote.RemoteProgressSource
import com.example.vietforces.data.storage.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines local progress (UserProgressManager / SharedPreferences) with remote
 * progress (Supabase user_progress table via RemoteProgressSource).
 *
 * Conflict resolution: last-write-wins based on last_practiced / updated_at timestamps.
 *
 * Game ViewModels can call [ProgressRepository.instance]?.syncIfLoggedIn() after
 * game completion for immediate SYNC-01 coverage.
 * The onResume foreground sync in MainActivity is the guaranteed fallback.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val remoteSource: RemoteProgressSource,
    private val authRepository: AuthRepository
) {

    companion object {
        /**
         * Static accessor for use in places without full Hilt injection
         * (e.g., game managers that want to trigger sync after a session ends).
         * Set in init; always available after first injection.
         */
        var instance: ProgressRepository? = null
    }

    init {
        instance = this
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun nowIso8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date())
    }

    private fun buildDtoFromLocal(userId: String): UserProgressDto {
        val session = UserProgressManager.getUserSession()
        return UserProgressDto(
            userId = userId,
            eloRating = session.eloRating,
            currentStreak = session.currentStreak,
            longestStreak = session.longestStreak,
            wordsLearnedCount = session.learnedWordIds.size,
            lastPracticed = session.lastPracticeDate,
            updatedAt = nowIso8601()
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * SYNC-01: Push local progress to the cloud.
     * No-op and returns success if not logged in.
     */
    suspend fun syncToCloud(): Result<Unit> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return remoteSource.upsertProgress(buildDtoFromLocal(userId))
    }

    /**
     * SYNC-02: Pull cloud progress and overwrite local if cloud is newer.
     * No-op and returns success if not logged in or no cloud row exists.
     *
     * Conflict resolution: last-write-wins — overwrites local only when
     * cloud lastPracticed > local lastPracticeDate (lexicographic ISO 8601 compare).
     */
    suspend fun loadFromCloud(): Result<Unit> {
        val userId = authRepository.currentUserId
            ?: return Result.success(Unit)   // not logged in — silent no-op

        val cloudDto = remoteSource.fetchProgress(userId)
            .getOrNull() ?: return Result.success(Unit)  // no cloud row yet

        val localDate = UserProgressManager.getUserSession().lastPracticeDate
        val cloudDate = cloudDto.lastPracticed

        if (cloudDate.isNotEmpty() && (localDate.isEmpty() || cloudDate > localDate)) {
            // Cloud is newer — overwrite local stats.
            // getUserSession() returns the live mutable object; mutations persist in memory.
            val session = UserProgressManager.getUserSession()
            session.eloRating = cloudDto.eloRating
            session.currentStreak = cloudDto.currentStreak
            session.longestStreak = cloudDto.longestStreak
            session.lastPracticeDate = cloudDto.lastPracticed
            // Note: learnedWordIds is a set of IDs; the cloud only stores the count.
            // We keep the local set intact and do NOT clear it — individual word IDs
            // cannot be restored from the count alone. The count is for display only.
            // Persist the updated session to SharedPreferences.
            PreferencesManager.saveUserSession(session)
        }

        return Result.success(Unit)
    }

    /**
     * Convenience: sync to cloud only when logged in. Returns success silently if not logged in.
     * Suitable for fire-and-forget calls from onResume / post-game hooks.
     */
    suspend fun syncIfLoggedIn(): Result<Unit> {
        if (authRepository.currentUserId == null) return Result.success(Unit)
        return syncToCloud()
    }
}
