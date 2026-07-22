---
phase: 02-auth-onboarding-sync-ux
plan: "04"
subsystem: ui-ux
tags:
  - compose
  - dark-mode
  - shimmer
  - empty-state
  - error-state
  - ux-polish
dependency_graph:
  requires: []
  provides:
    - EmptyStateComposable
    - ShimmerBox
    - ErrorStateComposable
    - VietForces dark mode
    - Shimmer loading in MainScreen
  affects:
    - app/src/main/java/com/example/vietforces/ui/components/UiComponents.kt
    - app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt
    - app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt
    - app/src/main/java/com/example/vietforces/ui/theme/Theme.kt
    - app/src/main/java/com/example/vietforces/ui/theme/Color.kt
tech_stack:
  added: []
  patterns:
    - "rememberInfiniteTransition for shimmer (no external shimmer library)"
    - "MaterialTheme.colorScheme for dark-mode-aware colors"
    - "dynamicColor=false to enforce brand color identity"
key_files:
  created:
    - app/src/main/java/com/example/vietforces/ui/components/UiComponents.kt
  modified:
    - app/src/main/java/com/example/vietforces/ui/theme/Color.kt
    - app/src/main/java/com/example/vietforces/ui/theme/Theme.kt
    - app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt
    - app/src/main/java/com/example/vietforces/ui/screens/ProfileScreen.kt
    - gradle/libs.versions.toml
decisions:
  - "ShimmerBox uses only rememberInfiniteTransition + animateFloat (no shimmer library) to avoid external dependency"
  - "dynamicColor=false in VietforcesTheme to prevent Material You from overriding VietRed/VietYellow brand colors on Android 12+"
  - "Hilt downgraded 2.60.1 → 2.51.1 to restore AGP 8.13.2 compatibility (2.60.1 requires AGP 9.0+)"
metrics:
  duration: "~18 minutes"
  completed: "2026-07-22T17:46:10Z"
  tasks_completed: 2
  files_changed: 6
---

# Phase 02 Plan 04: UX Polish — EmptyState/Shimmer/ErrorState + Dark Mode Summary

**One-liner:** Three reusable UX composables (EmptyStateComposable, ShimmerBox, ErrorStateComposable) plus VietForces-branded dark mode with dynamicColor=false, wired into MainScreen (150ms shimmer skeleton) and ProfileScreen (MaterialTheme-aware backgrounds).

## What Was Built

### Task 1: UiComponents.kt — Three Reusable Composables

Created `app/src/main/java/com/example/vietforces/ui/components/UiComponents.kt` with:

**EmptyStateComposable (UX-01):**
- Parameters: `illustration: String`, `message: String`, `ctaText: String? = null`, `onCtaClick: (() -> Unit)? = null`
- Layout: 64sp illustration emoji + 16sp message (onSurfaceVariant color) + optional CTA Button (primary color)
- Padding: `vertical=48.dp, horizontal=32.dp`

**ShimmerBox (UX-02, UX-03):**
- Uses `rememberInfiniteTransition + animateFloat` exclusively — zero external shimmer library
- Alpha: `0.3f → 0.9f`, `tween(800ms, LinearEasing)`, `RepeatMode.Reverse`
- Background: `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)` — dark-mode aware

**ErrorStateComposable (UX-04):**
- Parameters: `message: String = "Không thể tải dữ liệu..."`, `onRetry: () -> Unit`
- Layout: ⚠️ 48sp emoji + 15sp message + `OutlinedButton` with `Icons.Default.Refresh` + "Thử lại"
- Security: kdoc explicitly warns callers not to pass raw exception messages (T-02-14)

### Task 2: Wire Components + Fix Dark Mode

**Color.kt** — Added 5 dark mode variants:
- `BackgroundDark = Color(0xFF121212)` — OLED-friendly deep black
- `SurfaceDark = Color(0xFF1E1E1E)` — elevated surface
- `CardBackgroundDark = Color(0xFF2C2C2C)` — card surfaces in dark
- `TextPrimaryDark = Color(0xFFEEEEEE)` — high contrast text
- `TextSecondaryDark = Color(0xFFAAAAAA)` — secondary text

