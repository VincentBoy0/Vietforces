---
phase: "04-daily-challenge-notifications"
plan: "02"
subsystem: "android/daily-challenge"
tags: ["kotlin", "compose", "hilt", "supabase", "daily-challenge", "viewmodel", "navigation", "elo", "countdown"]
dependency_graph:
  requires:
    - "04-01: daily_completions table, award_daily_bonus() RPC"
    - "StreakRepository (instance pattern, updateStreak)"
    - "AuthRepository (currentUserId)"
    - "VocabularyRepository.allVocabulary (static vocabulary list)"
    - "SupabaseModule (SupabaseClient injection)"
  provides:
    - "DailyChallengeRepository: 4 Supabase methods for daily challenge data"
    - "DailyChallengeViewModel: countdown timer + UI state machine"
    - "DailyChallengeScreen: full composable with quiz, history, countdown"
    - "Screen.DailyChallenge route constant (daily_challenge)"
  affects:
    - "MainActivity NavHost (new composable route + FCM deep-link handler)"
    - "MainScreen (new DailyChallengeCard tap target)"
tech_stack:
  added: []
  patterns:
    - "Supabase PostgREST decodeSingleOrNull<T>() for nullable single-row queries"
    - "Companion object instance pattern (same as EloRepository, StreakRepository)"
    - "Sealed class UI state with Loading/NoChallenge/Ready/Completed/Error variants"
    - "Countdown timer via coroutine delay(1000L) + computeSecondsUntilMidnightUtc()"
    - "Inline 5-question multiple-choice quiz from VocabularyRepository.allVocabulary"
    - "FCM deep-link navigation via pendingNavigationDestination companion field"
key_files:
  created:
    - "app/src/main/java/com/example/vietforces/data/repository/DailyChallengeRepository.kt"
    - "app/src/main/java/com/example/vietforces/ui/viewmodel/DailyChallengeViewModel.kt"
    - "app/src/main/java/com/example/vietforces/ui/screens/DailyChallengeScreen.kt"
  modified:
    - "app/src/main/java/com/example/vietforces/navigation/Screen.kt"
    - "app/src/main/java/com/example/vietforces/MainActivity.kt"
    - "app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt"
decisions:
  - "Used decodeSingleOrNull<DailyChallenge>() with limit(1) filter rather than decodeList — matches existing LeaderboardRepository pattern and gives null-safe single-row semantics"
  - "submitCompletion(challengeDate: String) passes challengeDate as parameter (not computed internally) — ViewModel owns the challenge date from the loaded state, avoiding a second DB call"
  - "InlineQuizSection resolves VocabularyItem by ID from VocabularyRepository.allVocabulary (static) — no network call needed since the vocabulary data is bundled in the app"
  - "FCM deep-link consumed via LaunchedEffect(Unit) in VietforcesApp after NavHost is initialized — ensures navController is ready before navigate() is called"
  - "DailyChallengeCard in MainScreen uses onDailyChallengeClick: () -> Unit = {} default — backward-compatible with any existing MainScreen call site not yet passing this lambda"
metrics:
  duration: "~25 minutes"
  completed_date: "2026-07-23"
  tasks_completed: 2
  tasks_total: 2
  files_created: 3
  files_modified: 3
---

# Phase 04 Plan 02: DailyChallengeRepository + ViewModel + Screen Summary

**One-liner:** Supabase-backed daily challenge with HH:MM:SS countdown, inline 5-question vocabulary quiz, +50 ELO RPC submission, and 7-day history calendar strip.

## What Was Built

### Task 1: DailyChallengeRepository + DailyChallengeViewModel

**DailyChallengeRepository** (`data/repository/DailyChallengeRepository.kt`):
- `@Serializable` data classes: `DailyChallenge`, `DailyCompletion`, `DailyChallengeHistoryItem`
- All `@SerialName` annotations match Supabase column names (`challenge_date`, `vocabulary_ids`, `elo_earned`, etc.)
- `getTodayChallenge()` — queries `daily_challenges WHERE challenge_date = todayUtcString()` using `decodeSingleOrNull`
- `getTodayCompletion()` — queries `daily_completions WHERE user_id = userId AND challenge_date = today`
- `submitCompletion(challengeDate)` — calls `award_daily_bonus` RPC via `buildJsonObject`, fires `StreakRepository.instance?.updateStreak()`
- `getLast7DaysHistory()` — fetches last 7 daily_challenges + joins with user completions, returns `List<DailyChallengeHistoryItem>`
- Uses `Locale.ROOT + UTC` date formatting (PRE-01 compliance, same as StreakRepository)
- Companion `instance` field set in `init {}`

