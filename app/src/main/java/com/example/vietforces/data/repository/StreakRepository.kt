package com.example.vietforces.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streak result returned by the `update_streak` SQL RPC.
 * Field names match the JSON keys from json_build_object().
 *
 * STREAK-01: [streakCount] reflects the server-authoritative streak after freeze logic.
 * STREAK-03: [wasFreezeUsed] is available for display on the post-game results screen.
 */
@Serializable
data class StreakResult(
    @SerialName("streak_count")            val streakCount: Int,
    @SerialName("streak_freeze_available") val freezeAvailable: Boolean,
    @SerialName("was_freeze_used")         val wasFreezeUsed: Boolean
)

/**
 * One entry from the `streak_history` table — a date on which the user practiced.
 * STREAK-04: used as the data source for the streak heatmap in plan 03-04.
 */
@Serializable
data class StreakHistoryEntry(
    @SerialName("practiced_date") val practicedDate: String
)

/**
 * Repository that wraps the `update_streak` Supabase RPC and the `streak_history` table.
 *
 * Companion object [instance] is set in `init {}` so [ProgressRepository.postGame]
 * can call it without full Hilt injection at the call site.
 */
@Singleton
class StreakRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {

    companion object {
        /**
         * Static accessor set during Hilt initialisation.
         * Always non-null after the singleton is first injected.
         * @Volatile ensures cross-thread visibility when read from background threads (WA-02).
         */
        @Volatile var instance: StreakRepository? = null
    }

    init {
        instance = this
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns today's date as "yyyy-MM-dd" in UTC.
     * PRE-01: Always use Locale.ROOT + UTC timezone to avoid locale-dependent
     * formatting that could produce a wrong date string.
     */
    private fun todayUtcString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    /**
     * Returns a UTC date string [days] before today, formatted "yyyy-MM-dd".
     */
    private fun cutoffDateString(days: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT)
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calls the `update_streak` RPC with today's UTC date and returns [StreakResult].
     *
     * STREAK-01: server validates the gap and applies freeze logic — the client never
     * sends a streak delta directly.
     */
    suspend fun updateStreak(): Result<StreakResult> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        val today = todayUtcString()
        return try {
            // Threat T-03-06: date is computed by todayUtcString() using Locale.ROOT + UTC (PRE-01).
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_today_date", today)
            }
            val result = supabase.postgrest
                .rpc("update_streak", params)
                .decodeAs<StreakResult>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the last [days] days of practice history for the current user.
     *
     * STREAK-04: data source for the streak heatmap composable in plan 03-04.
     * Returns [Result.failure] when not logged in or on network error.
     */
    suspend fun getStreakHistory(days: Int = 28): Result<List<StreakHistoryEntry>> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        val cutoff = cutoffDateString(days)
        return try {
            val entries = supabase.from("streak_history")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("practiced_date", cutoff)
                    }
                    order("practiced_date", Order.ASCENDING)
                }
                .decodeList<StreakHistoryEntry>()
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
