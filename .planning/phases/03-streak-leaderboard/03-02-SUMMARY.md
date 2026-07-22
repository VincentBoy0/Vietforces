---
phase: "03-streak-leaderboard"
plan: "02"
subsystem: "data/repository"
tags: ["elo", "streak", "supabase-rpc", "hilt", "post-game", "notification"]
dependency_graph:
  requires:
    - "03-01 (calculate_elo + update_streak SQL RPCs)"
    - "ProgressRepository (SYNC-01/02 base)"
    - "AuthRepository.currentUserId"
    - "UserProgressManager + PreferencesManager"
    - "NotificationManager.checkEloMilestone + addStreakNotification"
  provides:
    - "EloRepository.calculateElo() — calls calculate_elo RPC"
    - "StreakRepository.updateStreak() — calls update_streak RPC"
    - "StreakRepository.getStreakHistory(28) — streak heatmap data source (STREAK-04)"
    - "ProgressRepository.postGame() — single post-game entry point"
    - "PostGameResult data class"
  affects:
    - "ImageToWordScreen — postGame() wired at game completion"
    - "UserProgressManager.eloRating and currentStreak (updated server-side)"
    - "NotificationManager (ELO milestone + streak notifications)"
tech_stack:
  added:
    - "kotlinx.serialization.json.buildJsonObject — RPC params serialization"
    - "Postgrest.rpc(String, JsonObject) — Supabase RPC call pattern"
  patterns:
    - "companion object instance set in init{} for non-Hilt callsites"
    - "buildJsonObject for RPC params (Postgrest.rpc takes JsonObject, not data class)"
    - "postGame() as single post-session orchestrator (chain: ELO → streak → local state → notify → sync)"
    - "Locale.ROOT + UTC timezone for date strings (PRE-01)"
key_files:
  created:
    - "app/src/main/java/com/example/vietforces/data/repository/EloRepository.kt"
    - "app/src/main/java/com/example/vietforces/data/repository/StreakRepository.kt"
    - "app/src/main/java/com/example/vietforces/di/GameModule.kt"
  modified:
    - "app/src/main/java/com/example/vietforces/data/repository/ProgressRepository.kt"
    - "app/src/main/java/com/example/vietforces/ui/screens/game/ImageToWordScreen.kt"
decisions:
  - "Used buildJsonObject for RPC params — Postgrest.rpc() interface takes JsonObject, not data class (inline extension exists but JsonObject is simpler and more explicit)"
  - "postGame() uses companion object instance pattern (EloRepository.instance, StreakRepository.instance) rather than injecting into the composable — avoids Hilt propagation to non-ViewModel Composable"
  - "EloRankUtils.getVietnameseRankName() applied to server-returned rankTier before NotificationManager.checkEloMilestone() so UI always shows Vietnamese tier names"
  - "longestStreak updated locally in postGame() if streakResult.streakCount exceeds local record"
  - "syncToCloud() called with runCatching — postGame() never throws even if cloud sync fails; local state is always updated first"
metrics:
  duration: "9 minutes"
  completed_date: "2026-07-23"
  tasks_completed: 2
  tasks_total: 2
  files_created: 3
  files_modified: 2
---

# Phase 03 Plan 02: EloRepository + StreakRepository + postGame() Hook Summary

**One-liner:** EloRepository and StreakRepository wrap `calculate_elo`/`update_streak` RPCs via `buildJsonObject`; `ProgressRepository.postGame()` chains both into a single post-session call with local state update and milestone notifications.

## What Was Built

### EloRepository.kt
- `@Singleton @Inject constructor(supabase, authRepository)` — Hilt auto-bound
- `companion object { var instance: EloRepository? = null }` set in `init {}` 
- `@Serializable data class EloResult(newElo, rankTier, eloDelta)` with `@SerialName`
- `suspend fun calculateElo(correct, total, timeMs): Result<EloResult>` — calls `calculate_elo` RPC with `buildJsonObject` params; guards on `authRepository.currentUserId`

### StreakRepository.kt
- Same `@Singleton @Inject` + `companion object instance` pattern
- `@Serializable data class StreakResult(streakCount, freezeAvailable, wasFreezeUsed)` 
- `@Serializable data class StreakHistoryEntry(practicedDate)` for STREAK-04 heatmap
- `suspend fun updateStreak(): Result<StreakResult>` — calls `update_streak` with UTC today date
- `suspend fun getStreakHistory(days: Int = 28): Result<List<StreakHistoryEntry>>` — queries `streak_history` with `practiced_date >= cutoff` and ascending order
- `todayUtcString()` and `cutoffDateString(days)` use `Locale.ROOT + UTC` (PRE-01)

