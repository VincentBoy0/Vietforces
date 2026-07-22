---
phase: 02-auth-onboarding-sync-ux
plan: "03"
subsystem: data-sync
tags: [sync, cloud, migration, supabase, postgrest, progress]
dependency_graph:
  requires:
    - 02-01  # AuthRepository + Supabase client
    - 02-02  # PreferencesManager onboarding/guest flags
  provides:
    - ProgressRepository (syncToCloud, loadFromCloud, syncIfLoggedIn)
    - RemoteProgressSource (upsert, fetch)
    - MigrationService (one-time guest migration)
  affects:
    - MainActivity (onResume sync + migration trigger)
    - PreferencesManager (migration_completed flag)
tech_stack:
  added:
    - io.github.jan-tennert.supabase:postgrest-kt 3.7.0 (already in BOM; no new dep added)
  patterns:
    - "@Singleton @Inject constructor for Hilt-managed singletons"
    - "companion object instance for non-Hilt game-manager sync hooks"
    - "last-write-wins conflict resolution via ISO 8601 date string comparison"
key_files:
  created:
    - app/src/main/java/com/example/vietforces/data/model/UserProgressDto.kt
    - app/src/main/java/com/example/vietforces/data/remote/RemoteProgressSource.kt
    - app/src/main/java/com/example/vietforces/data/repository/ProgressRepository.kt
    - app/src/main/java/com/example/vietforces/data/service/MigrationService.kt
    - app/src/main/java/com/example/vietforces/di/RepositoryModule.kt
  modified:
    - app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt
    - app/src/main/java/com/example/vietforces/MainActivity.kt
decisions:
  - "UserSession.learnedWordIds is kept local-only: cloud stores only the count (wordsLearnedCount). Individual word IDs cannot be reconstructed from the count alone; local set is preserved on loadFromCloud."
  - "java.time.Instant avoided (requires API 26+, minSdk=24); ISO-8601 timestamp via SimpleDateFormat with UTC timezone."
  - "companion object instance pattern used instead of a ViewModel layer for post-game sync hooks — avoids requiring full Hilt injection in game managers while keeping the door open for future ViewModel wiring."
  - "EncounteredItemsManager spaced-repetition weights deferred from migration scope per CONTEXT.md."
metrics:
  duration_minutes: 25
  completed_date: "2026-07-23"
  tasks_completed: 2
  files_created: 5
  files_modified: 2
---

# Phase 02 Plan 03: Progress Sync Summary

**One-liner:** Supabase postgrest cloud sync (upsert + last-write-wins fetch) with one-time guest migration gated by SharedPreferences flag.

## What Was Built

### Task 1 — Core Sync Layer

**UserProgressDto** (`data/model/UserProgressDto.kt`)
- `@Serializable` data class with `@SerialName` mappings for all `user_progress` columns
- Fields: `userId`, `eloRating`, `currentStreak`, `longestStreak`, `wordsLearnedCount`, `lastPracticed`, `updatedAt`

**RemoteProgressSource** (`data/remote/RemoteProgressSource.kt`)
- `@Singleton @Inject constructor(SupabaseClient)`
- `upsertProgress(dto)`: `supabase.from("user_progress").upsert(dto) { onConflict = "user_id" }`
- `fetchProgress(userId)`: `select { filter { eq("user_id", userId) } }.decodeSingleOrNull<UserProgressDto>()`
- Both wrapped in `Result<T>` for safe error propagation

**ProgressRepository** (`data/repository/ProgressRepository.kt`)
- `syncToCloud()`: builds DTO from `UserProgressManager.getUserSession()` + current UTC timestamp, upserts
- `loadFromCloud()`: fetches row; overwrites local session fields only when `cloudDate > localDate` (ISO-8601 lexicographic compare); saves via `PreferencesManager.saveUserSession()`
- `syncIfLoggedIn()`: convenience wrapper — silent no-op when `currentUserId == null`
- `companion object { var instance }` set in `init {}` for non-Hilt call sites

**RepositoryModule** (`di/RepositoryModule.kt`)
- Empty `@Module @InstallIn(SingletonComponent::class) object` — documents module boundary; Hilt auto-binds `@Singleton @Inject constructor` classes without explicit `@Provides`

### Task 2 — Migration Service + MainActivity Integration

**MigrationService** (`data/service/MigrationService.kt`)
- `migrateIfNeeded()`: checks `PreferencesManager.getMigrationCompleted()` guard; calls `syncToCloud()`; sets flag on success
- Idempotent: cleared flag re-syncs same local data (no data loss)

**PreferencesManager** (updated)
- Added `KEY_MIGRATION_COMPLETED = "migration_completed"`
- `setMigrationCompleted(done: Boolean)` + `getMigrationCompleted(): Boolean`

**MainActivity** (updated)
- `@Inject lateinit var progressRepository: ProgressRepository`
- `@Inject lateinit var migrationService: MigrationService`
- `override fun onResume()`: launches `loadFromCloud()` then `syncIfLoggedIn()` (SYNC-01 + SYNC-02)
- Passes `migrationService` to `VietforcesApp(migrationService)`

**VietforcesApp composable** (updated)
- Added `migrationService: MigrationService` parameter
- `rememberCoroutineScope()` for composable-scoped coroutines
- `LaunchedEffect(authState)` — fires `migrateIfNeeded()` on first `AuthState.Authenticated` (ONBOARD-03)

## Requirements Fulfilled

| Requirement | Status |
|-------------|--------|
| SYNC-01: Progress syncs to Supabase after game sessions (foreground sync fallback) | ✅ |
| SYNC-02: Load cloud progress on foreground if cloud is newer | ✅ |
| ONBOARD-03: One-time guest→cloud migration on first login | ✅ |

## Deviations from Plan

### Minor API Adaptations

**1. [Rule 1 - Bug] java.time.Instant replaced with SimpleDateFormat**
- **Found during:** Task 1 implementation
- **Issue:** `Instant.now()` requires API 26+; minSdk=24
- **Fix:** Used `SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)` with UTC timezone
- **Files modified:** `ProgressRepository.kt`

**2. [Rule 2 - Missing param] VietforcesApp receives migrationService as parameter**
- **Found during:** Task 2 implementation
- **Issue:** `VietforcesApp` is a standalone `@Composable`; `migrationService` is only available in `MainActivity` via field injection. Hilt doesn't inject into arbitrary composables.
- **Fix:** Added `migrationService: MigrationService` parameter to `VietforcesApp`; passed from `MainActivity.setContent` block. Clean, testable, no coupling violation.

## Known Stubs

None. All wired data flows through live `UserProgressManager.getUserSession()` and `RemoteProgressSource`.

## Threat Flags

No new threat surface beyond what was declared in the plan's `<threat_model>`.

**Reminder (from plan):** Supabase RLS policy on `user_progress` must enforce `auth.uid() = user_id`. Verify in Supabase dashboard that the policy was created in Phase 1 (FOUND-01). If absent, run:
```sql
CREATE POLICY "Users can upsert own progress" ON user_progress
FOR ALL USING (auth.uid() = user_id);
```

## Self-Check: PASSED

Files exist:
- ✅ `data/model/UserProgressDto.kt`
- ✅ `data/remote/RemoteProgressSource.kt`
- ✅ `data/repository/ProgressRepository.kt`
- ✅ `data/service/MigrationService.kt`
- ✅ `di/RepositoryModule.kt`
- ✅ `data/storage/PreferencesManager.kt` (updated)
- ✅ `MainActivity.kt` (updated)

Commit: `d603509` — verified in `git log --oneline`.

Build: `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**
