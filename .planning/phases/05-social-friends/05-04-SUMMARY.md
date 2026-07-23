---
phase: 05-social-friends
plan: "04"
subsystem: social-feed
tags: [social, activity-feed, hilt, supabase, compose, rls]
dependency_graph:
  requires:
    - 05-01  # activity_events table + RLS policy
    - 05-02  # SearchUsersScreen, friendships follow graph
    - 05-03  # PublicProfileScreen (feed tap target)
  provides:
    - ActivityFeedScreen
    - ActivityFeedViewModel
    - Screen.ActivityFeed
  affects:
    - MainActivity.kt
    - MainScreen.kt
    - Screen.kt
tech_stack:
  added:
    - kotlinx.serialization.json.JsonObject (metadata JSONB deserialization)
  patterns:
    - Two-query batch fetch for usernames (avoids embedded FK select ambiguity)
    - RLS auto-scoping (no client-side follow filter needed)
    - StateFlow + collectAsStateWithLifecycle for UI state
key_files:
  created:
    - app/src/main/java/com/example/vietforces/ui/viewmodel/ActivityFeedViewModel.kt
    - app/src/main/java/com/example/vietforces/ui/screens/ActivityFeedScreen.kt
  modified:
    - app/src/main/java/com/example/vietforces/navigation/Screen.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
    - app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt
decisions:
  - "Used two-query approach (activity_events + leaderboard batch) instead of embedded PostgREST join to avoid FK reference ambiguity between auth.users and public.users"
  - "ActivityEventItem uses Long for id field (Supabase bigserial default) rather than String for safe deserialization"
  - "RLS policy activity_events_select_following handles all friend-scoping server-side — no client filter needed"
metrics:
  duration: "~15 minutes"
  completed: "2026-07-23"
  tasks_completed: 3
  files_changed: 5
---

# Phase 05 Plan 04: ActivityFeedScreen + ViewModel Summary

**One-liner:** Friends' activity feed reading `activity_events` via Supabase RLS with shimmer/empty/error/success states and Vietnamese relative timestamps.

## What Was Built

### Task 1: ActivityFeedViewModel.kt (new)

- **`ActivityEventItem`** — `@Serializable` data class mapping `activity_events` rows: `id: Long`, `userId`, `eventType`, `metadata: JsonObject`, `createdAt`
- **`ActivityFeedUiState`** — holds `isLoading`, `events`, `usernameMap: Map<String, String>`, `error`
- **`ActivityFeedViewModel`** — `@HiltViewModel` injecting `SupabaseClient` + `AuthRepository`
  - `loadFeed()`: queries `activity_events` filtered to last 7 days, ordered DESC, limit 50; RLS `activity_events_select_following` auto-scopes to followed users
  - Batch-fetches usernames from `leaderboard` by `user_id IN (...)` (avoids embedded FK select ambiguity)
  - `retry()` convenience method for error state button

### Task 2: ActivityFeedScreen.kt (new)

- **`ActivityFeedScreen`** — `@Composable` with `onBackClick`, `onNavigateToProfile`, `hiltViewModel()`
  - TopAppBar: "Hoạt động bạn bè 📰" with back arrow + refresh icon button
  - **Loading**: 4 `ShimmerBox` placeholders (80dp height, 12dp rounded corners)
  - **Error**: `ErrorStateComposable` with retry calling `viewModel.retry()`
  - **Empty**: `EmptyStateComposable` with "📭", Vietnamese message, CTA "Tìm bạn bè" → `onBackClick`
  - **Success**: `LazyColumn` keyed by `event.id` of `ActivityEventCard` items

- **`ActivityEventCard`** — private composable:
  - Event emoji circle (44dp, VietRed 10% alpha): 📅 daily_completion, ⚡ elo_milestone, 🎯 default
  - Username (bold, 14sp) + action description (13sp, onSurfaceVariant)
  - `daily_completion`: "đã hoàn thành thách đấu ngày {date} (+{elo} ELO 🌟)" from metadata JSONB
  - `elo_milestone`: "vừa đạt {milestone_elo} ELO! ⚡"
  - Relative timestamp: `formatRelativeTime()` → "vừa xong" / "X phút trước" / "X giờ trước" / "X ngày trước"
  - Tapping calls `onNavigateToProfile(event.userId)` → `PublicProfileScreen`

