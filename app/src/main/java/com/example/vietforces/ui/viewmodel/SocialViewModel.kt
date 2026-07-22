package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.SocialRepository
import com.example.vietforces.data.repository.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the search query results.
 */
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Searching : SearchUiState()
    data class Results(val users: List<UserSearchResult>) : SearchUiState()
    object Empty : SearchUiState()
    data class Error(val msg: String) : SearchUiState()
}

/**
 * ViewModel for SearchUsersScreen.
 *
 * Debounces search input (300 ms) to avoid hammering the backend on every keystroke.
 * Maintains [followingIds] as an optimistically-updated Set so the Follow/Unfollow button
 * reflects intent immediately while the network call is in-flight.
 */
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: StateFlow<Set<String>> = _followingIds.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFollowingIds()
    }

    /** Load the current user's following list on init. */
    private fun loadFollowingIds() {
        viewModelScope.launch {
            val ids = try {
                repository.getFollowingIds().toSet()
            } catch (e: Exception) {
                emptySet()
            }
            _followingIds.value = ids
        }
    }

    /**
     * Debounced search — waits 300 ms after the last keystroke before issuing a query.
     * Resets to [SearchUiState.Idle] when [query] is blank.
     */
    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                _searchState.value = SearchUiState.Idle
                return@launch
            }
            delay(300L)
            _searchState.value = SearchUiState.Searching
            try {
                val results = repository.searchUsers(query)
                _searchState.value = if (results.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Results(results)
                }
            } catch (e: Exception) {
                // T-02-14: do not expose raw exception detail to UI
                _searchState.value = SearchUiState.Error("Không thể tìm kiếm. Kiểm tra kết nối mạng.")
            }
        }
    }

    /**
     * Toggles follow/unfollow for [userId] with optimistic UI update.
     * On network failure, the optimistic update is reverted.
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val current = _followingIds.value
            val isCurrentlyFollowing = userId in current

            // Optimistic update
            _followingIds.value = if (isCurrentlyFollowing) {
                current - userId
            } else {
                current + userId
            }

            // Persist to Supabase; revert on failure
            val result = if (isCurrentlyFollowing) {
                repository.unfollowUser(userId)
            } else {
                repository.followUser(userId)
            }
            if (result.isFailure) {
                _followingIds.value = current // revert
            }
        }
    }
}
