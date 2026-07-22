# Phase 0: Pre-work Fixes — Context

**Gathered:** 2026-07-22
**Status:** Ready for planning
**Mode:** Auto-generated (autonomous mode, no gray areas)

<domain>
## Phase Boundary

Fix two silent bugs before any Supabase integration code is written:
1. `Locale.getDefault()` in date formatters → `Locale.ROOT` (prevents streak date corruption on non-English devices)
2. `applicationId` rename from `com.example.vietforces` → `com.vietforces.app` (unblocks FCM + Play Store)

These are mechanical changes with no user-facing UI. Scope is strictly limited to these two fixes.

</domain>

<decisions>
## Implementation Decisions

### Locale Fix Strategy
- Replace all `SimpleDateFormat("...", Locale.getDefault())` with `SimpleDateFormat("...", Locale.ROOT)`
- **Files affected** (confirmed by codebase grep):
  - `app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt` — lines 84, 189
  - `app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt` — line 610
  - `app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt` — line 196
- **Note**: `NotificationManager.kt:196` uses `"dd/MM/yyyy"` pattern for display formatting — this one CAN stay `Locale.getDefault()` since it's UI display (not storage), but switch to `Locale.ROOT` anyway for consistency
- Do NOT change: locale usage in UI strings, resource formatting, or non-date contexts

### applicationId Decision
- **New ID**: `com.vietforces.app`
- **Rationale**: Clean, non-example namespace, suitable for FCM/Play Store registration, consistent with app brand
- **Files to update**:
  - `app/build.gradle.kts`: change `applicationId = "com.example.vietforces"` → `applicationId = "com.vietforces.app"`
  - Keep `namespace = "com.example.vietforces"` unchanged (namespace is the Kotlin package, applicationId is the app ID — they are independent)
  - Android Manifest: no changes needed (applicationId override handled by Gradle)
  - **Do NOT rename** Kotlin packages/imports — that's a full package rename, out of scope for this phase

### Testing Scope
- Build must succeed (clean build)
- Zero regressions on existing game modes (manual verification)
- Grep confirms zero `Locale.getDefault()` hits in date-related files

</decisions>

<code_context>
## Existing Code Insights

- `UserProgressManager.getCurrentDate()` (line 84–85): returns `"yyyy-MM-dd"` string used as streak key in SharedPreferences — CRITICAL to fix, this is the streak date storage
- `UserProgressManager` line 189: same pattern, used in streak comparison logic
- `PerformanceScreen.kt` line 610: used to generate calendar heatmap dates — affects UI correctness on non-English devices
- `NotificationManager.kt` line 196: UI display only, lower risk but fix for consistency
- `namespace` vs `applicationId`: Gradle separates these — namespace = Kotlin package root, applicationId = Play Store identifier. Safe to change applicationId without touching Kotlin source.

</code_context>

<specifics>
## Specific Implementation Notes

- Atomic commits: one commit per fix (locale fix, then applicationId fix)
- Each commit message should reference the requirement (PRE-01, PRE-02)
- After applicationId change, do a clean build to verify no manifest merge conflicts

</specifics>

<deferred>
## Deferred (not in scope for Phase 0)

- Full Kotlin package rename (`com.example.vietforces` → `com.vietforces.app`) — deferred, out of scope
- Migrating `SimpleDateFormat` to `java.time` / `kotlinx-datetime` — deferred to Phase 1 or later
- ProGuard configuration — out of scope per PROJECT.md
- Any Supabase code — blocked until Phase 1

</deferred>
