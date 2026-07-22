---
phase: 01-supabase-foundation
plan: "01"
subsystem: infrastructure/di
tags: [supabase, hilt, di, ksp, kotlin-serialization, ktor]

dependency_graph:
  requires: []
  provides:
    - SupabaseClient (Hilt singleton, injectable across all phases)
    - VietForcesApplication (@HiltAndroidApp bootstrap)
    - BuildConfig.SUPABASE_URL
    - BuildConfig.SUPABASE_ANON_KEY
  affects:
    - app/src/main/java/com/example/vietforces/**/*.kt (any class using @AndroidEntryPoint or @Inject)

tech_stack:
  added:
    - supabase-kt 3.7.0 (BOM + postgrest-kt, auth-kt, realtime-kt, storage-kt)
    - Hilt 2.60.1 (hilt-android, hilt-android-compiler via KSP, hilt-navigation-compose 1.2.0)
    - KSP 2.0.21-1.0.28 (replaces kapt for annotation processing)
    - ktor-client-okhttp 3.5.1 (HTTP engine for supabase-kt)
    - kotlinx-serialization-json 1.11.0
  patterns:
    - "@HiltAndroidApp on Application class for Hilt component hierarchy"
    - "@Module @InstallIn(SingletonComponent) for app-scoped singletons"
    - "@Provides @Singleton for SupabaseClient DI binding"
    - "BuildConfig fields read from local.properties for secrets"

key_files:
  created:
    - app/src/main/java/com/example/vietforces/VietForcesApplication.kt
    - app/src/main/java/com/example/vietforces/di/SupabaseModule.kt
  modified:
    - gradle/libs.versions.toml
    - build.gradle.kts
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/example/vietforces/MainActivity.kt

decisions:
  - "KSP (not kapt) for Hilt annotation processing — kapt deprecated with Kotlin 2.x"
  - "supabase-kt BOM pins all module versions to 3.7.0 — no per-module version drift"
  - "ktor-client-okhttp auto-discovered by supabase-kt (only engine on classpath)"
  - "coroutines bumped 1.8.1→1.11.0 for Realtime websocket support per STACK.md"
  - "supabaseUrl/supabaseAnonKey extracted to Kotlin vals before BuildConfig interpolation — avoids inner-quote escaping bugs"

metrics:
  duration: "~15 minutes"
  completed: "2026-07-22"
  tasks_completed: 2
  files_created: 2
  files_modified: 5
---

# Phase 01 Plan 01: Supabase-kt + Hilt DI Infrastructure Summary

**One-liner:** Hilt 2.60.1 + supabase-kt 3.7.0 BOM wired into the Android project; singleton `SupabaseClient` injectable via `@Inject` anywhere in the app.

## What Was Built

### Task 1 — Version Catalog + Build Files
Added all version catalog entries, plugin declarations, and dependency references needed for the new libraries:

| Artifact | Version | Purpose |
|---|---|---|
| `io.github.jan-tennert.supabase:bom` | 3.7.0 | BOM pins all supabase module versions |
| `io.ktor:ktor-client-okhttp` | 3.5.1 | HTTP engine for supabase-kt |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | JSON codec for supabase responses |
| `com.google.dagger:hilt-android` | 2.60.1 | Hilt DI runtime |
| `com.google.dagger:hilt-android-compiler` (KSP) | 2.60.1 | Hilt code generator |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 | ViewModel injection in Compose nav |
| `com.google.devtools.ksp` plugin | 2.0.21-1.0.28 | Annotation processing (no kapt) |

- `gradle/libs.versions.toml` — 6 new version entries, 11 new library entries, 3 new plugin entries
- `build.gradle.kts` (root) — 3 new `alias(…) apply false` plugin declarations
- `app/build.gradle.kts` — ksp/hilt/serialization plugins, 2 BuildConfig fields, 10 new dependencies

### Task 2 — Application Class, DI Module, Manifest, MainActivity
- **`VietForcesApplication`** — `@HiltAndroidApp` class extending `Application`; triggers Hilt component hierarchy generation at compile time
- **`di/SupabaseModule`** — `@Module @InstallIn(SingletonComponent)` object with a single `@Provides @Singleton` function; calls `createSupabaseClient(url, key)` installing `Auth`, `Postgrest`, `Realtime`, `Storage`
- **`AndroidManifest.xml`** — `android:name=".VietForcesApplication"` added to `<application>` element
- **`MainActivity.kt`** — `@AndroidEntryPoint` annotation added; enables Hilt injection for any `@Inject`-annotated fields in activities/fragments/composables

## Verification Results

All structural checks passed:
```
✅ android:name=".VietForcesApplication" in AndroidManifest.xml
✅ createSupabaseClient in di/SupabaseModule.kt
✅ SUPABASE_URL + SUPABASE_ANON_KEY BuildConfig fields in app/build.gradle.kts
✅ @AndroidEntryPoint on MainActivity
✅ @HiltAndroidApp on VietForcesApplication
```

### Build Note (pre-existing environment issue)

`./gradlew assembleDebug` cannot be run in the current CI environment: only **JDK 26** is installed via Homebrew, and the Kotlin compiler embedded in Gradle 8.13 fails to parse the Java version string `26.0.1` (`JavaVersion.parse` only understands up to JDK 21). **This is a pre-existing machine configuration issue unrelated to the plan's code changes.** Build verification requires:
- JDK 17 or JDK 21 (Android Studio's bundled JBR 21 works out of the box)
- Or: `brew install openjdk@21` and `org.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` in `gradle.properties`

## Deviations from Plan

### Auto-fixed: BuildConfig field syntax (Rule 1 — Bug Prevention)

- **Found during:** Task 1 — writing BuildConfig fields
- **Issue:** Plan suggested inline `localProperties.getProperty("KEY", "")` inside Kotlin string template `"\"${...}\""` — this creates ambiguous inner quote escaping in `.kts` files
- **Fix:** Extracted `val supabaseUrl` and `val supabaseAnonKey` variables before the `android {}` block, matching the same pattern used for `openAiApiKey` / `openAiModel`
- **Files modified:** `app/build.gradle.kts`
- **Commit:** a5a934a (included in main task commit)

None — otherwise executed exactly as planned.

## Threat Model Compliance

| Threat | Status |
|---|---|
| T-01-SC: Maven dependency pinning | ✅ All versions pinned via `version.ref` in TOML; no `+` or dynamic ranges |
| T-01-01: Anon key in APK | Accepted — by design (RLS enforced in Plan 01-02) |
| T-01-02: local.properties secrets | git-ignored; BuildConfig reads at build time only |

## Known Stubs

None — this plan has no UI or data stubs. The `SupabaseClient` is a real singleton; credentials will be provided via `local.properties` before first use.

## Self-Check: PASSED

| Item | Status |
|---|---|
| `VietForcesApplication.kt` exists | ✅ |
| `di/SupabaseModule.kt` exists | ✅ |
| `android:name=".VietForcesApplication"` in manifest | ✅ |
| `@AndroidEntryPoint` on MainActivity | ✅ |
| `SUPABASE_URL` BuildConfig field | ✅ |
| `SUPABASE_ANON_KEY` BuildConfig field | ✅ |
| Commit `a5a934a` exists | ✅ |
| No kapt usage | ✅ (KSP only) |
