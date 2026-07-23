---
phase: 02-05
fixed_at: 2026-07-23T20:45:00+07:00
review_path: .planning/02-05-REVIEW.md
iteration: 1
findings_in_scope: 11
fixed: 10
skipped: 1
status: partial
---

# Phase 02-05: Code Review Fix Report

**Fixed at:** 2026-07-23T20:45:00+07:00
**Source review:** `.planning/02-05-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 11 (5 Critical + 6 Warning)
- Fixed: 10
- Skipped: 1 (WA-03 — already resolved in existing code)

---

## Fixed Issues

### CR-01: UserProgressDto column names do not match the database schema

**Files modified:** `app/src/main/java/com/example/vietforces/data/model/UserProgressDto.kt`, `app/src/main/java/com/example/vietforces/data/repository/ProgressRepository.kt`
**Commit:** `a199134`
**Applied fix:**
- `UserProgressDto` rewritten with correct `@SerialName` values matching `001_initial_schema.sql`:
  - `"elo_rating"` → `"elo_score"` (field renamed `eloRating` → `eloScore`)
  - `"current_streak"` → `"streak_count"` (field renamed `currentStreak` → `streakCount`)
  - `"last_practiced"` → `"last_practice_date"` (field renamed, now nullable `String?`)
  - `"words_learned_count"` → `"total_games"` (no `words_learned_count` column in DB)
  - `"longest_streak"` removed (no corresponding DB column)
- `ProgressRepository.buildDtoFromLocal()` updated to use new field names
- `ProgressRepository.loadFromCloud()` updated to use `eloScore`, `streakCount`, `lastPracticeDate`

---

### CR-02: SECURITY DEFINER RPCs accept p_user_id but never verify it equals auth.uid()

**Files modified:** `supabase/migrations/002_elo_function.sql`, `supabase/migrations/003_streak_function.sql`, `supabase/migrations/006_daily_bonus_elo.sql`
**Commit:** `3f51a26`
**Applied fix:**
Added the following guard as the first statement in each `BEGIN` block:
```sql
IF p_user_id <> auth.uid() THEN
  RAISE EXCEPTION 'unauthorized'
    USING HINT = 'p_user_id must equal the calling user''s auth.uid()';
