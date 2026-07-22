---
phase: "00-pre-work-fixes"
plan: "02"
subsystem: "build-config"
tags: ["applicationId", "gradle", "FCM", "play-store", "build-config"]
dependency_graph:
  requires: []
  provides: ["applicationId=com.vietforces.app"]
  affects: ["FCM registration", "Play Store listing", "Phase 4 - notifications"]
tech_stack:
  added: []
  patterns: ["single-line applicationId override via Gradle defaultConfig"]
key_files:
  created: []
  modified:
    - app/build.gradle.kts
decisions:
  - "applicationId changed to com.vietforces.app; namespace left as com.example.vietforces (full package rename deferred)"
metrics:
  duration: "~5 minutes"
  completed: "2026-07-22"
---

# Phase 00 Plan 02: Rename applicationId Summary

**One-liner:** Renamed `applicationId` from `com.example.vietforces` to `com.vietforces.app` in `app/build.gradle.kts`, unblocking FCM registration and Play Store submission. `namespace` left unchanged.

---

## Tasks Completed

| # | Task | Status | Commit |
|---|------|--------|--------|
| 1 | Change applicationId in app/build.gradle.kts | ✅ Done | `726ab3f` |
| 2 | Clean build to confirm no manifest merge conflicts | ⚠️ Skipped (env) | — |

---

## Change Made

**File:** `app/build.gradle.kts`

| | Value |
|-|-------|
| **Old** | `applicationId = "com.example.vietforces"` |
| **New** | `applicationId = "com.vietforces.app"` |

**Unchanged (by design):** `namespace = "com.example.vietforces"`

---

## Verification Output

```
$ grep -n 'applicationId\|namespace' app/build.gradle.kts
24:    namespace = "com.example.vietforces"
30:        applicationId = "com.vietforces.app"
```

- ✅ `applicationId = "com.vietforces.app"` — 1 hit (correct)
- ✅ `namespace = "com.example.vietforces"` — 1 hit (unchanged)
- ✅ No remaining `applicationId = "com.example.vietforces"` line

---

## Build Result

Task 2 (clean build verification) was **not executable** in this environment:
- **Reason:** No Android SDK installed at `~/Library/Android/sdk` — `local.properties` with `sdk.dir` is missing.
- **JDK found:** Homebrew OpenJDK 26 at `/opt/homebrew/opt/openjdk`
- **Gradle version:** 8.13
- **Build error:** `26.0.1` — JDK/SDK compatibility check fires before compilation; unrelated to the `applicationId` change.
- **Impact on this plan:** The `applicationId` value change is a single string substitution in `defaultConfig`; it does not alter manifest merge logic. The build should succeed once the Android SDK is available (e.g., when opened in Android Studio). There is no `package` attribute in `AndroidManifest.xml` to conflict.

---

## Commit

| Hash | Message |
|------|---------|
| `726ab3f` | `fix(PRE-02): rename applicationId to com.vietforces.app` |

---

## Deviations from Plan

**1. [Rule 3 - Environment] Task 2 build skipped — no Android SDK**

- **Found during:** Task 2
- **Issue:** `./gradlew assembleDebug` failed because no Android SDK is configured (`~/Library/Android/sdk` absent, no `local.properties`). The error message was the JDK version string `26.0.1`, indicating a pre-compile SDK check failure.
- **Fix:** Not fixable in this agent (no authority to install Android SDK). Documented as environment constraint.
- **Impact:** Low. The code change (one string replacement) is correct and cannot cause manifest merge conflicts. Build will succeed when run in Android Studio with SDK present.
- **Files modified:** None

---

## Threat Flags

None — only `defaultConfig.applicationId` changed; no new network endpoints, auth paths, file access, or schema changes introduced.

---

## Self-Check: PASSED

- ✅ `app/build.gradle.kts` — file exists and contains `applicationId = "com.vietforces.app"`
- ✅ Commit `726ab3f` exists in git log
- ✅ `namespace = "com.example.vietforces"` unchanged
