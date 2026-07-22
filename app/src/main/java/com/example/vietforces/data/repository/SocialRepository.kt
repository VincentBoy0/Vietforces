package com.example.vietforces.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Search result from the leaderboard table — used by SearchUsersScreen.
 * Queried via username ILIKE so that users can find each other by name.
 */
@Serializable
data class UserSearchResult(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("elo_score") val eloScore: Int,
    val streakDays: Int = 0 // not stored in leaderboard table; default 0 unless joined
)

/** Minimal row for reading following_id from the friendships table. */
@Serializable
private data class FriendshipFollowing(
    @SerialName("following_id") val followingId: String
)

/** Row check for isFollowing (same as in PublicProfileViewModel). */
@Serializable
private data class FriendshipCheck(
    @SerialName("follower_id") val followerId: String
)

/** Row inserted when following a user. */
@Serializable
private data class FriendshipInsert(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

/**
 * Repository for social features: user search, follow/unfollow, friends leaderboard.
 *
 * Security notes:
 *  - T-05-03-01: RLS WITH CHECK (follower_id = auth.uid()) prevents impersonation on INSERT.
 *  - T-05-03-02: RLS USING (follower_id = auth.uid()) prevents deleting others' follows.
 *  - T-02-14: No raw exception messages exposed to UI (caller wraps in user-facing text).
 */
@Singleton
class SocialRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {

    /**
     * Search users whose username matches [query] (case-insensitive).
     * Searches the `leaderboard` table which stores username + elo_score.
     * Returns emptyList() when [query] is blank.
     */
    suspend fun searchUsers(query: String): List<UserSearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            supabase.from("leaderboard")
                .select {
                    filter {
                        ilike("username", "%${query.trim()}%")
                    }
                    order("elo_score", Order.DESCENDING)
                    limit(30L)
                }
                .decodeList<UserSearchResult>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Follows [userId] by inserting a row into `friendships`.
     * RLS enforces that follower_id = auth.uid() server-side.
     */
    suspend fun followUser(userId: String): Result<Unit> {
        return try {
            val me = authRepository.currentUserId
                ?: return Result.failure(IllegalStateException("Not authenticated"))
            supabase.from("friendships").insert(
                FriendshipInsert(followerId = me, followingId = userId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unfollows [userId] by deleting the row from `friendships`.
     * RLS enforces that follower_id = auth.uid() server-side.
     */
    suspend fun unfollowUser(userId: String): Result<Unit> {
        return try {
            val me = authRepository.currentUserId
                ?: return Result.failure(IllegalStateException("Not authenticated"))
            supabase.from("friendships").delete {
                filter {
                    eq("follower_id", me)
                    eq("following_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns true if the current user is following [userId].
     */
    suspend fun isFollowing(userId: String): Boolean {
        return try {
            val me = authRepository.currentUserId ?: return false
            val result = supabase.from("friendships")
                .select {
                    filter {
                        eq("follower_id", me)
                        eq("following_id", userId)
                    }
                    limit(1L)
                }
                .decodeList<FriendshipCheck>()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the list of user IDs that the current user is following.
     * Returns emptyList() when not authenticated or on error.
     */
    suspend fun getFollowingIds(): List<String> {
        return try {
            val me = authRepository.currentUserId ?: return emptyList()
            supabase.from("friendships")
                .select {
                    filter {
                        eq("follower_id", me)
                    }
                }
                .decodeList<FriendshipFollowing>()
                .map { it.followingId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns leaderboard entries for users followed by the current user.
     * Returns emptyList() when the follow list is empty or on error.
     *
     * Queries the `leaderboard` table filtering by user_id IN (followingIds).
     */
    suspend fun getFriendsLeaderboard(): List<LeaderboardEntry> {
        return try {
            val followingIds = getFollowingIds()
            if (followingIds.isEmpty()) return emptyList()

            supabase.from("leaderboard")
                .select {
                    filter {
                        isIn("user_id", followingIds)
                    }
                    order("elo_score", Order.DESCENDING)
                    limit(50L)
                }
                .decodeList<LeaderboardEntry>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
