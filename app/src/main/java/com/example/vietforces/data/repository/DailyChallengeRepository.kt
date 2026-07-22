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

// ---------------------------------------------------------------------------
// Data classes — field names via @SerialName matching Supabase column names
// ---------------------------------------------------------------------------

/**
 * Represents one row from public.daily_challenges.
 * DAILY-01: vocabulary_ids is a JSONB array of VocabularyRepository item IDs.
 */
@Serializable
data class DailyChallenge(
    @SerialName("id")             val id: String,
    @SerialName("challenge_date") val challengeDate: String,
    @SerialName("game_mode")      val gameMode: String,
    @SerialName("vocabulary_ids") val vocabularyIds: List<String>,
    @SerialName("bonus_elo")      val bonusElo: Int
)

/**
 * Represents one row from public.daily_completions.
 * DAILY-03: eloEarned is always 50 on first completion.
 */
@Serializable
data class DailyCompletion(
    @SerialName("user_id")        val userId: String,
    @SerialName("challenge_date") val challengeDate: String,
    @SerialName("elo_earned")     val eloEarned: Int,
    @SerialName("completed_at")   val completedAt: String
)

/**
 * Aggregated view for one day in the 7-day history strip (DAILY-04).
 */
data class DailyChallengeHistoryItem(
    val challengeDate: String,
    val bonusElo: Int,
    val isCompleted: Boolean,
    val eloEarned: Int
)

// ---------------------------------------------------------------------------
// Repository
// ---------------------------------------------------------------------------

/**
 * Supabase-backed data layer for the daily challenge feature.
 *
 * PRE-01: All date strings use Locale.ROOT + UTC timezone (same convention as
 * [StreakRepository]) to prevent locale-dependent formatting bugs.
 *
 * Threat T-04-07 / T-04-09: RPC is SECURITY DEFINER; SELECT is guarded by RLS.
 * The client only calls with the authenticated user's own UID.
 */
@Singleton
class DailyChallengeRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {

    companion object {
        /** Static accessor set in init{} — non-null after first Hilt injection. */
        var instance: DailyChallengeRepository? = null
    }

    init {
        instance = this
    }

    // ── Date helpers ─────────────────────────────────────────────────────────

    /** Today's date as "yyyy-MM-dd" in UTC (PRE-01). */
    private fun todayUtcString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    /** Date string [days] before today in UTC. */
    private fun cutoffDateString(days: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT)
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches today's challenge row (or null if none has been generated yet).
     * DAILY-01: returns the server-generated challenge for today.
     */
    suspend fun getTodayChallenge(): Result<DailyChallenge?> = try {
        val challenge = supabase.from("daily_challenges")
            .select {
                filter { eq("challenge_date", todayUtcString()) }
                limit(1)
            }
            .decodeSingleOrNull<DailyChallenge>()
        Result.success(challenge)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Returns the current user's completion row for today, or null if not yet done.
     * T-04-09: RLS completions_select_own ensures the user sees only their own row.
     */
    suspend fun getTodayCompletion(): Result<DailyCompletion?> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val completion = supabase.from("daily_completions")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("challenge_date", todayUtcString())
                    }
                    limit(1)
                }
                .decodeSingleOrNull<DailyCompletion>()
            Result.success(completion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calls the award_daily_bonus() SECURITY DEFINER RPC.
     * Returns [Result.success] with ELO earned (always 50 on first call per day).
     * Returns [Result.failure] with "already_completed" message on double-submit.
     *
     * DAILY-03: also fires StreakRepository.updateStreak() (server also does this,
     * but calling it locally keeps the local streak counter in sync without a
     * separate network round-trip for the streak response).
     *
     * T-04-07: the server validates auth.uid() from JWT; client-supplied p_user_id
     * cannot exceed the caller's own identity.
     */
    suspend fun submitCompletion(challengeDate: String): Result<Int> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_challenge_date", challengeDate)
            }
            val eloEarned = supabase.postgrest
                .rpc("award_daily_bonus", params)
                .decodeAs<Int>()
            // Fire-and-forget streak update (server already handles it in SQL, but
            // updating locally keeps the local UserProgressManager streak in sync).
            StreakRepository.instance?.updateStreak()
            Result.success(eloEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the last 7 days of challenge + completion history for the calendar strip.
     * DAILY-04: pairs each challenge row with whether the current user completed it.
     */
    suspend fun getLast7DaysHistory(): Result<List<DailyChallengeHistoryItem>> {
        val cutoff = cutoffDateString(6) // today + 6 days back = 7 rows
        return try {
            // 1. Fetch challenges for the last 7 days
            val challenges = supabase.from("daily_challenges")
                .select {
                    filter { gte("challenge_date", cutoff) }
                    order("challenge_date", Order.ASCENDING)
                }
                .decodeList<DailyChallenge>()

            // 2. Fetch completions for the same window (only if logged in)
            val completedDates: Map<String, Int> = authRepository.currentUserId
                ?.let { userId ->
                    try {
                        supabase.from("daily_completions")
                            .select {
                                filter {
                                    eq("user_id", userId)
                                    gte("challenge_date", cutoff)
                                }
                            }
                            .decodeList<DailyCompletion>()
                            .associate { it.challengeDate to it.eloEarned }
                    } catch (_: Exception) {
                        emptyMap()
                    }
                } ?: emptyMap()

            // 3. Join
            val history = challenges.map { challenge ->
                val eloEarned = completedDates[challenge.challengeDate] ?: 0
                DailyChallengeHistoryItem(
                    challengeDate = challenge.challengeDate,
                    bonusElo      = challenge.bonusElo,
                    isCompleted   = completedDates.containsKey(challenge.challengeDate),
                    eloEarned     = eloEarned
                )
            }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
