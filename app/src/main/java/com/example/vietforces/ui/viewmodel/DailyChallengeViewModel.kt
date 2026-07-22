package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.DailyChallenge
import com.example.vietforces.data.repository.DailyChallengeHistoryItem
import com.example.vietforces.data.repository.DailyChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

sealed class DailyChallengeUiState {
    /** Initial loading — show shimmer placeholders. */
    object Loading : DailyChallengeUiState()

    /** Server has no challenge row for today. */
    object NoChallenge : DailyChallengeUiState()

    /**
     * A challenge exists and we know whether the user completed it.
     * DAILY-02: countdownSeconds ticks down via startCountdown().
     */
    data class Ready(
        val challenge: DailyChallenge,
        val isCompleted: Boolean,
        val eloEarned: Int,
        val history: List<DailyChallengeHistoryItem>,
        val countdownSeconds: Long
    ) : DailyChallengeUiState()

    /** User just completed the challenge this session. DAILY-03. */
    data class Completed(
        val eloEarned: Int,
        val streakUpdated: Boolean = true
    ) : DailyChallengeUiState()

    /** Network / server error. */
    data class Error(val message: String) : DailyChallengeUiState()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * ViewModel for DailyChallengeScreen.
 *
 * Responsibilities:
 * - DAILY-01: load today's challenge from [DailyChallengeRepository]
 * - DAILY-02: countdown timer (seconds until midnight UTC) that ticks every second
 * - DAILY-03: submit completion → update ELO + streak via the repository
 * - DAILY-04: load 7-day history for the calendar strip
 *
 * T-04-10: countdown uses delay(1000L) — lightweight; cancelled in onCleared().
 */
@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val repository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyChallengeUiState>(DailyChallengeUiState.Loading)
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    /** Coroutine that ticks countdownSeconds once per second. */
    private var countdownJob: Job? = null

    init {
        loadChallenge()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Reload challenge data (called on init, retry, and after midnight rollover). */
    fun loadChallenge() {
        viewModelScope.launch {
            _uiState.value = DailyChallengeUiState.Loading
            countdownJob?.cancel()

            // 1. Fetch today's challenge
            val challengeResult = repository.getTodayChallenge()
            if (challengeResult.isFailure) {
                _uiState.value = DailyChallengeUiState.Error(
                    "Không thể tải thử thách. Kiểm tra kết nối mạng."
                )
                return@launch
            }
            val challenge = challengeResult.getOrNull()
            if (challenge == null) {
                _uiState.value = DailyChallengeUiState.NoChallenge
                return@launch
            }

            // 2. Check completion status
            val completion = repository.getTodayCompletion().getOrNull()

            // 3. Load 7-day history
            val history = repository.getLast7DaysHistory().getOrDefault(emptyList())

            // 4. Emit Ready state
            _uiState.value = DailyChallengeUiState.Ready(
                challenge        = challenge,
                isCompleted      = completion != null,
                eloEarned        = completion?.eloEarned ?: 0,
                history          = history,
                countdownSeconds = computeSecondsUntilMidnightUtc()
            )

            // 5. Start countdown only when not yet completed
            if (completion == null) {
                startCountdown()
            }
        }
    }

    /**
     * Submit challenge completion.
     * On success → emits [DailyChallengeUiState.Completed].
     * On "already_completed" → reloads (edge case: concurrent session on another device).
     * On other errors → emits [DailyChallengeUiState.Error].
     */
    fun submitCompletion(challengeDate: String) {
        viewModelScope.launch {
            val result = repository.submitCompletion(challengeDate)
            result
                .onSuccess { eloEarned ->
                    countdownJob?.cancel()
                    _uiState.value = DailyChallengeUiState.Completed(
                        eloEarned      = eloEarned,
                        streakUpdated  = true
                    )
                }
                .onFailure { e ->
                    if (e.message?.contains("already_completed") == true) {
                        loadChallenge() // Reload to show already-completed state
                    } else {
                        _uiState.value = DailyChallengeUiState.Error(
                            "Lỗi khi hoàn thành thử thách. Vui lòng thử lại."
                        )
                    }
                }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Computes seconds remaining until the next UTC midnight.
     * DAILY-02: used to initialise and update the HH:MM:SS countdown display.
     */
    fun computeSecondsUntilMidnightUtc(): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val msUntilMidnight = cal.timeInMillis - now
        return if (msUntilMidnight > 0L) msUntilMidnight / 1000L else 0L
    }

    /**
     * Starts a coroutine that decrements [DailyChallengeUiState.Ready.countdownSeconds]
     * once per second.  After midnight, reloads the challenge for the new day.
     * T-04-10: cancelled on [onCleared].
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                if (state is DailyChallengeUiState.Ready) {
                    val secs = computeSecondsUntilMidnightUtc()
                    if (secs <= 0L) {
                        loadChallenge() // New day — reload
                        break
                    }
                    _uiState.value = state.copy(countdownSeconds = secs)
                } else {
                    break
                }
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
