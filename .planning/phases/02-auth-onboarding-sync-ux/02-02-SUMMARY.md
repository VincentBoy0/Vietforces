---
phase: 02-auth-onboarding-sync-ux
plan: "02"
subsystem: onboarding
tags: [onboarding, guest-mode, compose, horizontal-pager, preferences, profile]
dependency_graph:
  requires: ["02-01"]
  provides: ["onboarding-flow", "guest-mode", "profile-guest-banner"]
  affects: ["MainActivity.kt", "ProfileScreen.kt", "PreferencesManager.kt", "Screen.kt"]
tech_stack:
  added: ["HorizontalPager (Compose Foundation Pager — existing BOM)"]
  patterns: ["HorizontalPager with userScrollEnabled=false for guided flow", "remember { PreferencesManager.getFlag() } for state from SharedPreferences", "LaunchedEffect for one-time dialog trigger"]
key_files:
  created:
    - app/src/main/java/com/example/vietforces/ui/screens/OnboardingScreen.kt
  modified:
    - app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt
    - app/src/main/java/com/example/vietforces/navigation/Screen.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
    - app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt
decisions:
  - "HorizontalPager userScrollEnabled=false enforces forward-only guided flow via buttons"
  - "Skip (Bỏ qua) button saves defaults and marks onboarding complete so users are never stuck"
  - "ProfileScreen receives onNavigateToRegister lambda instead of navController to keep screen decoupled"
  - "AlertDialog for one-time guest nudge triggered in ProfileScreen LaunchedEffect"
metrics:
  duration: "~15 min"
  completed: "2026-07-23"
  tasks_completed: 2
  files_changed: 5
---

# Phase 02 Plan 02: Onboarding + Guest Mode Summary

**One-liner:** 4-step HorizontalPager onboarding (Welcome → Level → Goal → Name/Avatar) gates first-launch and sets guest mode; ProfileScreen shows persistent VietRed banner + one-time dialog nudge.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | PreferencesManager flags + OnboardingScreen | `2353135` | PreferencesManager.kt (new flags/methods), OnboardingScreen.kt (created) |
| 2 | Wire Onboarding Start Destination + Guest Mode in ProfileScreen | `5699572` | Screen.kt, MainActivity.kt, ProfileScreen.kt |

## What Was Built

### PreferencesManager.kt (additions)
5 new SharedPreferences keys and 10 new getter/setter methods:
- `getOnboardingCompleted()` / `setOnboardingCompleted(Boolean)`
- `getIsGuest()` / `setIsGuest(Boolean)`
- `getSelectedLevel()` / `setSelectedLevel(String)` — default `"beginner"`
- `getDailyGoal()` / `setDailyGoal(Int)` — default `10`
- `getGuestPromptShown()` / `setGuestPromptShown(Boolean)`

### OnboardingScreen.kt (new)
- `@Composable fun OnboardingScreen(onFinish: () -> Unit)` — 4-page HorizontalPager, `userScrollEnabled=false`
- **Page 0 — WelcomePage:** Logo emoji, VietForces title, 3 feature bullet lines
- **Page 1 — ChooseLevelPage:** 3 LevelCard options (beginner/intermediate/advanced) with border highlight on selection
- **Page 2 — ChooseGoalPage:** 3 GoalCard options (5/10/15 words/day) with recommended label
- **Page 3 — NameAvatarPage:** 8-avatar FilterChip row + OutlinedTextField for display name; "Bắt đầu học!" button disabled when name is blank
- **PagerDotIndicator:** 4 dots, filled VietRed for current page
- **"Bỏ qua" (skip):** TextButton on pages 0-2, writes defaults and calls `onFinish()`
- On finish: writes `onboarding_completed=true`, `is_guest=true`, `selectedLevel`, `dailyGoal`, and `ProfileManager.updateName()`

### Screen.kt
- Added `object Onboarding : Screen("onboarding")`

### MainActivity.kt
- `val onboardingCompleted = remember { PreferencesManager.getOnboardingCompleted() }`
- `val startDestination = if (!onboardingCompleted) Screen.Onboarding.route else Screen.Main.route`
- NavHost now uses dynamic `startDestination`
- `composable(Screen.Onboarding.route)` added first in NavHost, `popUpTo(Screen.Onboarding.route) { inclusive = true }` on navigate to Main
- `ProfileScreen` composable wired with `onNavigateToRegister = { navController.navigate(Screen.Register.route) }`

### ProfileScreen.kt
- New parameter: `onNavigateToRegister: () -> Unit = {}`
- `isGuest`, `promptAlreadyShown`, `totalGamesPlayed` read with `remember {}` at top
- `LaunchedEffect` triggers `showGuestDialog = true` when `isGuest && totalGamesPlayed >= 1 && !promptAlreadyShown`
- **AlertDialog:** "Để sau" (TextButton) sets guestPromptShown + dismisses; "Đăng ký ngay" (Button) additionally calls `onNavigateToRegister()`
- **Guest banner Card:** VietRed 0.08 alpha background, 1dp border 0.3 alpha, text "💾 Lưu tiến độ lên cloud" + subtitle + "Đăng ký" Button — only visible when `isGuest = true`

## Verification

```
✅ ./gradlew :app:compileDebugKotlin — BUILD SUCCESSFUL in 27s
✅ grep "onboarding" PreferencesManager.kt — KEY_ONBOARDING_COMPLETED, getOnboardingCompleted, setOnboardingCompleted
✅ grep "Screen.Onboarding" MainActivity.kt — startDestination logic present
✅ grep "isGuest" ProfileScreen.kt — banner card and dialog conditions present
✅ grep "HorizontalPager" OnboardingScreen.kt — present with userScrollEnabled=false
```

> Note: `:app:assembleDebug` fails at `hiltJavaCompileDebug` with the pre-existing metadata version error (`kotlinx-metadata-jvm` 2.4.0 > max 2.1.0). This was documented in 02-01-SUMMARY and is unrelated to the changes in this plan. The Kotlin compilation target (`compileDebugKotlin`) succeeds cleanly.

## Deviations from Plan

### Auto-added enhancements (Rule 2)

**1. [Rule 2 - Missing critical functionality] Skip button writes defaults before calling onFinish**
- **Found during:** Task 1 (OnboardingScreen implementation)
- **Issue:** Plan described Bỏ qua button but didn't specify what happens to prefs on skip — users who skip would have `onboarding_completed=false` and see onboarding again next launch
- **Fix:** Skip button also calls `setOnboardingCompleted(true)` and `setIsGuest(true)` before `onFinish()`, same as finishing normally; blank name defaults to "Chiến binh"
- **Files modified:** OnboardingScreen.kt

## Known Stubs

None — all UI data is wired to PreferencesManager reads/writes and ProfileManager.

## Threat Flags

No new network endpoints, auth paths, or trust boundary changes introduced. All data is stored locally in SharedPreferences.

## Self-Check: PASSED

- ✅ `OnboardingScreen.kt` — FOUND
- ✅ Commits `2353135` and `5699572` — FOUND in git log
- ✅ PreferencesManager.kt has 5 new KEY constants and 10 new methods
- ✅ Screen.kt has `object Onboarding : Screen("onboarding")`
- ✅ MainActivity.kt uses `startDestination` variable
- ✅ ProfileScreen.kt has `isGuest` banner card