END IF;
```
Applied to `calculate_elo`, `update_streak`, and `award_daily_bonus`. Also patched in `009_security_fixes.sql` (WA-04 commit) as `CREATE OR REPLACE FUNCTION` to update already-deployed instances.

---

### CR-03: StreakDangerWorker crashes with IllegalStateException when WorkManager fires without prior MainActivity launch

**Files modified:** `app/src/main/java/com/example/vietforces/VietForcesApplication.kt`, `app/src/main/java/com/example/vietforces/MainActivity.kt`
**Commit:** `9b23648`
**Applied fix:**
- Added `PreferencesManager.init(this)` to `VietForcesApplication.onCreate()` (after `super.onCreate()`), with import added
- Removed the duplicate `PreferencesManager.init(this)` call from `MainActivity.onCreate()`; kept `SettingsManager.loadFromPreferences()` and other manager loads there (safe, UI-context-only ops)

---

### CR-04: VietForcesFirebaseMessagingService leaks a SupabaseClient on every FCM token refresh

**Files modified:** `app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt`
**Commit:** `e3a4e00`
**Applied fix:**
- Added `@AndroidEntryPoint` to the service class
- Added `@Inject lateinit var supabase: SupabaseClient` and `@Inject lateinit var authRepository: AuthRepository` fields
- Replaced the manual `createSupabaseClient(...)` block in `onNewToken()` with `FCMTokenManager.registerToken(userId, supabase)` using the injected client
- Replaced manual `client.auth.currentUserOrNull()?.id` lookup with `authRepository.currentUserId`
- Removed unused `io.github.jan.supabase.auth.auth` import and `BuildConfig` references
- Added required imports: `dagger.hilt.android.AndroidEntryPoint`, `javax.inject.Inject`

---

### CR-05: LeaderboardViewModel.onCleared() launches into a cancelled viewModelScope

**Files modified:** `app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt`
**Commit:** `e316aa9`
**Applied fix:**
- Added `private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` field
- Changed `onCleared()` to launch on `cleanupScope` instead of `viewModelScope`
- After `removeChannel()` completes, `cleanupScope.cancel()` is called to release the scope
- Added required imports: `CoroutineScope`, `Dispatchers`, `SupervisorJob`, `cancel`

---

### WA-01: FCMTokenManager.deleteToken() deletes all device tokens for a user

**Files modified:** `app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt`
**Commit:** `4fde57d`
**Applied fix:**
- `deleteToken()` now fetches the current FCM token via `FirebaseMessaging.getInstance().token.await()`
- If token fetch fails (Firebase not initialized), returns early without deleting anything
- Delete filter now includes both `eq("user_id", userId)` and `eq("token", currentToken)`, so only this device's registration is removed

---

### WA-02: Companion object instance vars are non-@Volatile

**Files modified:** `app/src/main/java/com/example/vietforces/data/repository/EloRepository.kt`, `app/src/main/java/com/example/vietforces/data/repository/StreakRepository.kt`, `app/src/main/java/com/example/vietforces/data/repository/ProgressRepository.kt`, `app/src/main/java/com/example/vietforces/data/repository/DailyChallengeRepository.kt`
**Commit:** `8a7c4aa`
**Applied fix:**
Changed `var instance: XxxRepository? = null` to `@Volatile var instance: XxxRepository? = null` in all four repository companion objects. Updated KDoc comment on each to explain the threading rationale.

---

### WA-04: RLS policies expose all columns with USING(TRUE)

**Files modified:** `supabase/migrations/009_security_fixes.sql` (new file)
**Commit:** `f8419c4`
**Applied fix:**
Created `supabase/migrations/009_security_fixes.sql` which:
1. Creates `public.users_public` view: `SELECT id, username FROM public.users`
2. Creates `public.user_progress_public` view: `SELECT user_id, elo_score, streak_count, total_games FROM public.user_progress`
3. Grants SELECT on both views to `authenticated` role
4. Also includes CR-02 `CREATE OR REPLACE FUNCTION` patches for `calculate_elo`, `update_streak`, and `award_daily_bonus` (with auth.uid() guard) to ensure already-deployed functions are updated
5. Also includes WA-06 patch for `award_daily_bonus` (CURRENT_DATE for streak credit)

---

### WA-05: send-streak-reminder edge function ignores per-user notification opt-out preferences

**Files modified:** `supabase/migrations/010_notif_preferences.sql` (new file), `supabase/functions/send-streak-reminder/index.ts`
**Commit:** `82f23e6`
**Applied fix:**
- Created migration `010_notif_preferences.sql` adding:
  - `notif_streak_enabled BOOLEAN NOT NULL DEFAULT TRUE` to `public.users`
  - `notif_daily_enabled BOOLEAN NOT NULL DEFAULT TRUE` to `public.users`
  - `users_update_notif_prefs` RLS UPDATE policy so users can update their own preferences
- Updated Edge Function query to add `.eq("notif_streak_enabled", true)` filter so users who opted out are excluded from streak reminders

---

### WA-06: award_daily_bonus passes p_challenge_date to update_streak, not today's server date

**Files modified:** `supabase/migrations/006_daily_bonus_elo.sql`
**Commit:** `b230140`
**Applied fix:**
Changed `PERFORM public.update_streak(p_user_id, p_challenge_date)` to `PERFORM public.update_streak(p_user_id, CURRENT_DATE)` in `006_daily_bonus_elo.sql`. The fix is also included in `009_security_fixes.sql` as a `CREATE OR REPLACE FUNCTION` patch for deployed instances.

---

## Skipped Issues

### WA-03: MainActivity.pendingNavigationDestination is static mutable state never cleared after consumption

**File:** `app/src/main/java/com/example/vietforces/MainActivity.kt`
**Reason:** Already resolved in existing code — code was correctly fixed before this review was applied.
**Original issue:** `pendingNavigationDestination` would be re-read on Activity recreation, triggering duplicate navigation. The `LaunchedEffect` in `VietforcesApp` already sets `MainActivity.pendingNavigationDestination = null` before calling `navController.navigate(destination)` (lines 514–519).

---

## Compile Status

Java Runtime is not available in this environment; `./gradlew :app:compileDebugKotlin` could not be run. All changes were verified by re-reading the modified files (Tier 1 verification). No structural or syntactic issues were observed. Full compile verification should be performed on a machine with a JDK installed.

---

_Fixed: 2026-07-23T20:45:00+07:00_
_Fixer: gsd-code-fixer agent_
_Iteration: 1_
