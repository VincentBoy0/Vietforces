---
phase: 02-05
reviewed: 2026-07-23T13:40:00Z
depth: standard
files_reviewed: 52
files_reviewed_list:
  - app/src/main/java/com/example/vietforces/MainActivity.kt
  - app/src/main/java/com/example/vietforces/VietForcesApplication.kt
  - app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt
  - app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt
  - app/src/main/java/com/example/vietforces/data/model/UserProgressDto.kt
  - app/src/main/java/com/example/vietforces/data/remote/RemoteProgressSource.kt
  - app/src/main/java/com/example/vietforces/data/repository/AuthRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/DailyChallengeRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/EloRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/ProgressRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/SocialRepository.kt
  - app/src/main/java/com/example/vietforces/data/repository/StreakRepository.kt
  - app/src/main/java/com/example/vietforces/data/service/MigrationService.kt
  - app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt
  - app/src/main/java/com/example/vietforces/data/worker/StreakDangerWorker.kt
  - app/src/main/java/com/example/vietforces/di/AuthModule.kt
  - app/src/main/java/com/example/vietforces/di/GameModule.kt
  - app/src/main/java/com/example/vietforces/di/RepositoryModule.kt
  - app/src/main/java/com/example/vietforces/navigation/Screen.kt
  - app/src/main/java/com/example/vietforces/ui/components/BottomNavigation.kt
  - app/src/main/java/com/example/vietforces/ui/components/StreakHeatmapComposable.kt
  - app/src/main/java/com/example/vietforces/ui/components/UiComponents.kt
  - app/src/main/java/com/example/vietforces/ui/screens/ActivityFeedScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/DailyChallengeScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/LeaderboardScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/LoginScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/OnboardingScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/PublicProfileScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/RegisterScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/SearchUsersScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/SettingsScreen.kt
  - app/src/main/java/com/example/vietforces/ui/screens/game/ImageToWordScreen.kt
  - app/src/main/java/com/example/vietforces/ui/theme/Color.kt
  - app/src/main/java/com/example/vietforces/ui/theme/Theme.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/ActivityFeedViewModel.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/AuthViewModel.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/DailyChallengeViewModel.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/PublicProfileViewModel.kt
  - app/src/main/java/com/example/vietforces/ui/viewmodel/SocialViewModel.kt
  - supabase/migrations/002_elo_function.sql
  - supabase/migrations/003_streak_function.sql
  - supabase/migrations/004_leaderboard_week.sql
  - supabase/migrations/005_daily_completions.sql
  - supabase/migrations/006_daily_bonus_elo.sql
  - supabase/migrations/007_activity_feed.sql
  - supabase/functions/generate-daily-challenge/index.ts
  - supabase/functions/refresh-streak-freeze/index.ts
  - supabase/functions/send-streak-reminder/index.ts
findings:
  critical: 5
  warning: 6
  info: 5
  total: 16
status: issues_found
---

# Code Review — Phases 2–5

**Scope:** Android Kotlin (43 files) + SQL migrations (6) + Edge Functions (3)
**Depth:** Standard
**Date:** 2026-07-23

## Summary

- 🔴 Critical: 5
- 🟡 Warning: 6
- 🔵 Info: 5

Phases 2–5 introduce ELO, streak, daily challenge, leaderboard, and social features on top of a Supabase backend. The architecture is well-structured — Hilt DI, typed repositories, sealed UI state, and server-side ELO/streak computation are all sound choices. However, five critical defects make the app currently broken or exploitable in production:

