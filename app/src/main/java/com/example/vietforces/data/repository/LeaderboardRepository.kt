package com.example.vietforces.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data model for a leaderboard row.
 * Maps to the `leaderboard` Supabase table.
 * Columns: user_id, username, elo_score, weekly_elo, rank, last_updated
 */
@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("elo_score") val eloScore: Int,
    @SerialName("weekly_elo") val weeklyElo: Int,
    @SerialName("rank") val rank: Int? = null
)

/**
 * Sort mode for leaderboard queries.
 * ALL_TIME  → elo_score DESC
 * THIS_WEEK → weekly_elo DESC
 * FRIENDS   → elo_score DESC filtered to followed users
 */
enum class LeaderboardTab { ALL_TIME, THIS_WEEK, FRIENDS }

/**
 * Repository for leaderboard data.
 * Reads from the public `leaderboard` table (SELECT policy — authenticated users).
 *
 * Security: no writes performed from this client; service_role-only INSERT/UPDATE policy
 * prevents client-side rank manipulation (T-03-10).
 */
@Singleton
class LeaderboardRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository
) {

    /**
     * Fetches the top 50 leaderboard entries ordered by the given [tab].
     * Returns Result.failure on any network/parse error.
     * For FRIENDS tab, delegates to [getFriendsLeaderboard].
     */
    suspend fun getTop50(tab: LeaderboardTab): Result<List<LeaderboardEntry>> {
        if (tab == LeaderboardTab.FRIENDS) return getFriendsLeaderboard()
        return try {
            val sortColumn = if (tab == LeaderboardTab.ALL_TIME) "elo_score" else "weekly_elo"
            val entries = supabase.from("leaderboard")
                .select {
                    order(sortColumn, Order.DESCENDING)
                    limit(50L)
                }
                .decodeList<LeaderboardEntry>()
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches leaderboard entries for users followed by the current user.
     * Returns Result.success(emptyList()) when the follow list is empty.
     */
    suspend fun getFriendsLeaderboard(): Result<List<LeaderboardEntry>> {
        return try {
            val entries = socialRepository.getFriendsLeaderboard()
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the leaderboard entry for the currently authenticated user.
     * Returns Result.success(null) when not logged in or when the user has no row yet.
     */
    suspend fun getMyEntry(): Result<LeaderboardEntry?> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.success(null)
            val entry = supabase.from("leaderboard")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<LeaderboardEntry>()
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
