---
phase: 05-social-friends
plan: "02"
subsystem: social
tags: [social, search, leaderboard, friends, follow]
dependency_graph:
  requires:
    - 05-01 (PublicProfileViewModel, Screen.SearchUsers/PublicProfile, friendships RLS)
  provides:
    - SocialRepository (searchUsers, follow, unfollow, getFollowingIds, getFriendsLeaderboard)
    - SearchUsersScreen (user search with follow/unfollow)
    - LeaderboardTab.FRIENDS (friends leaderboard tab)
  affects:
    - LeaderboardRepository (new FRIENDS tab + SocialRepository injection)
    - LeaderboardViewModel (FRIENDS tab handling)
    - LeaderboardScreen (3rd tab UI)
    - MainActivity (SearchUsers route)
    - MainScreen (FindFriendsCard)
tech_stack:
  added:
    - SocialRepository (Hilt Singleton, SupabaseClient + AuthRepository)
    - SocialViewModel (HiltViewModel, debounced search, optimistic follow state)
  patterns:
    - Optimistic UI update with revert on failure (followingIds StateFlow)
    - 300ms debounced search via Job cancel + delay
    - Sealed class UI state (SearchUiState)
key_files:
  created:
    - app/src/main/java/com/example/vietforces/data/repository/SocialRepository.kt
    - app/src/main/java/com/example/vietforces/ui/viewmodel/SocialViewModel.kt
    - app/src/main/java/com/example/vietforces/ui/screens/SearchUsersScreen.kt
  modified:
    - app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt
    - app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt
    - app/src/main/java/com/example/vietforces/ui/screens/LeaderboardScreen.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
    - app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt
decisions:
  - SocialRepository queries leaderboard table (not users) for searchUsers — avoids join; username and elo_score coexist in leaderboard table
  - getFriendsLeaderboard uses filter { isIn("user_id", followingIds) } DSL — confirmed working in supabase-kt 3.7.0
  - streakDays defaults to 0 in UserSearchResult — not in leaderboard table; separate join not warranted for search results
  - getFriendsLeaderboard delegates via SocialRepository injected into LeaderboardRepository — avoids code duplication
  - FRIENDS tab skips my-entry pinned row in LeaderboardViewModel (no personal rank footer needed for friends list)
metrics:
  duration: ~15 minutes
  completed: "2026-07-22"
  tasks_completed: 8
  files_modified: 8
---

# Phase 05 Plan 02: SocialRepository + SearchUsersScreen + Friends Leaderboard Summary

**One-liner:** Social follow/unfollow system with username search, user cards, and friends leaderboard tab using Supabase postgrest filter DSL and optimistic StateFlow updates.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Create SocialRepository | 33cbfdd | SocialRepository.kt (new) |
| 2 | Create SocialViewModel | 33cbfdd | SocialViewModel.kt (new) |
| 3 | Create SearchUsersScreen | 33cbfdd | SearchUsersScreen.kt (new) |
| 4 | Update LeaderboardRepository | 33cbfdd | LeaderboardRepository.kt |
| 5 | Update LeaderboardViewModel | 33cbfdd | LeaderboardViewModel.kt |
| 6 | Update LeaderboardScreen | 33cbfdd | LeaderboardScreen.kt |
| 7 | Update MainActivity | 33cbfdd | MainActivity.kt |
| 8 | Update MainScreen | 33cbfdd | MainScreen.kt |

## What Was Built

### SocialRepository.kt
- `@Serializable data class UserSearchResult(userId, username, eloScore, streakDays=0)` — queried from leaderboard table
- `searchUsers(query)` — ILIKE on `username` column in `leaderboard` table, returns top 30 by elo_score DESC
- `followUser(userId)` — INSERT into `friendships` (RLS enforces follower_id = auth.uid())
- `unfollowUser(userId)` — DELETE from `friendships` with follower/following filter
- `isFollowing(userId)` — SELECT with limit 1, returns `result.isNotEmpty()`
- `getFollowingIds()` — SELECT following_id FROM friendships WHERE follower_id = me
- `getFriendsLeaderboard()` — get followingIds then query leaderboard with `filter { isIn("user_id", followingIds) }`

### SocialViewModel.kt
- `sealed class SearchUiState { Idle, Searching, Results, Empty, Error }`
- `searchState: StateFlow<SearchUiState>` — debounced 300ms via `Job.cancel() + delay`
- `followingIds: StateFlow<Set<String>>` — loaded on init, optimistically updated on toggleFollow
- `toggleFollow(userId)` — optimistic UI update, reverts on failure

### SearchUsersScreen.kt
- TopAppBar with back arrow
- `OutlinedTextField` search bar triggering `viewModel.search()`
- LazyColumn of `UserSearchCard`: 🎓 avatar + username + ELO tier chip + Follow/Unfollow button
- States: Idle (👥 prompt), Searching (shimmer), Results (list), Empty (🔍 message), Error (⚠️ message)

### LeaderboardRepository / ViewModel / Screen
- `LeaderboardTab.FRIENDS` enum added
- `getTop50(FRIENDS)` delegates to `getFriendsLeaderboard()`
- FRIENDS tab skips `getMyEntry()` (no own-rank footer)
- LeaderboardScreen: 3rd tab "Bạn bè 👥", friends-specific empty state with follow-prompt

### MainActivity + MainScreen
- `Screen.SearchUsers.route` composable added with `onUserClick → PublicProfile` navigation
- `onSearchFriendsClick` parameter added to `MainScreen`
- `FindFriendsCard` composable added to MainScreen's LazyColumn

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed filter(String) vs filter DSL incompatibility**
- **Found during:** Initial compile (Task 1 implementation)
- **Issue:** Used `filter(inFilter)` as raw string at outer select block level, but supabase-kt 3.7.0 `filter()` at that level takes a DSL lambda, not a String
- **Fix:** Changed to `filter { isIn("user_id", followingIds) }` DSL approach
- **Files modified:** SocialRepository.kt
- **Compile result:** BUILD SUCCESSFUL after fix

### Design Decisions

1. **searchUsers queries leaderboard not users table** — The `leaderboard` table contains username and elo_score, avoiding a join. This means only ranked users (who have completed at least one game) are searchable. Non-ranked users won't appear in search.

2. **streakDays = 0 in UserSearchResult** — The leaderboard table doesn't include streak data. Fetching streaks would require a separate `user_progress` query per result row. Since the plan said "or query separately and zip", we default to 0 for now.

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| `streakDays = 0` | SocialRepository.kt | leaderboard table doesn't include streak; future plan can add user_progress join if needed |

## Threat Flags

No new network endpoints or trust boundary changes beyond what plan's threat model covers.
Friendships INSERT/DELETE RLS is enforced server-side (T-05-03-01/02 from prior migration).
SearchUsers queries public leaderboard table (RLS allows authenticated SELECT per 007_activity_feed.sql).

## Self-Check

### Commits
- 33cbfdd: feat(social): add SocialRepository, SearchUsersScreen, friends leaderboard tab

### Files Created
- SocialRepository.kt: FOUND
- SocialViewModel.kt: FOUND
- SearchUsersScreen.kt: FOUND

### Build Status
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (warnings only, no errors)

## Self-Check: PASSED