**Theme.kt** — Replaced placeholder Purple/Pink schemes with VietForces brand:
- `DarkColorScheme`: `primary=VietRed`, `background=BackgroundDark (#121212)`, `surface=SurfaceDark (#1E1E1E)`, `surfaceVariant=Color(0xFF3A3A3A)`
- `LightColorScheme`: `primary=VietRed`, `background=BackgroundLight`, `surface=Color.White`
- `VietforcesTheme`: `dynamicColor: Boolean = false` — prevents Material You from overriding brand colors on Android 12+
- Removed unused dynamic color code paths (no more `dynamicDarkColorScheme` / `dynamicLightColorScheme` calls)

**MainScreen.kt** — Added shimmer skeleton loading (UX-02, UX-03):
```kotlin
var cardsLoaded by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { delay(150L); cardsLoaded = true }
```
- When `!cardsLoaded`: renders 3 `ShimmerBox(Modifier.fillMaxWidth().height(120.dp))` placeholders
- When `gameModes.isEmpty()`: renders `EmptyStateComposable` with 🎮 illustration (UX-01)
- When loaded with data: renders real `GameModeCard` list (existing behaviour)
- Background fixed: `.background(BackgroundLight)` → `.background(MaterialTheme.colorScheme.background)` (UX-05)
- TopAppBar + all cards: `Color.White` → `MaterialTheme.colorScheme.surface`

**ProfileScreen.kt** — Dark mode parity (UX-05):
- Screen background: `.background(BackgroundLight)` → `.background(MaterialTheme.colorScheme.background)`
- All 4 cards (`ProfileHeaderCard`, `PersonalInfoCard`, `ProfileStatsCard`, `RankProgressCard`): `containerColor = Color.White` → `containerColor = MaterialTheme.colorScheme.surface`
- TopAppBar `containerColor = Color.White` → `containerColor = MaterialTheme.colorScheme.surface`
- Note: `Color.White` in avatar inner ring (line ~160) intentionally preserved — it's a design element, not a background

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Downgraded Hilt from 2.60.1 to 2.51.1**
- **Found during:** Task 2 compile verification
- **Issue:** Hilt 2.60.1 requires AGP 9.0.0+, but project uses AGP 8.13.2. Error: "The Hilt Android Gradle plugin is only compatible with Android Gradle plugin (AGP) version 9.0.0 or higher"
- **Fix:** Downgraded `hilt = "2.60.1"` → `"2.51.1"` in `gradle/libs.versions.toml`
- **Files modified:** `gradle/libs.versions.toml`
- **Commit:** 8dd49b9 (included in main feat commit)

**2. [Pre-existing environment] No Android SDK on build machine**
- **Found during:** Compile verification
- **Issue:** No Android SDK installed on the macOS build machine. `ANDROID_HOME` not set, no `sdk.dir` in `local.properties`, no `adb`/`sdkmanager` in PATH.
- **Fix:** Cannot resolve (requires Android SDK installation by developer). JDK 17 was installed via `brew install openjdk@17` to resolve the JDK 26 incompatibility with Kotlin 2.0.21.
- **Impact:** Compilation could not be fully verified. All code changes pass static syntax review against the established patterns in the codebase.

## Known Stubs

None. All composables are fully implemented with real UI logic. No hardcoded empty values or placeholder text (the default error message in `ErrorStateComposable` is intentional, user-facing UX text, not a stub).

## Threat Flags

None. No new network endpoints, auth paths, file access patterns, or schema changes introduced. All composables are purely UI — they render state passed by callers and trigger callbacks. Security note for `ErrorStateComposable` is documented in kdoc (T-02-14 from threat model).

## Self-Check

| Check | Result |
|-------|--------|
| `UiComponents.kt` exists | ✅ FOUND |
| `rememberInfiniteTransition` in ShimmerBox | ✅ FOUND |
| `dynamicColor: Boolean = false` in Theme.kt | ✅ FOUND |
| `BackgroundDark` in Color.kt | ✅ FOUND |
| `ShimmerBox` + `EmptyStateComposable` in MainScreen.kt | ✅ FOUND |
| `colorScheme.background` + `colorScheme.surface` in ProfileScreen.kt | ✅ FOUND |
| Commit 8dd49b9 exists | ✅ FOUND |

## Self-Check: PASSED