1. **The entire cloud sync pipeline is silently broken** (`UserProgressDto` column names don't match the actual `user_progress` table — every `syncToCloud()` call fails with a PostgREST 400 that is swallowed).
2. **Three SECURITY DEFINER RPCs accept a `p_user_id` parameter but never verify it equals `auth.uid()`**, allowing any authenticated user to boost another user's ELO, credit their streak, or claim daily bonuses on their behalf.
3. **`StreakDangerWorker` will crash** at runtime when WorkManager fires the background job before `MainActivity` has ever run, because `PreferencesManager` is initialized in `MainActivity.onCreate()` rather than `Application.onCreate()`.
4. **`VietForcesFirebaseMessagingService.onNewToken`** creates a brand-new `SupabaseClient` instance on every token rotation without ever disposing it — a coroutine/connection resource leak.
5. **`LeaderboardViewModel.onCleared()`** launches a cleanup coroutine inside an already-cancelled `viewModelScope`, so the Realtime WebSocket channel is never actually unsubscribed.

---

## Critical Findings

### [CR-01] UserProgressDto column names do not match the database schema — all cloud sync silently fails

**File:** `app/src/main/java/com/example/vietforces/data/model/UserProgressDto.kt` (line 13–18) · `app/src/main/java/com/example/vietforces/data/remote/RemoteProgressSource.kt` (line 21–24)

**Issue:** `UserProgressDto` uses `@SerialName` names that do not exist in the `user_progress` table:

| DTO `@SerialName` | Actual DB column (`001_initial_schema.sql`) | Status |
|---|---|---|
| `elo_rating` | `elo_score` | **wrong name** |
| `current_streak` | `streak_count` | **wrong name** |
| `longest_streak` | *(no column)* | **column absent** |
| `words_learned_count` | *(no column — table has `words_learned` JSONB)* | **column absent** |
| `last_practiced` | `last_practice_date` | **wrong name** |

PostgREST rejects writes with unknown column names (HTTP 400 "column X does not exist"). `RemoteProgressSource.upsertProgress()` wraps the error in `Result.failure(e)`, and all callers either ignore the failure with `runCatching { syncToCloud() }` or silently discard a `Result.failure`. The result: **every SYNC-01 and SYNC-02 operation has been silently failing since Phase 3 was merged**.

**Risk:** User ELO, streak, and learned-word count are **never persisted to Supabase**. Data is lost on app reinstall. The `loadFromCloud()` path is equally broken — it decodes all zeros because no column names match.

**Fix:** Align `UserProgressDto` with the actual schema:
```kotlin
@Serializable
data class UserProgressDto(
    @SerialName("user_id")          val userId: String,
    @SerialName("elo_score")        val eloScore: Int,          // was "elo_rating"
    @SerialName("streak_count")     val streakCount: Int,       // was "current_streak"
    @SerialName("last_practice_date") val lastPracticeDate: String?, // was "last_practiced"
    @SerialName("total_games")      val totalGames: Int = 0,    // replaces words_learned_count
    @SerialName("updated_at")       val updatedAt: String = ""
)
```
Then update `ProgressRepository.buildDtoFromLocal()` and `loadFromCloud()` to use the renamed fields. The `longest_streak` field has no corresponding DB column; it should either be added to the schema or dropped from the DTO.

---

### [CR-02] SECURITY DEFINER RPCs accept `p_user_id` but never verify it equals `auth.uid()` — ELO/streak injection

**Files:**
- `supabase/migrations/002_elo_function.sql` (line 48–70) — `calculate_elo`
- `supabase/migrations/003_streak_function.sql` (line 43–62) — `update_streak`
- `supabase/migrations/006_daily_bonus_elo.sql` (line 29–47) — `award_daily_bonus`

**Issue:** All three functions are `SECURITY DEFINER` (bypass RLS) and take a `p_user_id UUID` argument, but none of them check that `p_user_id = auth.uid()`. Any authenticated user can call the RPC with any other user's UUID and:
- `calculate_elo(other_user_uuid, 10, 10, 100)` → silently inflates that user's ELO to 3000.
- `update_streak(other_user_uuid, '2030-01-01')` → credits a future date to their streak.
- `award_daily_bonus(other_user_uuid, '2026-07-23')` → steals that user's +50 daily ELO.

The code comment in `006_daily_bonus_elo.sql` acknowledges the concern ("client cannot exceed the caller's own identity") but the mitigation is described as being enforced by the SDK — which is not a server-side guarantee.

**Risk:** Full ELO/streak manipulation by any authenticated user. Leaderboard integrity completely compromised.

**Fix:** Add a caller-identity check at the top of each function:
```sql
-- In calculate_elo, update_streak, award_daily_bonus — first lines of the BEGIN block:
IF p_user_id <> auth.uid() THEN
  RAISE EXCEPTION 'unauthorized'
    USING HINT = 'p_user_id must equal the calling user''s auth.uid()';
END IF;
```

---

### [CR-03] `StreakDangerWorker` crashes with `IllegalStateException` when WorkManager fires without prior `MainActivity` launch

**Files:** `app/src/main/java/com/example/vietforces/data/worker/StreakDangerWorker.kt` (line 39, 50) · `app/src/main/java/com/example/vietforces/VietForcesApplication.kt` · `app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt` (line 71)

**Issue:** `PreferencesManager.init(context)` is only called inside `MainActivity.onCreate()`. When Android reboots the device (or WorkManager brings up the app process in the background for the scheduled hourly check), `VietForcesApplication.onCreate()` runs but `MainActivity.onCreate()` does **not**. `StreakDangerWorker.doWork()` then calls `PreferencesManager.getIsGuest()` on line 39, which internally calls `getPrefs()`, which throws:
```
IllegalStateException: PreferencesManager not initialized. Call init(context) first.
```
The worker will crash repeatedly, exhausting WorkManager retry backoff and eventually being permanently stopped.

**Risk:** App process crash on background wakeup. Streak danger notifications never fire after device reboot.

**Fix:** Move `PreferencesManager.init(this)` (and optionally the other `loadFromPreferences()` calls that are safe without a UI) to `VietForcesApplication.onCreate()`:
```kotlin
// VietForcesApplication.kt
override fun onCreate() {
    super.onCreate()
    PreferencesManager.init(this)   // ← move here from MainActivity
    // Worker only needs PreferencesManager, not the other managers
    scheduleStreakWorker()
}
```

---

### [CR-04] `VietForcesFirebaseMessagingService` leaks a `SupabaseClient` on every FCM token refresh

**File:** `app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt` (line 55–68)

**Issue:** `onNewToken()` calls `io.github.jan.supabase.createSupabaseClient(...)` inside a `serviceScope` coroutine. The created client initialises HTTP connection pools, Realtime WebSocket infrastructure, and coroutine supervisors — but `close()` is never called on it. Each token rotation (which happens on app reinstall, token expiry, and Firebase project migrations) leaks one `SupabaseClient` instance.

Additionally, this code duplicates Hilt-injected infrastructure. The app already has a `SupabaseClient` singleton provided by Hilt; the service should use that instead.

**Risk:** Memory/connection leak accumulating with each token rotation. In extreme cases (frequent reinstalls in testing), can exhaust file descriptors.

**Fix:** Inject the Hilt-provided `SupabaseClient` into the service and delegate to `FCMTokenManager`:
```kotlin
@AndroidEntryPoint
class VietForcesFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var supabase: SupabaseClient
    @Inject lateinit var authRepository: AuthRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = authRepository.currentUserId ?: return
        serviceScope.launch {
            FCMTokenManager.registerToken(userId, supabase)
        }
    }
    // ...
}
```
Note: `FirebaseMessagingService` supports `@AndroidEntryPoint` with Hilt.

---

### [CR-05] `LeaderboardViewModel.onCleared()` launches into a cancelled `viewModelScope` — Realtime channel never unsubscribed

**File:** `app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt` (line 153–157)

**Issue:**
```kotlin
override fun onCleared() {
    super.onCleared()
    viewModelScope.launch {                          // ← viewModelScope already cancelled here
        realtimeChannel?.let { supabase.realtime.removeChannel(it) }
    }
}
```
`ViewModel.onCleared()` is called *after* `viewModelScope` has been cancelled. Any coroutine launched on a cancelled scope is immediately cancelled before executing. The `removeChannel()` call never runs, leaving the Supabase Realtime WebSocket subscription active permanently (until the process dies).

**Risk:** Dangling WebSocket subscription on every LeaderboardScreen navigation away. With repeated navigations, multiple orphaned subscriptions accumulate and fire `loadLeaderboard()` in the background, causing spurious network calls and potential state corruption.

**Fix:** Use a separate `SupervisorJob` coroutine scope for cleanup, or use `runBlocking` as a last resort:
```kotlin
// Option A: dedicated cleanup scope not tied to viewModelScope lifecycle
private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

override fun onCleared() {
    super.onCleared()
    cleanupScope.launch {
        realtimeChannel?.let { supabase.realtime.removeChannel(it) }
        cleanupScope.cancel()
    }
}
```

---

## Warning Findings

### [WA-01] `FCMTokenManager.deleteToken()` deletes all device tokens for a user, not just the current device's token

**File:** `app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt` (line 51–59)

**Issue:** The delete filter uses only `eq("user_id", userId)`. If a user has the app installed on multiple devices, signing out on one device silently removes FCM registrations for **all** devices.

**Fix:** Store the current device's token in memory or SharedPreferences at registration time, and include it in the delete filter:
```kotlin
suspend fun deleteToken(userId: String, supabaseClient: SupabaseClient) {
    val currentToken = try { FirebaseMessaging.getInstance().token.await() } catch (_: Exception) { null }
        ?: return
    supabaseClient.postgrest["fcm_tokens"].delete {
        filter {
            eq("user_id", userId)
            eq("token", currentToken)
        }
    }
}
```

---

### [WA-02] Companion object `instance` vars are non-`@Volatile` and not thread-safe

**Files:**
- `data/repository/EloRepository.kt` (line 44–50)
- `data/repository/StreakRepository.kt` (line 57–63)
- `data/repository/ProgressRepository.kt` (line 48–55)
- `data/repository/DailyChallengeRepository.kt` (line 77–83)

**Issue:** All four repositories use the pattern:
```kotlin
companion object {
    var instance: XxxRepository? = null  // not @Volatile
}
init { instance = this }
```
`instance` is written on the main thread (Hilt injection) and read from background threads (e.g., `StreakDangerWorker`, coroutines in `ProgressRepository.postGame`). Without `@Volatile`, the JVM may cache a stale `null` on the reading thread, and the null-safe `?.` call sites would silently no-op instead of running.

**Fix:** Annotate with `@Volatile`:
```kotlin
companion object {
    @Volatile var instance: EloRepository? = null
}
```

---

### [WA-03] `MainActivity.pendingNavigationDestination` is static mutable state never cleared after consumption

**File:** `app/src/main/java/com/example/vietforces/MainActivity.kt` (companion object)

**Issue:** `pendingNavigationDestination` is set in `handleFcmNavigationIntent()` but there is no corresponding clear (set to `null`) after the `NavController` consumes it. On Activity recreation (rotation, system-initiated restart), the stored destination would be read again and re-trigger navigation — potentially navigating the user away from wherever they currently are.

**Fix:** Clear after consuming in `VietforcesApp`:
```kotlin
// In VietforcesApp, after navController.navigate(destination):
LaunchedEffect(Unit) {
    val destination = MainActivity.pendingNavigationDestination ?: return@LaunchedEffect
    MainActivity.pendingNavigationDestination = null   // clear before navigating
    navController.navigate(destination)
}
```

---

### [WA-04] RLS policies `users_select_public_username` and `progress_select_public` expose all columns with `USING (TRUE)`

**File:** `supabase/migrations/007_activity_feed.sql` (line 98–117)

**Issue:** Both policies use bare `USING (TRUE)` which grants **any authenticated user SELECT access to every column** in `users` and `user_progress`. For `users`, this includes fields that may contain sensitive data (e.g., `is_banned`, any future fields like timezone, phone). For `user_progress`, it exposes `streak_freeze_count`, `words_learned` JSONB, and `updated_at` — more data than needed.

PostgreSQL RLS cannot restrict individual columns, but the surface area can be minimised via column-level grants or views.

**Fix (pragmatic for academic project):** Create narrow views that expose only the required columns, and replace the broad policies with view-based access:
```sql
-- Minimal public view for username lookup
CREATE VIEW public.users_public AS
  SELECT id, username FROM public.users;
GRANT SELECT ON public.users_public TO authenticated;

-- Minimal public view for profile stats
CREATE VIEW public.user_progress_public AS
  SELECT user_id, elo_score, streak_count, total_games FROM public.user_progress;
GRANT SELECT ON public.user_progress_public TO authenticated;
```

---

### [WA-05] `send-streak-reminder` edge function ignores per-user notification opt-out preferences

**File:** `supabase/functions/send-streak-reminder/index.ts` (line 185–215)

**Issue:** `PreferencesManager` stores `KEY_NOTIF_STREAK_ENABLED` (user's opt-out preference for streak reminders), but the edge function queries all users with an FCM token regardless of their preference. Users who have disabled streak notifications in Settings will still receive them.

**Risk:** Possible app store policy violation (sending notifications users have explicitly disabled). Minor GDPR concern.

**Fix:** Persist the notification preference to a server-side column (e.g., `users.notif_streak_enabled BOOLEAN DEFAULT TRUE`) and add a filter to the edge function query:
```typescript
.eq("notif_streak_enabled", true)
```

---

### [WA-06] `award_daily_bonus` passes `p_challenge_date` to `update_streak`, not today's actual server date

**File:** `supabase/migrations/006_daily_bonus_elo.sql` (line 82)

**Issue:**
```sql
PERFORM public.update_streak(p_user_id, p_challenge_date);
```
`p_challenge_date` is the date of the challenge being completed (supplied by the client), not `CURRENT_DATE` on the server. If a user completes a stale challenge (e.g., yesterday's challenge via a cached response after midnight), `update_streak` records `last_practice_date` as yesterday, not today. This could silently prevent the streak from advancing on the actual current day.

**Fix:**
```sql
PERFORM public.update_streak(p_user_id, CURRENT_DATE);
```

---

## Info Findings

### [IN-01] `ActivityFeedViewModel` injects `AuthRepository` but never uses it

**File:** `app/src/main/java/com/example/vietforces/ui/viewmodel/ActivityFeedViewModel.kt` (line 61–63)

**Issue:** `@Suppress("UnusedPrivateMember")` on the injected `authRepository` confirms it is dead code. The suppression annotation signals a known issue rather than fixing it.

**Fix:** Remove the unused injection, or use it to add an authentication guard before fetching data.

---

### [IN-02] `DailyChallengeViewModel.computeSecondsUntilMidnightUtc()` is unnecessarily `public`

**File:** `app/src/main/java/com/example/vietforces/ui/viewmodel/DailyChallengeViewModel.kt` (line ~131)

**Issue:** The method is a private implementation detail of the countdown logic, but is declared with default (public) visibility — it was likely made public for unit-testing purposes. The ViewModel's `uiState` is the correct test surface.

**Fix:** Change to `private fun computeSecondsUntilMidnightUtc()`.

---

### [IN-03] `ImageToWordScreen` launches game-logic coroutines via `rememberCoroutineScope()` in composition

**File:** `app/src/main/java/com/example/vietforces/ui/screens/game/ImageToWordScreen.kt` (line ~56)

**Issue:** `val scope = rememberCoroutineScope()` is used to orchestrate game state transitions and AI grading calls. This scope is tied to the Compose `LifecycleOwner`, not to a ViewModel. If the user navigates away mid-grading, the coroutine is cancelled abruptly. If the screen recomposes, the scope is recreated. Game logic in a ViewModel would survive configuration changes and navigation events.

**Fix:** Migrate game state and async AI calls to a `@HiltViewModel` with `viewModelScope`.

---

### [IN-04] `MainScreen` and `ProfileScreen` read `UserProgressManager` state non-reactively

**Files:** `app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt` (line 44–45) · `app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt` (line 97–98)

**Issue:**
```kotlin
val currentStreak = UserProgressManager.getCurrentStreak()  // MainScreen
val eloRating = UserProgressManager.getEloRating()          // MainScreen + ProfileScreen
```
These are plain reads of in-memory singleton state. When `ProgressRepository.postGame()` or `loadFromCloud()` updates `UserProgressManager`, the Compose UI is **not recomposed** — the displayed ELO and streak remain stale until the next Activity lifecycle event.

**Fix:** Wrap the values in `mutableStateOf` inside `UserProgressManager`, or expose them as `StateFlow`s from a shared `ProfileViewModel` that the screens observe.

---

### [IN-05] `PublicProfileViewModel.rankTier` stores raw ELO integer as a string instead of a tier name

**File:** `app/src/main/java/com/example/vietforces/ui/viewmodel/PublicProfileViewModel.kt` (line ~115)

**Issue:**
```kotlin
rankTier = leaderEntry.eloScore.toString(), // raw ELO; rank name computed in Screen
```
The comment acknowledges this is intentional, but it means `PublicProfileData.rankTier` carries a misleading field name. `PublicProfileScreen` then re-derives the rank from `profile.eloScore` directly, making `rankTier` entirely unused in the data class. It is dead data.

**Fix:** Either populate `rankTier` with `EloRankUtils.getVietnameseRankName(...)` in the ViewModel (the correct place for display logic), or remove the field and have the Screen compute the tier from `eloScore` as it already does.

---

## Files with No Issues

The following reviewed files had no significant findings at standard depth:

- `VietForcesApplication.kt` *(functional — only issue is PreferencesManager.init placement, reported in CR-03)*
- `data/repository/AuthRepository.kt`
- `data/repository/LeaderboardRepository.kt`
- `data/repository/SocialRepository.kt`
- `data/repository/MigrationService.kt`
- `di/AuthModule.kt`
- `di/GameModule.kt`
- `di/RepositoryModule.kt`
- `navigation/Screen.kt`
- `ui/components/StreakHeatmapComposable.kt`
- `ui/screens/LoginScreen.kt`
- `ui/screens/RegisterScreen.kt`
- `ui/screens/SearchUsersScreen.kt`
- `ui/screens/PublicProfileScreen.kt`
- `ui/screens/ActivityFeedScreen.kt`
- `ui/screens/DailyChallengeScreen.kt`
- `ui/screens/LeaderboardScreen.kt`
- `ui/viewmodel/AuthViewModel.kt`
- `ui/viewmodel/DailyChallengeViewModel.kt`
- `ui/viewmodel/SocialViewModel.kt`
- `ui/viewmodel/ActivityFeedViewModel.kt` *(modulo IN-01)*
- `supabase/migrations/004_leaderboard_week.sql`
- `supabase/migrations/005_daily_completions.sql`
- `supabase/functions/refresh-streak-freeze/index.ts`
- `supabase/functions/generate-daily-challenge/index.ts` *(CORS wildcard is acceptable for pg_cron invoked endpoints)*

---

_Reviewed: 2026-07-23T13:40:00Z_
_Reviewer: gsd-code-reviewer (adversarial, standard depth)_
_Depth: standard_
