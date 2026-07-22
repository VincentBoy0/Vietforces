---
phase: 03-streak-leaderboard
plan: "03"
subsystem: leaderboard
tags: [leaderboard, realtime, viewmodel, compose, supabase, hilt, navigation]
dependency_graph:
  requires:
    - 03-01 (weekly_elo column migration)
    - SupabaseModule (Realtime plugin installed)
    - AuthRepository (currentUserId for own-rank query)
  provides:
    - LeaderboardRepository (getTop50, getMyEntry)
    - LeaderboardViewModel (StateFlow + Realtime lifecycle)
    - LeaderboardScreen (full UI)
    - Screen.Leaderboard route
    - BottomNavItem.Leaderboard nav entry
  affects:
    - MainActivity.kt (NavHost + showBottomNav)
    - BottomNavigation.kt (items list)
    - Screen.kt (sealed class)
tech_stack:
  added:
    - supabase-kt Realtime (postgresChangeFlow, channel, removeChannel)
    - kotlinx.coroutines.flow.merge for INSERT+UPDATE event merging
  patterns:
    - HiltViewModel with StateFlow<LeaderboardUiState>
    - Realtime channel created in init{}, removed in onCleared() (LEAD-04)
    - Full-reload on Realtime event (simple + correct for top-50 display)
    - EloRankUtils.getRankColor() for tier badges derived from eloScore
key_files:
  created:
    - app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt
    - app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt
    - app/src/main/java/com/example/vietforces/ui/screens/LeaderboardScreen.kt
  modified:
    - app/src/main/java/com/example/vietforces/navigation/Screen.kt
    - app/src/main/java/com/example/vietforces/ui/components/BottomNavigation.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
decisions:
  - LeaderboardTab enum co-located in LeaderboardRepository.kt for single-file cohesion
  - Used Icons.Filled.EmojiEvents / Icons.Outlined.EmojiEvents (plan-approved fallback; both in material-icons-extended)
  - Full-reload strategy on Realtime events (vs. in-place mutation) — simpler, correct for top-50 rank reorder
  - rank_tier not fetched from DB (column absent from schema); computed client-side via EloRankUtils
  - showBottomNav allowlist in MainActivity updated to include Screen.Leaderboard.route
metrics:
  duration: "~25 minutes"
  completed: "2026-07-23"
  tasks_completed: 2
  files_created: 3
  files_modified: 3
requirements:
  - LEAD-01
  - LEAD-02
  - LEAD-03
  - LEAD-04
---

# Phase 03 Plan 03: LeaderboardScreen + Realtime ViewModel Summary

**One-liner:** Full leaderboard feature — Top-50 Supabase postgrest queries, Realtime INSERT/UPDATE subscription with lifecycle-aware cancellation, two-tab Compose UI with own-rank footer.

## Tasks Completed

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | LeaderboardRepository + Screen.kt + BottomNavigation.kt | b8e8ac7 | LeaderboardRepository.kt, Screen.kt, BottomNavigation.kt |
| 2 | LeaderboardViewModel + LeaderboardScreen + MainActivity wiring | b8e8ac7 | LeaderboardViewModel.kt, LeaderboardScreen.kt, MainActivity.kt |

Both tasks committed together in one cohesive commit after full compile verification.

## What Was Built

### LeaderboardRepository.kt

- `@Serializable data class LeaderboardEntry(userId, username, eloScore, weeklyElo, rank?)` — maps to `leaderboard` Supabase table columns
- `enum class LeaderboardTab { ALL_TIME, THIS_WEEK }` — tab selector for sort column
- `@Singleton class LeaderboardRepository @Inject constructor(supabase, authRepository)`:
  - `getTop50(tab)` — `SELECT ... ORDER BY elo_score|weekly_elo DESC LIMIT 50`
  - `getMyEntry()` — `SELECT ... WHERE user_id = currentUserId` returns own row or null

### LeaderboardViewModel.kt

- `data class LeaderboardUiState(isLoading, entries, myEntry, myRank, selectedTab, error)`
- `@HiltViewModel class LeaderboardViewModel`:
  - `init {}` calls `loadLeaderboard()` + `subscribeRealtime()` (LEAD-01)
  - `selectTab(tab)` — updates state and reloads
  - `loadLeaderboard()` — fetches top50 + myEntry in sequence; computes `myRank` (1-based in list, -1 if absent) (LEAD-02)
  - `subscribeRealtime()` — creates `supabase.channel("public-leaderboard")`, merges `postgresChangeFlow<Insert>` + `postgresChangeFlow<Update>`, calls `loadLeaderboard()` on each event
  - `onCleared()` — `supabase.realtime.removeChannel(channel)` (LEAD-04)

