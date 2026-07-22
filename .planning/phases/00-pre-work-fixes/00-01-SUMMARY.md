---
phase: "00-pre-work-fixes"
plan: "01"
subsystem: "data/manager, ui/screens"
tags: [locale, date-formatting, streak, bug-fix]

dependency_graph:
  requires: []
  provides: [PRE-01]
  affects:
    - UserProgressManager streak key generation
    - PerformanceScreen heatmap calendar
    - NotificationManager display timestamps

tech_stack:
  added: []
  patterns:
    - "Locale.ROOT for all SimpleDateFormat instances used as data keys or storage values"

key_files:
  modified:
    - app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt
    - app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt
    - app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt

decisions:
  - "Used Locale.ROOT (not Locale.ENGLISH) — ROOT is the canonical locale for machine-readable date keys"
  - "Did not migrate SimpleDateFormat to java.time — deferred per plan constraints"
  - "NotificationManager display formatter also switched to Locale.ROOT for consistency, per plan"

metrics:
  duration: "< 5 minutes"
  completed: "2026-07-22T23:45:00Z"
  tasks_completed: 2
  files_modified: 3
  substitutions: 4
---

# Phase 00 Plan 01: Locale.ROOT date formatters Summary

**One-liner:** Replaced all four `Locale.getDefault()` calls in `SimpleDateFormat` constructors with `Locale.ROOT`, preventing streak date-key corruption on Arabic/Persian/Hindi Android locales.

## What Was Done

Four occurrences of `SimpleDateFormat("...", Locale.getDefault())` were replaced with `Locale.ROOT` across three files:

| File | Line | Pattern | Context |
|------|------|---------|---------|
| `UserProgressManager.kt` | 84 | `yyyy-MM-dd` | `getTodayDateString()` — primary streak key |
| `UserProgressManager.kt` | 189 | `yyyy-MM-dd` | Streak update diff calculation |
| `PerformanceScreen.kt` | 610 | `yyyy-MM-dd` | `generateWeeksData()` heatmap calendar |
| `NotificationManager.kt` | 196 | `dd/MM/yyyy` | Relative-time display formatter |

## Why It Matters

`SimpleDateFormat` with `Locale.getDefault()` formats digits using locale-specific numeral systems on some Android devices. On Arabic locale, for example, the date `2024-01-15` becomes `٢٠٢٤-٠١-١٥`. When this string is stored as a SharedPreferences key and later looked up with a freshly-constructed formatter (also defaulting to device locale), the keys match — but only on that device. On a device that changes language, or after an OS update that resets locale, all previously stored keys become unmatchable, **silently resetting every user's streak to zero**.

`Locale.ROOT` forces ASCII digits and invariant formatting, making all keys deterministic regardless of device language.

## Verification

```
grep -rn "Locale.getDefault()" \
  app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt \
  app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt \
  app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt
```

**Result: zero hits** — no `Locale.getDefault()` remains in any date-formatter call site across the three files.

## Commits

| Task | Commit | Files | Description |
|------|--------|-------|-------------|
| Task 1 | `81ca2c3` | `UserProgressManager.kt` | Lines 84 + 189 — streak date keys |
| Task 2 | `fa9484c` | `PerformanceScreen.kt`, `NotificationManager.kt` | Lines 610 + 196 — heatmap + display |

## Deviations from Plan

None — plan executed exactly as written. All four substitutions applied at the documented line numbers. No imports changed (Locale was already imported in all three files). No other code touched.

## Known Stubs

None.

## Threat Flags

None. This change reduces attack surface (T-00-01 mitigated): streak keys can no longer be corrupted by device locale injection.

## Self-Check: PASSED

- [x] `UserProgressManager.kt` line 84 → `Locale.ROOT` ✓
- [x] `UserProgressManager.kt` line 189 → `Locale.ROOT` ✓
- [x] `PerformanceScreen.kt` line 610 → `Locale.ROOT` ✓
- [x] `NotificationManager.kt` line 196 → `Locale.ROOT` ✓
- [x] `grep` returns zero hits across all three files ✓
- [x] Task commit `81ca2c3` exists ✓
- [x] Task commit `fa9484c` exists ✓
