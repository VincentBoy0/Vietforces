---
phase: 05-social-friends
plan: "03"
subsystem: social-profile
tags: [social, profile, navigation, compose, hilt, supabase]
dependency_graph:
  requires: []
  provides:
    - PublicProfileViewModel (loadProfile, toggleFollow)
    - PublicProfileScreen composable
    - Screen.PublicProfile.createRoute(userId)
    - Screen.SearchUsers (stub for Wave 2)
    - MainActivity NavHost route: social/profile/{userId}
  affects:
    - app/src/main/java/com/example/vietforces/navigation/Screen.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
tech_stack:
  added: []
  patterns:
    - Parallel coroutines with async/await for multi-table Supabase fetch
    - Optimistic UI update pattern for follow/unfollow
    - LaunchedEffect(userId) key to prevent spurious reloads (T-05-03-05)
    - EloRankUtils.getCurrentRank for tier badge color/name
    - ShimmerBox/ErrorStateComposable/EmptyStateComposable for loading/error/empty states
key_files:
  created:
    - app/src/main/java/com/example/vietforces/ui/viewmodel/PublicProfileViewModel.kt
    - app/src/main/java/com/example/vietforces/ui/screens/PublicProfileScreen.kt
  modified:
    - app/src/main/java/com/example/vietforces/navigation/Screen.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
decisions:
  - "Used UserProgressPublic local data class (streak_count, total_games) instead of UserProgressDto to select only public, non-PII columns from user_progress"
  - "rankTier stored as raw ELO string in PublicProfileData; rank name computed at render time via EloRankUtils.getCurrentRank in PublicProfileScreen"
  - "FriendshipCheck and FriendshipInsert kept as private data classes inside ViewModel to limit scope"
  - "select('follower_id') API not available in supabase-kt — used select { } with full row + FriendshipCheck decoder"
metrics:
  duration: "~25 minutes"
  completed: "2026-07-22T19:44:14Z"
  tasks_completed: 3
  files_created: 2
  files_modified: 2
---

# Phase 5 Plan 03: PublicProfileScreen + Navigation Wiring Summary

**One-liner:** Read-only public profile with ELO rank badge, streak/games stats, optimistic follow button wired to Supabase friendships via NavHost route `social/profile/{userId}`.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Create PublicProfileViewModel.kt | 913a460 | ui/viewmodel/PublicProfileViewModel.kt |
| 2 | Create PublicProfileScreen.kt | 913a460 | ui/screens/PublicProfileScreen.kt |
| 3 | Add Screen.PublicProfile + wire MainActivity route | 913a460 | navigation/Screen.kt, MainActivity.kt |

## What Was Built

### PublicProfileViewModel.kt

- **`PublicProfileUiState`**: `isLoading`, `profile: PublicProfileData?`, `isFollowing`, `isCurrentUser`, `error`
- **`PublicProfileData`**: `userId`, `username`, `eloScore`, `rankTier`, `streakCount`, `totalGames`
- **`UserProgressPublic`**: `@Serializable` data class mapping `streak_count`/`total_games` from `user_progress` table
- **`loadProfile(targetUserId)`**: Fires two `async` coroutines in `coroutineScope` — one fetches `leaderboard` row, one fetches `user_progress` row. Then checks follow status (unless own profile). All wrapped in try/catch.
- **`toggleFollow()`**: Optimistic update → INSERT or DELETE `friendships` row → revert on error. RLS enforces `follower_id = auth.uid()` server-side.
- **`checkFollowStatus()`**: SELECT from `friendships` with limit(1) to check existing follow row.

### PublicProfileScreen.kt

- **Loading**: 3× `ShimmerBox` placeholders (80dp height, RoundedCornerShape(12dp))
- **Error**: `ErrorStateComposable` with retry callback `viewModel.loadProfile(userId)`
- **Empty**: `EmptyStateComposable(illustration="👤", ...)`
- **Profile content**:
  - Circular avatar (VietYellow background, first-letter initial, 80dp)
  - Username (20sp bold)
  - ELO rank badge (Surface with tier color at 15% alpha + Vietnamese rank name from `EloRankUtils`)
  - Stats row: ⚡ ELO | 🔥 Streak days | 🎮 Games — via private `StatItem` composable
  - Follow/Unfollow `Button`/`OutlinedButton` (hidden when `isCurrentUser=true`)
  - Inline error text for follow/unfollow failures
- **TopAppBar**: AutoMirrored ArrowBack → `onBackClick()`

### Screen.kt

Added two sealed objects:
```kotlin
object SearchUsers : Screen("social/search")

object PublicProfile : Screen("social/profile/{userId}") {
    fun createRoute(userId: String): String = "social/profile/$userId"
}
```

### MainActivity.kt

Added `NavType`/`navArgument` imports and composable route:
```kotlin
composable(
    route = Screen.PublicProfile.route,
    arguments = listOf(navArgument("userId") { type = NavType.StringType })
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
    PublicProfileScreen(userId = userId, onBackClick = { navController.popBackStack() })
}
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed `select("follower_id")` API mismatch**
- **Found during:** Task 1 — first compile attempt
- **Issue:** `supabase.from(...).select("follower_id") { ... }` — the supabase-kt `from().select()` does not accept a plain `String` as the columns parameter; it expects the overload-less `select { }` block.
- **Fix:** Changed `checkFollowStatus` to use `select { filter { ... }; limit(1L) }` with full row decode via `FriendshipCheck` data class (which only maps `follower_id`). This is consistent with the pattern used throughout the codebase (LeaderboardRepository, etc.).
- **Files modified:** `PublicProfileViewModel.kt`
- **Commit:** 913a460

## Known Stubs

| Stub | File | Line | Reason |
|------|------|------|--------|
| `Screen.SearchUsers` (no composable route) | MainActivity.kt | — | Wave 2 plan 05-02 will add `SearchUsersScreen` and wire the route; stub exists only in Screen.kt |

## Threat Model Review

All T-05-03-xx threats addressed:
- **T-05-03-01/02** (Spoofing/Tampering on friendships): RLS on `friendships` enforces `follower_id = auth.uid()` — server-side, not client-enforced. ViewModel does not pass `follower_id` as a user-provided parameter.
- **T-05-03-03/04** (Information Disclosure): Accepted — streak/games/ELO are public gamification stats, no PII.
- **T-05-03-05** (DoS via loadProfile recompose): Mitigated — `LaunchedEffect(userId)` key ensures load only fires on userId change.

## Self-Check: PASSED

- ✅ `app/src/main/java/com/example/vietforces/ui/viewmodel/PublicProfileViewModel.kt` — exists
- ✅ `app/src/main/java/com/example/vietforces/ui/screens/PublicProfileScreen.kt` — exists
- ✅ `app/src/main/java/com/example/vietforces/navigation/Screen.kt` — contains `SearchUsers` and `PublicProfile`
- ✅ `app/src/main/java/com/example/vietforces/MainActivity.kt` — contains `Screen.PublicProfile.route` composable
- ✅ Commit `913a460` exists: `feat(social): add PublicProfileScreen with ELO rank, streak stats, follow button placeholder (SOCIAL-03)`
- ✅ `compileDebugKotlin` → **BUILD SUCCESSFUL**
