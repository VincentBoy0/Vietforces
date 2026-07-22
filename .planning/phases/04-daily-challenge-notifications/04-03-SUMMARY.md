---
phase: "04-daily-challenge-notifications"
plan: "03"
subsystem: "notifications"
tags: ["fcm", "firebase", "push-notifications", "android13", "settings"]
dependency_graph:
  requires: ["04-01", "04-02"]
  provides: ["FCM token registration", "push notification delivery", "notification permission flow", "notification settings UI"]
  affects: ["PreferencesManager", "SettingsScreen", "MainActivity", "AndroidManifest"]
tech_stack:
  added:
    - "firebase-messaging-ktx 24.1.1 (no google-services plugin)"
  patterns:
    - "FCM service without google-services.json at compile time"
    - "Standalone SupabaseClient in FCM service (no Hilt)"
    - "Android 13 runtime permission request pattern"
key_files:
  created:
    - "app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt"
    - "app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt"
  modified:
    - "gradle/libs.versions.toml"
    - "app/build.gradle.kts"
    - "app/src/main/AndroidManifest.xml"
    - "app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt"
    - "app/src/main/java/com/example/vietforces/ui/screens/SettingsScreen.kt"
    - "app/src/main/java/com/example/vietforces/MainActivity.kt"
decisions:
  - "No google-services plugin applied — firebase-messaging-ktx added as plain dependency; app compiles without google-services.json; push messages only work when Firebase project is configured at runtime"
  - "VietForcesFirebaseMessagingService builds its own SupabaseClient (no Hilt injection) to avoid AndroidEntryPoint complexity for a rarely-active service"
  - "FCMTokenManager placed in data/manager/ alongside existing managers for consistency"
  - "pendingNavigationDestination companion-object field used to bridge FCM intent extras to Compose NavController"
metrics:
  duration: "~8 minutes"
  completed: "2026-07-22T19:17:55Z"
  tasks_completed: 8
  files_changed: 8
---

# Phase 04 Plan 03: FCM Setup + Notification Settings + Android 13 Permission Summary

**One-liner:** Firebase Messaging service, fcm_tokens Supabase upsert, POST_NOTIFICATIONS runtime permission, and notification toggle UI — all compile-safe without google-services.json.

## Tasks Completed

| # | Task | Status | Commit |
|---|------|--------|--------|
| 1 | Add firebase-messaging-ktx to libs.versions.toml + app/build.gradle.kts | ✅ | bfa5110 |
| 2 | Create FCMTokenManager.kt (token register/delete via Supabase) | ✅ | bfa5110 |
| 3 | Create VietForcesFirebaseMessagingService.kt (onNewToken + onMessageReceived) | ✅ | bfa5110 |
| 4 | AndroidManifest: POST_NOTIFICATIONS permission + service declaration | ✅ | bfa5110 |
| 5 | PreferencesManager: notif_streak_enabled + notif_daily_enabled keys | ✅ | bfa5110 |
| 6 | SettingsScreen: "🔔 Thông báo" card with two Switch rows | ✅ | bfa5110 |
| 7 | MainActivity: POST_NOTIFICATIONS request on Android 13+ | ✅ | bfa5110 |
| 8 | MainActivity: handle "navigate_to" FCM intent extra | ✅ | bfa5110 |

## What Was Built

### FCMTokenManager (`data/manager/FCMTokenManager.kt`)
- `registerToken(userId, supabaseClient)`: fetches FCM token via `FirebaseMessaging.getInstance().token.await()`, upserts to `fcm_tokens` table with `user_id`, `token`, `updated_at`
- `deleteToken(userId, supabaseClient)`: removes the token row from Supabase on logout
- All Firebase exceptions caught and logged — graceful no-op when Firebase is not initialized

### VietForcesFirebaseMessagingService
- `onNewToken(token)`: launches coroutine, builds ephemeral SupabaseClient from BuildConfig, calls `auth.currentUserOrNull()` to get user ID, calls `FCMTokenManager.registerToken`
- `onMessageReceived(message)`: creates notification channel on demand, builds `NotificationCompat` with title/body from message, sets `PendingIntent` to `MainActivity` with `navigate_to = "daily_challenge"` extra when `message.data["screen"] == "daily_challenge"`
- Channel ID: `vietforces_push`, importance HIGH

### AndroidManifest changes
- Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
- Added `<service android:name=".VietForcesFirebaseMessagingService" android:exported="false">` with `com.google.firebase.MESSAGING_EVENT` intent-filter

### PreferencesManager additions
- `KEY_NOTIF_STREAK_ENABLED = "notif_streak_enabled"` (default `true`)
- `KEY_NOTIF_DAILY_ENABLED = "notif_daily_enabled"` (default `true`)
- `getNotifStreakEnabled()` / `setNotifStreakEnabled(Boolean)`
- `getNotifDailyEnabled()` / `setNotifDailyEnabled(Boolean)`

### SettingsScreen — Notification card
- New `NotificationSettingsCard` composable inserted between AI settings and mascot preview
- "Nhắc nhở streak" toggle → `PreferencesManager.setNotifStreakEnabled()`
- "Thách đấu hàng ngày" toggle → `PreferencesManager.setNotifDailyEnabled()`
- Reuses existing `ToggleRow` composable for consistent look

### MainActivity changes
- Imports `Manifest`, `ActivityCompat`, `ContextCompat`, `Build`, `PackageManager`
- Requests `POST_NOTIFICATIONS` on Android 13+ (TIRAMISU) in `onCreate`
- `handleFcmNavigationIntent()` reads `navigate_to` string extra from incoming `Intent`
- `companion object` exposes `pendingNavigationDestination: String?` for Compose navigation

## Build Verification

```
BUILD SUCCESSFUL in 36s
18 actionable tasks: 18 executed
```
Only pre-existing deprecation warnings (unrelated icons/Divider), no errors.

## Deviations from Plan

### Auto-handled
**1. [Rule 3 - Blocking] No `getUserId()` in PreferencesManager**
- **Found during:** Task 3 (VietForcesFirebaseMessagingService implementation)
- **Issue:** Plan assumed `PreferencesManager.getUserId()` exists; it does not — the project uses `supabase.auth.currentUserOrNull()?.id` from the SupabaseClient
- **Fix:** Updated service to build ephemeral SupabaseClient and call `auth.currentUserOrNull()?.id` directly, matching the existing AuthRepository pattern
- **Files modified:** `VietForcesFirebaseMessagingService.kt`

**2. [Rule 2 - Missing import] Auth module import for `currentUserOrNull`**
- **Found during:** Task 3
- **Issue:** `io.github.jan.supabase.auth.auth` extension property needed explicit import
- **Fix:** Added `import io.github.jan.supabase.auth.auth`
- **Files modified:** `VietForcesFirebaseMessagingService.kt`

## Known Stubs

None — all toggle state is wired to real PreferencesManager persistence.

## Self-Check: PASSED

- ✅ `app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt` — FOUND
- ✅ `app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt` — FOUND
- ✅ Commit `bfa5110` — FOUND
- ✅ `BUILD SUCCESSFUL` confirmed
