# Phase 3 Context: Streak + Real-time Leaderboard

## Goal
Deliver the two highest-retention mechanics: a server-authoritative streak system (with freeze/danger alerts) and a real-time leaderboard using Supabase Realtime. ELO is computed server-side via a PostgreSQL SECURITY DEFINER function.

## Requirements
STREAK-01, STREAK-02, STREAK-03, STREAK-04, ELO-01, ELO-02, LEAD-01, LEAD-02, LEAD-03, LEAD-04

## What Already Exists (from Phases 0–2)
- Supabase schema: `user_progress` (elo_rating, streak_days, last_practiced), `leaderboard` (user_id, elo, rank, username), `users`
- Realtime enabled on `leaderboard` table (Phase 1 migration)
- `SupabaseModule.kt` provides `SupabaseClient` with Realtime plugin
- `UserProgressManager.kt` stores local ELO + streak in SharedPreferences
- `ProgressRepository.kt` handles cloud sync (upsert to user_progress)
- `AuthRepository.kt` provides current user ID
- Existing Kotlin screens: MainScreen, ProfileScreen, GameScreen, PerformanceScreen
- Screen.kt has: Main, Login, Register, Onboarding routes

## Architecture Decisions

### ELO Server-Side (ELO-01)
- Client sends `game_metrics` (correct/incorrect counts, question count, time_ms) to a Supabase PostgreSQL function `calculate_elo(user_id, correct, total, time_ms)`
- The function is `SECURITY DEFINER` and reads/writes `user_progress.elo_rating` directly
- Client reads back new ELO from the function return value
- Client NEVER sends ELO delta; no client-side ELO mutation

### Streak Server-Authoritative (STREAK-01)
- `last_practiced` date stored in UTC; streak computed relative to user's last login timezone
- After each game: call `update_streak(user_id, today_utc_date)` PostgreSQL function
  - If `today - last_practiced == 1 day` → streak_days + 1
  - If `today == last_practiced` → no change (already played today)
  - If `today - last_practiced > 1` AND streak_freeze available → consume freeze, streak unchanged
  - Else → streak_days = 1 (reset)
- Return: { streak_days, streak_freeze_available, was_freeze_used }

### Streak Freeze (STREAK-03)
- `streak_freeze_count` column in `user_progress` (default 1, max 1 pending)
- Weekly cron (Supabase Edge Function `refresh-streak-freeze`) grants 1 freeze to all users with streak_freeze_count < 1
- Auto-applied by the `update_streak` function when streak would break

### Streak Danger Alert (STREAK-02)
- Android `WorkManager` periodic task every hour checks: current time + user's last_practiced
- If current time > 22:00 local AND last_practiced != today → show notification "⚠️ Streak sắp bị mất!"
- Uses existing NotificationManager.kt

### Streak Heatmap (STREAK-04)
- `streak_history` table: user_id, practiced_date (one row per day, upserted)
- Android: `StreakHeatmapComposable` — 7-col grid showing last 28 days (4 weeks), color-coded by activity
- Embedded in ProfileScreen below the streak counter

### Leaderboard Real-time (LEAD-01–04)
- `LeaderboardViewModel` subscribes to `supabase.realtime.channel("leaderboard")`
- Subscription listens for `INSERT/UPDATE` on `leaderboard` table
- Initial load: top 50 by elo DESC; user's own rank loaded separately
- Tab filter: "Tuần này" uses `week_elo` column; "All-time" uses `elo`
- `LeaderboardScreen.kt` — new screen with Tab row + LazyColumn
- Subscription cancelled in `onCleared()`

## New SQL Migrations Needed
- `002_elo_function.sql` — `calculate_elo()` SECURITY DEFINER function
- `003_streak_function.sql` — `update_streak()` function + `streak_history` table + `streak_freeze_count` column
- `004_leaderboard_week.sql` — add `week_elo` column to leaderboard + weekly reset Edge Function

## New Android Files
- `EloRepository.kt` — calls `calculate_elo` RPC
- `StreakRepository.kt` — calls `update_streak` RPC, reads streak_history
- `LeaderboardRepository.kt` — top 50 query + own rank + Realtime subscription
- `LeaderboardViewModel.kt` — StateFlow<LeaderboardUiState>, manages Realtime channel lifecycle
- `LeaderboardScreen.kt` — new full screen
- `StreakHeatmapComposable.kt` — 28-day heatmap in ProfileScreen
- `StreakDangerWorker.kt` — WorkManager periodic task
- `di/GameModule.kt` — Hilt module for EloRepository, StreakRepository, LeaderboardRepository

## Wave Structure
- Wave 1 (parallel): 03-01 (SQL migrations + ELO function) + 03-04 (LeaderboardScreen UI)
- Wave 2: 03-02 (StreakRepository + heatmap + danger worker)
- Wave 3: 03-03 (LeaderboardViewModel Realtime + wire into MainScreen nav)
