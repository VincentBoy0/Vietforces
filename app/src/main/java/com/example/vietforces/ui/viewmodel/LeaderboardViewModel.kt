package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.LeaderboardEntry
import com.example.vietforces.data.repository.LeaderboardRepository
import com.example.vietforces.data.repository.LeaderboardTab
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregated UI state for the leaderboard screen.
 *
 * @param isLoading   True while the first (or tab-switch) load is in-flight.
 * @param entries     Top-50 list for the active tab.
 * @param myEntry     The authenticated user's own leaderboard row, or null if not ranked.
 * @param myRank      1-based rank inside the top-50 list; -1 when the user is outside top 50.
 * @param selectedTab Currently active tab (ALL_TIME or THIS_WEEK).
 * @param error       User-facing error message; null when there is no error.
 */
data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val entries: List<LeaderboardEntry> = emptyList(),
    val myEntry: LeaderboardEntry? = null,
    val myRank: Int = -1,
    val selectedTab: LeaderboardTab = LeaderboardTab.ALL_TIME,
    val error: String? = null
)

/**
 * ViewModel for [com.example.vietforces.ui.screens.LeaderboardScreen].
 *
 * Lifecycle responsibilities (LEAD-04):
 *  - Creates a Supabase Realtime channel in [init] and subscribes to INSERT/UPDATE
 *    events on the `leaderboard` table.
 *  - Calls [loadLeaderboard] on every Realtime event for a simple full-reload strategy.
 *  - Removes the channel in [onCleared] so the WebSocket subscription is not leaked.
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    /** Holds the live Realtime channel; null until [subscribeRealtime] completes. */
    private var realtimeChannel: RealtimeChannel? = null

    init {
        loadLeaderboard()
        subscribeRealtime()
    }

    /**
     * Switch to [tab] and re-fetch the leaderboard.
     * Safe to call from the UI thread; internally launches on [viewModelScope].
     */
    fun selectTab(tab: LeaderboardTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, isLoading = true)
        loadLeaderboard()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadLeaderboard() {
        viewModelScope.launch {
            val tab = _uiState.value.selectedTab

            val top50Result = repository.getTop50(tab)
            if (top50Result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    // T-02-14: generic user-facing message — do NOT expose raw exception detail
                    error = "Không thể tải bảng xếp hạng. Kiểm tra kết nối mạng và thử lại."
                )
                return@launch
            }

            val entries = top50Result.getOrDefault(emptyList())
            val myEntry = repository.getMyEntry().getOrNull()

            // Find 1-based position in the fetched top-50 list; -1 = not in top 50 (LEAD-02)
            val myRank = if (myEntry != null) {
                val idx = entries.indexOfFirst { it.userId == myEntry.userId }
                if (idx >= 0) idx + 1 else -1
            } else -1

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = entries,
                myEntry = myEntry,
                myRank = myRank,
                error = null
            )
        }
    }

    /**
     * Subscribes to Realtime INSERT + UPDATE events on the `leaderboard` table (LEAD-01).
     * On each event the entire top-50 list is reloaded — simple full-refresh strategy.
     *
     * Failure is non-critical: the screen still displays the initial data load.
     * T-03-09: single channel per ViewModel instance; SDK handles reconnect with backoff.
     */
    private fun subscribeRealtime() {
        viewModelScope.launch {
            try {
                val channel = supabase.channel("public-leaderboard")
                realtimeChannel = channel

                val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "leaderboard"
                }
                val updateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "leaderboard"
                }

                merge(insertFlow, updateFlow)
                    .onEach { loadLeaderboard() }
                    .launchIn(viewModelScope)

                channel.subscribe()
            } catch (_: Exception) {
                // Non-critical: live updates are optional — initial data load already succeeded.
            }
        }
    }

    /**
     * Removes the Realtime channel when this ViewModel is cleared (LEAD-04).
     * Prevents dangling WebSocket subscriptions after the screen is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeChannel?.let { supabase.realtime.removeChannel(it) }
        }
    }
}