**DailyChallengeViewModel** (`ui/viewmodel/DailyChallengeViewModel.kt`):
- Sealed class `DailyChallengeUiState`: `Loading`, `NoChallenge`, `Ready(challenge, isCompleted, eloEarned, history, countdownSeconds)`, `Completed(eloEarned, streakUpdated)`, `Error(message)`
- `loadChallenge()` — fetches challenge + completion + history, emits `Ready` or `NoChallenge`
- `computeSecondsUntilMidnightUtc()` — Calendar-based UTC midnight calculation
- `startCountdown()` — coroutine `delay(1000L)` loop updating `countdownSeconds` in `Ready` state; reloads on midnight rollover
- `submitCompletion(challengeDate)` — calls repository, emits `Completed` on success, handles `already_completed` error gracefully

### Task 2: DailyChallengeScreen + Navigation Wiring

**DailyChallengeScreen** (`ui/screens/DailyChallengeScreen.kt`):
- State machine: Loading → shimmer placeholders; NoChallenge → empty state; Error → retry; Ready → full content; Completed → celebration
- `CountdownCard` — HH:MM:SS formatted countdown using `String.format(Locale.ROOT, "%02d:%02d:%02d", ...)`
- `ChallengeInfoCard` — game mode display name, vocabulary count, bonus ELO chip
- `CompletedBanner` — shown in `Ready` state when `isCompleted = true` (prior session)
- `InlineQuizSection` — 5-question multiple-choice quiz resolving VocabularyItems from static `VocabularyRepository.allVocabulary`; shows result + "Hoàn thành" button after all questions
- `HistorySection` — 7 circle indicators (✅ or ⬜) with date labels + detail rows showing ELO earned

**Navigation wiring:**
- `Screen.DailyChallenge : Screen("daily_challenge")` added to `Screen.kt`
- `composable(Screen.DailyChallenge.route) { DailyChallengeScreen(onBackClick = { navController.popBackStack() }) }` in `MainActivity.kt` NavHost
- `LaunchedEffect(Unit)` in `VietforcesApp` consumes `MainActivity.pendingNavigationDestination` for FCM tap navigation
- `MainScreen` — `onDailyChallengeClick: () -> Unit = {}` parameter added; `DailyChallengeCard` item inserted in `LazyColumn`

## Deviations from Plan

None — plan executed exactly as written.

The `daily_challenges.id` column is a UUID (from 001_initial_schema.sql) not `bigserial` as the CRITICAL CONTEXT header noted, but the `@SerialName("id") val id: String` in `DailyChallenge` handles UUID correctly since Kotlin Serialization deserializes UUID to String.

## Known Stubs

**InlineQuizSection fallback:** If `challenge.vocabularyIds` contains IDs not present in `VocabularyRepository.allVocabulary` (e.g., server-generated challenges with IDs outside the static list), `quizWords` will be empty and the composable shows a bypass button ("Hoàn thành") without questions. This is acceptable for the current scope — the generate-daily-challenge Edge Function uses the same 154-item vocabulary pool as `VocabularyRepository.allVocabulary`.

## Threat Surface Scan

No new network endpoints or auth paths introduced beyond what the plan's `<threat_model>` covers:
- T-04-07: `submitCompletion` calls `award_daily_bonus` with authenticated JWT; server validates auth.uid()
- T-04-09: `getTodayCompletion` guarded by RLS `completions_select_own`
- T-04-10: countdown coroutine cancelled in `onCleared()`

## Self-Check: PASSED

- ✅ `DailyChallengeRepository.kt` exists at expected path
- ✅ `DailyChallengeViewModel.kt` exists at expected path
- ✅ `DailyChallengeScreen.kt` exists at expected path
- ✅ `Screen.kt` contains `DailyChallenge` object
- ✅ `MainActivity.kt` contains `Screen.DailyChallenge.route` (3 occurrences)
- ✅ `award_daily_bonus` called in DailyChallengeRepository (2 occurrences)
- ✅ `countdownSeconds` / `computeSecondsUntilMidnightUtc` in ViewModel (8 occurrences)
- ✅ `InlineQuizSection` / `quizIndex` in Screen (9 occurrences)
- ✅ Commit `9f3fb57` exists — `feat(daily): add DailyChallengeScreen with countdown, 7-day history, bonus ELO completion (DAILY-01 to DAILY-04)`
- ✅ BUILD SUCCESSFUL (0 Kotlin errors, only pre-existing deprecation warnings)