- **`formatRelativeTime()`** — private function using `java.time.Instant.parse()` + `ChronoUnit.SECONDS.between()`, wrapped in try/catch (T-05-04-04)

### Task 3: Navigation wiring

- **Screen.kt**: Added `object ActivityFeed : Screen("social/feed")`
- **MainActivity.kt**: Added `composable(Screen.ActivityFeed.route)` with `ActivityFeedScreen`; added `onActivityFeedClick` to `MainScreen(...)` call
- **MainScreen.kt**: Added `onActivityFeedClick: () -> Unit = {}` parameter; added `Icons.Default.DynamicFeed` icon in TopAppBar actions; added `ActivityFeedCard` composable in LazyColumn after `FindFriendsCard`

## Deviations from Plan

### Auto-applied Decisions (within plan flexibility)

**1. [Rule 2 - Design Choice] Two-query approach instead of embedded PostgREST join**
- **Planned:** `select("*, users(id, username)")` embedded join on `activity_events.user_id → users`
- **Applied:** Two separate queries: `activity_events` then batch `leaderboard` by `user_id IN (...)`
- **Reason:** `activity_events.user_id` references `auth.users(id)`, not `public.users(id)`. The PostgREST embedded join syntax `users(id, username)` requires an FK to a table in the public schema with a `username` column. The existing codebase shows username lookups via `leaderboard` table (see SocialRepository), not a `public.users` table. The two-query approach is robust and matches existing patterns.
- **Impact:** `ActivityFeedUiState` has `usernameMap: Map<String, String>` instead of embedded `user` field in `ActivityEventItem`. Screen accesses `uiState.usernameMap[event.userId] ?: "Người dùng"`.

**2. [Rule 2 - Type Safety] `id: Long` instead of `id: String`**
- **Planned:** `@SerialName("id") val id: String`
- **Applied:** `@SerialName("id") val id: Long`
- **Reason:** Supabase default `id` column is `bigserial` (integer), not UUID. Mapping a numeric DB value to `String` would cause kotlinx.serialization failure at runtime. `Long` deserializes correctly from Supabase JSON integers.

## Threat Model Compliance

| Threat ID | Status |
|-----------|--------|
| T-05-04-01 | ✅ RLS `activity_events_select_following` enforced server-side — no client filter needed |
| T-05-04-02 | ✅ metadata only exposes challenge_date (date) + elo_earned (integer) — no PII |
| T-05-04-03 | ✅ `limit(50L)` + `gte("created_at", sevenDaysAgo)` applied in query |
| T-05-04-04 | ✅ `formatRelativeTime()` wrapped in try/catch, returns `""` on parse failure |
| T-05-04-05 | ✅ Only `user_id` + `username` fetched from leaderboard (consistent with public exposure) |

## Known Stubs

None — all data flows from Supabase. The empty state ("Chưa có hoạt động nào") correctly reflects the case when the user follows nobody or no friends have completed daily challenges in the last 7 days.

## Threat Flags

None — no new network endpoints or auth paths introduced. ActivityFeedScreen reads from existing `activity_events` and `leaderboard` tables with existing RLS.

## Self-Check: PASSED

- ✅ `ActivityFeedViewModel.kt` exists at `app/src/main/java/com/example/vietforces/ui/viewmodel/`
- ✅ `ActivityFeedScreen.kt` exists at `app/src/main/java/com/example/vietforces/ui/screens/`
- ✅ `Screen.kt` contains `object ActivityFeed : Screen("social/feed")`
- ✅ `MainActivity.kt` has `composable(Screen.ActivityFeed.route)`
- ✅ `MainScreen.kt` has `onActivityFeedClick` parameter
- ✅ Commit `11d82b9` exists in git log
- ✅ `compileDebugKotlin` → `BUILD SUCCESSFUL in 29s`
