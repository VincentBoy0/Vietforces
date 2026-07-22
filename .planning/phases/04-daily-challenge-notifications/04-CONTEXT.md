# Phase 4 Context: Daily Challenge + Push Notifications

## Goal
Deliver the daily habit loop: a server-generated daily challenge with countdown, bonus ELO + streak credit on completion, 7-day history view, plus FCM push notifications for re-engagement (streak danger, daily challenge reminder).

## Requirements
DAILY-01, DAILY-02, DAILY-03, DAILY-04, NOTIF-01, NOTIF-02, NOTIF-03, NOTIF-04, NOTIF-05

## What Already Exists
- `daily_challenges` table: id, challenge_date date, word_ids jsonb, bonus_elo int4, created_at (Phase 1 schema)
- `fcm_tokens` table: user_id, token, updated_at (Phase 1 schema)
- `NotificationManager.kt` — handles local notifications, has channel setup
- `StreakRepository.kt` — `updateStreak()` RPC exists
- `EloRepository.kt` — `submitGameResult()` exists
- `calculate_elo()` SQL function exists
- WorkManager 2.9.1 added (Phase 3)
- Android 13 POST_NOTIFICATIONS permission pattern needed
- Existing game screens: ImageToWordScreen, etc.

## Architecture Decisions

### Daily Challenge Generation (DAILY-01)
- Supabase Edge Function `generate-daily-challenge/index.ts`
- Scheduled via pg_cron: `0 0 * * *` (00:00 UTC daily)
- Selects 10 random word_ids from `words` table → inserts into `daily_challenges`
- Android queries today's challenge by date: `eq("challenge_date", today)`

### Daily Challenge Screen (DAILY-02, DAILY-03, DAILY-04)
- New `DailyChallengeScreen.kt` — accessible from MainScreen card + bottom nav
- Countdown to midnight UTC using `LaunchedEffect` + `delay(1000L)` loop
- On completion: call `EloRepository.instance.submitGameResult()` + bonus +50 ELO RPC + `StreakRepository.instance.updateStreak()`
- 7-day history: query `daily_challenges` joined with `daily_completions` table

### FCM Setup (NOTIF-01)
- Add `com.google.firebase:firebase-messaging-ktx` to dependencies
- `VietForcesFirebaseMessagingService` extends `FirebaseMessagingService`
- On new token: upsert to Supabase `fcm_tokens` table
- On login: call `FCMTokenManager.registerToken(userId)`

### Push Notifications via Edge Function (NOTIF-02, NOTIF-03)
- Edge Function `send-streak-reminder/index.ts`
- Scheduled ~19:00 UTC: queries users who haven't practiced today + have FCM token
- Sends FCM via Google FCM HTTP v1 API with `data.screen = "daily_challenge"` for deep link
- Android: `MyFirebaseMessagingService.onMessageReceived()` reads `data.screen`, creates intent to `DailyChallengeScreen`

### Notification Settings (NOTIF-04, NOTIF-05)
- `PreferencesManager` gains: `notif_streak_enabled`, `notif_daily_enabled` booleans
- SettingsScreen gets a "Thông báo" section with 2 toggles
- Android 13+: request `POST_NOTIFICATIONS` in MainActivity on first launch

## New SQL Migrations
- `005_daily_completions.sql` — `daily_completions` table (user_id, challenge_date, elo_earned, completed_at)
- `006_daily_bonus_elo.sql` — `award_daily_bonus(p_user_id, p_challenge_date)` SECURITY DEFINER function: checks not already completed, awards +50 ELO via calculate_elo logic, inserts completion row

## New Files
### Android
- `DailyChallengeRepository.kt` — fetch today's challenge, submit completion, get 7-day history
- `DailyChallengeViewModel.kt` — StateFlow<DailyChallengeUiState>, countdown timer
- `DailyChallengeScreen.kt` — full screen with countdown, challenge words, completion state
- `FCMTokenManager.kt` — registers/refreshes FCM token to Supabase
- `VietForcesFirebaseMessagingService.kt` — handles FCM token refresh + message received
- Updated `SettingsScreen.kt` — notification toggles
- Updated `VietForcesApplication.kt` — request POST_NOTIFICATIONS

### Supabase
- `supabase/migrations/005_daily_completions.sql`
- `supabase/migrations/006_daily_bonus_elo.sql`
- `supabase/functions/generate-daily-challenge/index.ts`
- `supabase/functions/send-streak-reminder/index.ts`

## Wave Structure
- Wave 1 (parallel): 04-01 (SQL + Edge Functions) + 04-03 (FCM setup + notification settings)
- Wave 2: 04-02 (DailyChallengeRepository + ViewModel + Screen — needs 04-01 SQL)
