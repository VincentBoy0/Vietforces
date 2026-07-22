# Phase 5 Context: Social / Friends

## Goal
Deliver the social layer: asymmetric follow system, friends leaderboard tab, view other users' profiles, and a simple activity feed showing friends' daily completions and ELO milestones.

## Requirements
SOCIAL-01, SOCIAL-02, SOCIAL-03, SOCIAL-04

## What Already Exists
- `friendships` table: follower_id uuid, following_id uuid, created_at (Phase 1 schema)
- `leaderboard` table: user_id, username, elo_score, weekly_elo, rank (Phase 1 + Phase 3)
- `LeaderboardScreen.kt` has TabRow with "All-time" and "Tuần này" — needs a 3rd "Bạn bè" tab
- `LeaderboardViewModel.kt` + `LeaderboardRepository.kt` exist
- `ProfileScreen.kt` shows own profile; needs to also handle other users' profiles
- `AuthRepository.kt` provides current user ID
- `SupabaseClient` with Postgrest + Realtime available
- `Screen.kt` has existing routes
- `UiComponents.kt` has EmptyStateComposable, ShimmerBox, ErrorStateComposable

## Architecture Decisions

### Follow System (SOCIAL-01)
- Asymmetric follow: follower_id follows following_id (already modeled in `friendships` table)
- Search: query `users` table by username ILIKE '%query%' (Supabase full-text or ILIKE)
- New `SocialRepository.kt`: searchUsers(query), followUser(userId), unfollowUser(userId), getFollowing(), getFollowers()
- New `SearchScreen.kt` or bottom sheet: search bar + user list + follow/unfollow button
- RLS policy needed: users can SELECT all usernames (public discovery), INSERT/DELETE own friendship rows

### Friends Leaderboard (SOCIAL-02)
- Add 3rd tab "Bạn bè 👥" to LeaderboardScreen
- `LeaderboardRepository.getFriendsLeaderboard()`: JOIN leaderboard ON friendships WHERE follower_id = current_user ORDER BY elo_score DESC
- Only users the current user follows appear (asymmetric)

### Public Profiles (SOCIAL-03)
- `ProfileScreen` accepts optional `userId: String?` parameter
- If userId == null (or currentUser): show own profile (existing behavior)
- If userId != null: show read-only view — username, ELO, streak, games played, rank badge
- Navigation: `Screen.PublicProfile("profile/{userId}")` with argument
- Follow/unfollow button on public profile

### Activity Feed (SOCIAL-04)
- New `ActivityFeedScreen.kt` accessible from MainScreen or bottom nav
- `activity_feed` table OR derived from existing tables:
  * daily_completions JOIN users WHERE user_id IN (following list) — shows "X hoàn thành thách đấu hôm nay"
  * Could also track ELO milestones: when calculate_elo() pushes past tier boundary → insert into `activity_events`
- For MVP: query daily_completions for followed users (last 7 days) + leaderboard changes as simple list
- New SQL migration: `007_activity_feed.sql` — `activity_events` table (user_id, event_type, metadata jsonb, created_at)
- New Edge Function or Postgres trigger: INSERT into activity_events on daily_completion

## New Files
### Android
- `SocialRepository.kt` — search, follow/unfollow, get friends list, activity feed
- `SocialViewModel.kt` — search state + follow actions
- `SearchUsersScreen.kt` — search bar + user card list
- `PublicProfileScreen.kt` — read-only profile view with follow button
- Updated `LeaderboardRepository.kt` + `LeaderboardViewModel.kt` — add friends tab
- Updated `LeaderboardScreen.kt` — 3rd tab
- `ActivityFeedScreen.kt` — simple feed list
- Updated `Screen.kt`, `MainActivity.kt`, `BottomNavigation.kt` or `MainScreen.kt`

### Supabase
- `supabase/migrations/007_activity_feed.sql` — activity_events table + trigger
- RLS updates: allow public username search

## Wave Structure
- Wave 1 (parallel): 05-01 (SQL migrations + RLS) + 05-03 (PublicProfileScreen — minimal deps)
- Wave 2: 05-02 (SocialRepository + SearchUsersScreen + friends leaderboard tab)
- Wave 3: 05-04 (ActivityFeedScreen)
