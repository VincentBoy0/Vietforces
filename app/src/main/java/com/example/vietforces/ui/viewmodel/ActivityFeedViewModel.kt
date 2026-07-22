package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * A single row from the activity_events table.
 * The metadata JSONB column contains event-specific data:
 *  - daily_completion: { "challenge_date": "YYYY-MM-DD", "elo_earned": 50 }
 *  - elo_milestone:    { "milestone_elo": 1000 }
 *
 * Security: T-05-04-02 — metadata contains only non-PII gamification data (date + integer).
 */
@Serializable
data class ActivityEventItem(
    @SerialName("id") val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("metadata") val metadata: JsonObject,
    @SerialName("created_at") val createdAt: String
)

/** Minimal leaderboard row used only for the username batch lookup. */
@Serializable
private data class LeaderboardUsername(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String
)

/**
 * Aggregated UI state for the activity feed screen.
 *
 * [usernameMap] maps userId → display name, populated via a secondary leaderboard query.
 * This two-query approach avoids embedded FK select ambiguity (activity_events.user_id
 * may reference auth.users rather than public.users, making PostgREST joins unreliable).
 *
 * Security: T-05-04-01 — RLS policy `activity_events_select_following` on the Supabase
 * side automatically restricts SELECT to events from followed users. No client-side
 * filter is required.
 */
data class ActivityFeedUiState(
    val isLoading: Boolean = true,
    val events: List<ActivityEventItem> = emptyList(),
    val usernameMap: Map<String, String> = emptyMap(),
    val error: String? = null
)

/**
 * ViewModel for ActivityFeedScreen (SOCIAL-04).
 *
 * loadFeed() fetches activity_events for the last 7 days (50 events max).
 * RLS on activity_events automatically scopes results to followed users — no extra filter needed.
 * A second query fetches usernames from leaderboard, keyed by user_id.
 *
 * Security:
 *  - T-05-04-01: RLS `activity_events_select_following` enforces friend-scoping server-side.
 *  - T-05-04-03: limit(50L) + gte("created_at", sevenDaysAgo) caps result set size.
 *  - T-05-04-04: formatRelativeTime uses try/catch to handle malformed timestamps safely.
 */
@HiltViewModel
class ActivityFeedViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    @Suppress("UnusedPrivateMember")
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    /**
     * Loads friend activity events from the last 7 days (max 50).
     * Safe to call multiple times; each call resets isLoading and clears the error.
     */
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // T-05-04-03: 7-day window + 50-row limit caps payload size.
                val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS).toString()

                val events = supabase.from("activity_events")
                    .select {
                        filter {
                            gte("created_at", sevenDaysAgo)
                        }
                        order("created_at", Order.DESCENDING)
                        limit(50L)
                    }
                    .decodeList<ActivityEventItem>()

                // Batch-fetch usernames from leaderboard for all distinct user IDs in the feed.
                val userIds = events.map { it.userId }.distinct()
                val usernameMap: Map<String, String> = if (userIds.isNotEmpty()) {
                    supabase.from("leaderboard")
                        .select {
                            filter {
                                isIn("user_id", userIds)
                            }
                        }
                        .decodeList<LeaderboardUsername>()
                        .associate { it.userId to it.username }
                } else {
                    emptyMap()
                }

                _uiState.value = ActivityFeedUiState(
                    isLoading = false,
                    events = events,
                    usernameMap = usernameMap,
                    error = null
                )
            } catch (e: Exception) {
                // T-02-14: generic user-facing message; raw exception details not exposed.
                _uiState.value = ActivityFeedUiState(
                    isLoading = false,
                    error = "Không thể tải hoạt động bạn bè. Kiểm tra kết nối mạng."
                )
            }
        }
    }

    /** Convenience method for the retry button in ErrorStateComposable. */
    fun retry() {
        loadFeed()
    }
}