### LeaderboardScreen.kt

- `@Composable fun LeaderboardScreen(viewModel = hiltViewModel())`:
  - `TabRow` with "All-time 🏆" and "Tuần này 📅" tabs (LEAD-03)
  - Loading: 3 `ShimmerBox` placeholders (UX-02/UX-03)
  - Error: `ErrorStateComposable` with retry (UX-04)
  - Empty: `EmptyStateComposable` with 🏆 illustration (UX-01)
  - Success: `LazyColumn` with `LeaderboardRow` items (`key = userId`)
  - Own-rank footer: shown when `myEntry != null && myRank == -1`; "Vị trí của bạn (ngoài top 50)" label + VietRed-tinted row (LEAD-02)
- `@Composable private fun LeaderboardRow(rank, entry, isMe)`:
  - Medal emojis 🥇🥈🥉 for ranks 1–3; Gold/Silver/Bronze colour coding
  - Rank-tier badge from `EloRankUtils.getRankColor(eloScore)` + Vietnamese tier name
  - Own-user row highlighted with `VietRed.copy(alpha = 0.08f)` card background

### Navigation Updates

- **Screen.kt**: `object Leaderboard : Screen("leaderboard")` added
- **BottomNavigation.kt**: `BottomNavItem.Leaderboard` with `Icons.Filled.EmojiEvents` added; items list: `[Home, Leaderboard, Performance, Profile, Settings]`
- **MainActivity.kt**: `composable(Screen.Leaderboard.route) { LeaderboardScreen() }` in NavHost; `Screen.Leaderboard.route` added to `showBottomNav` allowlist

## Deviations from Plan

### Auto-adjustments (within plan scope)

**1. [Rule 1 - Bug] rank_tier field absent from DB schema**
- **Found during:** Task 1 implementation
- **Issue:** Plan specified `@SerialName("rank_tier") val rankTier: String` but the `leaderboard` table has no `rank_tier` column (actual columns: user_id, username, elo_score, weekly_elo, rank, last_updated)
- **Fix:** Dropped `rankTier` from `LeaderboardEntry`; compute tier client-side via `EloRankUtils.getCurrentRank(eloScore)` + `getRankColor(eloScore)` in the UI. Added `rank: Int?` field to match the actual schema.
- **Files modified:** `LeaderboardRepository.kt`, `LeaderboardScreen.kt`

**2. [Rule 2 - Missing error handling] T-02-14 security note applied**
- **Found during:** Task 2 implementation
- **Issue:** Plan instructed passing `e.message` as error text; UiComponents.kt's security note T-02-14 prohibits raw exception messages in `ErrorStateComposable`
- **Fix:** Used generic user-facing string "Không thể tải bảng xếp hạng. Kiểm tra kết nối mạng và thử lại." instead of `e.message`
- **Files modified:** `LeaderboardViewModel.kt`

**3. Icon substitution (plan-approved fallback)**
- `Icons.Filled.Leaderboard` → `Icons.Filled.EmojiEvents` / `Icons.Outlined.EmojiEvents` per plan's explicit fallback instruction ("If Icons.Outlined.Leaderboard is not available... substitute with Icons.Outlined.EmojiEvents"). Both are confirmed in `material-icons-extended`.

## Threat Surface Scan

No new network endpoints or auth paths introduced beyond what the plan's threat model covers:
- `leaderboard` SELECT — documented as T-03-08 (accept, intentionally public)
- Realtime subscription — documented as T-03-09 (mitigate: single channel, onCleared removes it ✓)
- Client-side rank display — documented as T-03-10 (mitigate: server-side elo_score only, no client writes ✓)

## Known Stubs

None — all data flows from Supabase live queries. The own-rank footer shows "ngoài top 50" when the user is not in the fetched list; a future enhancement could fetch the exact server-side rank, but the current behaviour correctly satisfies LEAD-02.

## Self-Check

### Verification

- `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings in unrelated files)
- `LeaderboardViewModel.onCleared()` → `supabase.realtime.removeChannel(channel)` ✓
- `LeaderboardScreen`: `TabRow` with 2 tabs; `LazyColumn` with `key = { it.userId }`; own-rank footer logic ✓
- `MainActivity.kt`: `composable(Screen.Leaderboard.route)` in NavHost ✓
- `BottomNavigation.kt`: items list = `[Home, Leaderboard, Performance, Profile, Settings]` ✓
- `Screen.kt`: `object Leaderboard : Screen("leaderboard")` ✓

## Self-Check: PASSED

All 6 files present and committed at b8e8ac7. Build clean.