### GameModule.kt
- Empty `@Module @InstallIn(SingletonComponent::class) object GameModule`
- Both repositories auto-bound by Hilt via `@Singleton @Inject constructor`

### ProgressRepository.kt (modified)
- Added `data class PostGameResult(eloResult: EloResult?, streakResult: StreakResult?)`
- Added `suspend fun postGame(correct, total, timeMs): PostGameResult`:
  1. Guards on `authRepository.currentUserId`
  2. Calls `EloRepository.instance?.calculateElo(...)` 
  3. Calls `StreakRepository.instance?.updateStreak()`
  4. Updates `UserProgressManager.getUserSession().eloRating` + `currentStreak` + `longestStreak`
  5. Calls `NotificationManager.checkEloMilestone()` (ELO-02) with Vietnamese tier name
  6. Calls `NotificationManager.addStreakNotification()`
  7. Fire-and-forgets `syncToCloud()` via `runCatching`

### ImageToWordScreen.kt (modified)
- Added `val startTimeMs = remember { System.currentTimeMillis() }` at composable start
- In `onNextQuestion` game-completion block: `scope.launch { ProgressRepository.instance?.postGame(score, total, elapsedMs) }`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Postgrest.rpc() takes JsonObject, not @Serializable data class**
- **Found during:** Task 1 first compile
- **Issue:** `supabase.postgrest.rpc("calculate_elo", CalculateEloParams(...))` compiled with error "None of the following candidates is applicable". The `Postgrest` interface's `rpc(String, JsonObject, lambda)` member function requires `JsonObject`, not a custom data class. While a generic extension exists in `PostgrestRpcKt`, the direct member function resolved first.
- **Fix:** Replaced private `@Serializable data class` params with `buildJsonObject { put(...) }` from `kotlinx.serialization.json`. Removed the `CalculateEloParams` and `UpdateStreakParams` classes.
- **Files modified:** EloRepository.kt, StreakRepository.kt
- **Import added:** `kotlinx.serialization.json.buildJsonObject`, `kotlinx.serialization.json.put`

## Success Criteria Verification

| Criterion | Status |
|-----------|--------|
| ELO-01: `rpc("calculate_elo")` receives only game metrics; server returns new_elo | ✅ `buildJsonObject` with correct/total/timeMs only |
| ELO-02: `NotificationManager.checkEloMilestone()` called with server-returned ELO | ✅ In `postGame()` after `eloResult != null` check |
| STREAK-01: `rpc("update_streak")` called with UTC today date using Locale.ROOT | ✅ `todayUtcString()` uses `SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)` + UTC |
| STREAK-03: `StreakResult.wasFreezeUsed` available for UI | ✅ Field in `StreakResult` |
| STREAK-04: `StreakRepository.getStreakHistory(28)` callable | ✅ Queries `streak_history` with cutoff filter |
| No ELO delta sent from client | ✅ Only correct/total/timeMs in params |
| companion object instances set | ✅ Both repos set `instance = this` in `init {}` |
| Hilt injectable | ✅ `@Singleton @Inject constructor` + empty `GameModule` |
| `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL | ✅ |

## Known Stubs

None — all repository calls are fully wired to real Supabase RPCs.

## Threat Flags

No new network endpoints, auth paths, or schema changes introduced beyond the plan's `<threat_model>`.

- T-03-05 mitigated: Only `correct/total/timeMs` sent to `calculate_elo`; no ELO delta from client.
- T-03-06 mitigated: `todayUtcString()` uses `Locale.ROOT` + UTC timezone (PRE-01).

## Self-Check: PASSED

- EloRepository.kt: FOUND ✓
- StreakRepository.kt: FOUND ✓
- GameModule.kt: FOUND ✓
- postGame() in ProgressRepository.kt: FOUND ✓
- postGame call in ImageToWordScreen.kt: FOUND ✓
- rpc("calculate_elo") in EloRepository: FOUND ✓
- rpc("update_streak") in StreakRepository: FOUND ✓
- Commit 6deeb29: FOUND ✓
