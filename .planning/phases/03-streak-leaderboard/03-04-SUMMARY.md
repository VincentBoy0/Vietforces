---
phase: 03-streak-leaderboard
plan: "04"
subsystem: streak-ui-worker
tags: [streak, heatmap, workmanager, notification, compose]
dependency_graph:
  requires: [03-02]
  provides: [streak-heatmap-composable, streak-danger-worker]
  affects: [ProfileScreen, VietForcesApplication]
tech_stack:
  added: [WorkManager 2.9.1, NotificationCompat, CoroutineWorker]
  patterns: [produceState, PeriodicWorkRequestBuilder, NotificationChannel]
key_files:
  created:
    - app/src/main/java/com/example/vietforces/ui/components/StreakHeatmapComposable.kt
    - app/src/main/java/com/example/vietforces/data/worker/StreakDangerWorker.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt
    - app/src/main/java/com/example/vietforces/VietForcesApplication.kt
decisions:
  - "Used produceState (not LaunchedEffect + mutableStateOf) for streak history loading in ProfileScreen — cleaner coroutine-scoped state pattern for async data"
  - "StreakDangerWorker uses Calendar.HOUR_OF_DAY (local time) for 22:00 check per spec, UTC only for date string comparison (PRE-01 compliance)"
  - "Notification channel created lazily inside doWork() to avoid requiring Application context init order dependency"
  - "Worker path placed under data/worker/ (plan spec listed workers/ but plan frontmatter files_modified listed data/worker/ — matched frontmatter)"
  - "Cell sizing uses aspectRatio(1f) + weight(1f) instead of fixed 32.dp for responsive grid across screen widths"
metrics:
  duration: "~15 minutes"
  completed: "2026-07-22T18:58:37Z"
  tasks_completed: 2
  files_changed: 6
---

# Phase 03 Plan 04: StreakHeatmapComposable + StreakDangerWorker Summary

**One-liner:** 28-day VietRed heatmap in ProfileScreen + hourly CoroutineWorker posting streak-danger notification after 22:00 local time via WorkManager KEEP policy.

## Tasks Completed

| # | Name | Commit | Files |
|---|------|--------|-------|
| 1 | WorkManager deps + StreakDangerWorker + VietForcesApplication | e24dd51 | libs.versions.toml, build.gradle.kts, StreakDangerWorker.kt, VietForcesApplication.kt |
| 2 | StreakHeatmapComposable + embed in ProfileScreen | e24dd51 | StreakHeatmapComposable.kt, ProfileScreen.kt |

## What Was Built

### StreakHeatmapComposable.kt
- `@Composable fun StreakHeatmapComposable(practicedDates: Set<String>, modifier: Modifier)`
- Renders 28 days in a 7-column grid (Mon–Sun header, Vietnamese labels T2–CN)
- Oldest date is top-left; today is bottom-right; grid is Monday-aligned with null padding
- Practiced days: VietRed background + 🔥 emoji; absent days: `MaterialTheme.colorScheme.surfaceVariant`
- Legend row below grid; `Card` wrapper with `RoundedCornerShape(16.dp)`
- PRE-01 compliant: `SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)` with UTC timezone

### StreakDangerWorker.kt
- `class StreakDangerWorker : CoroutineWorker` in `com.example.vietforces.data.worker`
- Guard 1: `PreferencesManager.getIsGuest()` → skip if guest
- Guard 2: `Calendar.getInstance().get(HOUR_OF_DAY) < 22` → skip if before 22:00 local
- Guard 3: `lastPracticed == todayUtc` → skip if already practiced today (UTC date comparison)
- Creates `NotificationChannel("streak_danger", ...)` for Android 8+
- Posts via `NotificationManagerCompat.notify()` with `PRIORITY_HIGH`, catches `SecurityException` silently for Android 13+
- Notification text: "⚠️ Streak sắp bị mất!" / "Bạn chưa học hôm nay. Hãy chơi 1 game để giữ chuỗi ngày!"

### VietForcesApplication.kt
- Added `override fun onCreate()` with `PeriodicWorkRequestBuilder<StreakDangerWorker>(1, TimeUnit.HOURS)`
- `enqueueUniquePeriodicWork("streak_danger_check", ExistingPeriodicWorkPolicy.KEEP, ...)`

### ProfileScreen.kt
- Added `produceState<Set<String>>(initialValue = emptySet())` loading `StreakRepository.instance?.getStreakHistory(28)`
- `StreakHeatmapComposable(practicedDates = practicedDates)` inserted below `ProfileStatsCard`

### Gradle
- `libs.versions.toml`: `work = "2.9.1"` + `work-runtime-ktx` library alias
- `app/build.gradle.kts`: `implementation(libs.work.runtime.ktx)`

## Verification

- `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (0 errors, only pre-existing deprecation warnings)
- All 6 success criteria met

## Deviations from Plan

### Minor Path Deviation (Auto-resolved)
- **Found during:** Task 1 setup
- **Issue:** Plan prompt specified `workers/` package directory; plan frontmatter `files_modified` listed `data/worker/` — chose `data/worker/` to match the frontmatter (canonical spec)
- **Fix:** Created `app/src/main/java/com/example/vietforces/data/worker/StreakDangerWorker.kt`

No other deviations — plan executed as written.

## Known Stubs

None — `StreakRepository.instance?.getStreakHistory(28)` is wired to live Supabase data (implemented in plan 03-02). `practicedDates` renders live data from the database. No placeholder values in rendered output.

## Threat Flags

None — no new network endpoints, auth paths, or file access patterns introduced beyond what the threat model already covers. `T-03-12` mitigation (KEEP policy) is applied as specified.

## Self-Check

- [x] `app/src/main/java/com/example/vietforces/ui/components/StreakHeatmapComposable.kt` — created
- [x] `app/src/main/java/com/example/vietforces/data/worker/StreakDangerWorker.kt` — created
- [x] `app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt` — contains `StreakHeatmapComposable`
- [x] `app/src/main/java/com/example/vietforces/VietForcesApplication.kt` — contains `enqueueUniquePeriodicWork`
- [x] Commit `e24dd51` exists in git log
- [x] `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

## Self-Check: PASSED
