package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.AuthRepository
import com.example.vietforces.data.repository.LeaderboardEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Public user progress subset — only reads streak_count and total_games from user_progress.
 * Security: RLS progress_select_public policy (05-01 migration) allows SELECT for any
 * authenticated user on these non-PII gamification columns.
 */
@Serializable
data class UserProgressPublic(
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("total_games") val totalGames: Int = 0
)

/** Minimal row used only for follow-status check (SELECT follower_id). */
@Serializable
private data class FriendshipCheck(
    @SerialName("follower_id") val followerId: String
)

/** Row inserted into friendships when following another user. */
@Serializable
private data class FriendshipInsert(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

/**
 * Merged result of leaderboard + user_progress for a target userId.
 * rankTier is the Vietnamese rank name computed from eloScore via EloRankUtils.
 */
data class PublicProfileData(
    val userId: String,
    val username: String,
    val eloScore: Int,
    val rankTier: String,
    val streakCount: Int,
    val totalGames: Int
)

/**
 * UI state for PublicProfileScreen.
 * isCurrentUser=true when the viewer is looking at their own profile (follow button hidden).
 */
data class PublicProfileUiState(
    val isLoading: Boolean = true,
    val profile: PublicProfileData? = null,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for PublicProfileScreen (SOCIAL-03).
 *
 * loadProfile() fetches leaderboard + user_progress in parallel coroutines, then checks
 * follow status (unless viewing own profile).
 *
 * toggleFollow() performs optimistic UI update and then persists INSERT/DELETE to friendships.
 * Security: RLS on friendships enforces follower_id = auth.uid() server-side (T-05-03-01/02).
 */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    /** Tracks the last requested target so toggleFollow() doesn't need a parameter. */
    private var currentTargetUserId: String? = null

    /**
     * Loads profile data for [targetUserId].
     * Safe to call multiple times (LaunchedEffect key = userId prevents spurious calls).
     * T-05-03-05: LaunchedEffect(userId) key ensures this is only triggered on userId change.
     */
    fun loadProfile(targetUserId: String) {
        currentTargetUserId = targetUserId
        viewModelScope.launch {
            _uiState.value = PublicProfileUiState(isLoading = true)
            try {
                val isCurrentUser = authRepository.currentUserId == targetUserId

                coroutineScope {
                    // T-05-03-04: leaderboard is public SELECT — no PII, consistent with 001 migration.
                    val leaderDeferred = async {
                        supabase.from("leaderboard")
                            .select {
                                filter {
                                    eq("user_id", targetUserId)
                                }
                            }
                            .decodeSingleOrNull<LeaderboardEntry>()
                    }

                    // T-05-03-03: user_progress SELECT accepted — streak/games are non-PII gamification stats.
                    val progressDeferred = async {
                        supabase.from("user_progress")
                            .select {
                                filter {
                                    eq("user_id", targetUserId)
                                }
                            }
                            .decodeSingleOrNull<UserProgressPublic>()
                    }

                    val leaderEntry = leaderDeferred.await()
                    val progressEntry = progressDeferred.await()

                    if (leaderEntry == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Không tìm thấy người dùng này."
                        )
                        return@coroutineScope
                    }

                    val profileData = PublicProfileData(
                        userId = targetUserId,
                        username = leaderEntry.username,
                        eloScore = leaderEntry.eloScore,
                        rankTier = leaderEntry.eloScore.toString(), // raw ELO; rank name computed in Screen
                        streakCount = progressEntry?.streakCount ?: 0,
                        totalGames = progressEntry?.totalGames ?: 0
                    )

                    val isFollowing = if (!isCurrentUser) {
                        checkFollowStatus(targetUserId)
                    } else {
                        false
                    }

                    _uiState.value = PublicProfileUiState(
                        isLoading = false,
                        profile = profileData,
                        isFollowing = isFollowing,
                        isCurrentUser = isCurrentUser,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Không thể tải hồ sơ. Kiểm tra kết nối mạng."
                )
            }
        }
    }

    /**
     * Checks whether the current user follows [targetUserId].
     * Returns false on any network error (non-fatal — follow state can be retried).
     */
    private suspend fun checkFollowStatus(targetUserId: String): Boolean {
        val currentUserId = authRepository.currentUserId ?: return false
        return try {
            val result = supabase.from("friendships")
                .select {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", targetUserId)
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
     * Toggles follow/unfollow for the current target user with optimistic UI update.
     * On error: reverts the optimistic update and surfaces a user-facing error message.
     *
     * Security:
     * - T-05-03-01: RLS WITH CHECK (follower_id = auth.uid()) prevents impersonation on INSERT.
     * - T-05-03-02: RLS USING (follower_id = auth.uid()) prevents deleting others' follows.
     */
    fun toggleFollow() {
        viewModelScope.launch {
            val targetId = currentTargetUserId ?: return@launch
            val currentUserId = authRepository.currentUserId ?: return@launch
            val wasFollowing = _uiState.value.isFollowing

            // Optimistic update
            _uiState.value = _uiState.value.copy(isFollowing = !wasFollowing, error = null)

            try {
                if (wasFollowing) {
                    supabase.from("friendships").delete {
                        filter {
                            eq("follower_id", currentUserId)
                            eq("following_id", targetId)
                        }
                    }
                } else {
                    supabase.from("friendships").insert(
                        FriendshipInsert(
                            followerId = currentUserId,
                            followingId = targetId
                        )
                    )
                }
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _uiState.value = _uiState.value.copy(
                    isFollowing = wasFollowing,
                    error = "Thao tác thất bại. Vui lòng thử lại."
                )
            }
        }
    }
}
