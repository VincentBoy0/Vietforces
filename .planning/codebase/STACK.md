# Technology Stack

**Analysis Date:** 2026-07-22

## Languages

**Primary:**
- Kotlin 2.0.21 - All application source code (`app/src/main/java/com/example/vietforces/`)

**Secondary:**
- XML - Android resources (layouts, drawables, themes, manifest) in `app/src/main/res/`
- Kotlin DSL (`.kts`) - Build scripts: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`

## Runtime

**Environment:**
- Android SDK — minSdk 24 (Android 7.0 Nougat), targetSdk / compileSdk 36
- JVM target: Java 11 (`sourceCompatibility = JavaVersion.VERSION_11`)

**Package Manager:**
- Gradle 8.13 (wrapper: `gradle/wrapper/gradle-wrapper.properties`)
- Version catalog: `gradle/libs.versions.toml` — all dependency versions centralized here
- Lockfile: Not present (no `gradle.lockfile`)

## Frameworks

**Core UI:**
- Jetpack Compose BOM 2024.09.00 — declarative UI toolkit
  - `androidx.compose.ui` — core Compose UI
  - `androidx.compose.material3` — Material Design 3 components
  - `androidx.compose.material:material-icons-extended` 1.6.1 — extended icon set
  - Dynamic color theming enabled (Android 12+) via `dynamicDarkColorScheme` / `dynamicLightColorScheme`
  - Theme defined in `app/src/main/java/com/example/vietforces/ui/theme/`

**Navigation:**
- `androidx.navigation:navigation-compose` 2.7.7 — Compose Navigation with typed routes
- Route definitions: `app/src/main/java/com/example/vietforces/navigation/Screen.kt`
- Screens: bottom nav (Main, Settings, Profile, Performance) + modal screens (Notification, WritingPractice, LearningPath, Roleplay) + 7 game screens under `game/`

**Lifecycle & Activity:**
- `androidx.lifecycle:lifecycle-runtime-ktx` 2.6.1 — lifecycle-aware coroutine scopes
- `androidx.activity:activity-compose` 1.12.2 — `ComponentActivity` + Compose integration

**Concurrency:**
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.8.1 — coroutines for async AI calls
- All OpenAI network calls dispatched on `Dispatchers.IO` via `withContext`

**Networking:**
- `com.squareup.okhttp3:okhttp` 4.12.0 — HTTP client for OpenAI REST API calls
- No Retrofit or other HTTP abstraction; raw `OkHttpClient` used directly in `OpenAiClient`

**Testing:**
- JUnit 4.13.2 — unit tests (`app/src/test/`)
- `androidx.test.ext:junit` 1.3.0 — AndroidX JUnit extension
- `androidx.test.espresso:espresso-core` 3.7.0 — UI instrumentation tests (`app/src/androidTest/`)
- `androidx.compose.ui:ui-test-junit4` — Compose UI testing

## Key Dependencies

**Critical:**
- `okhttp` 4.12.0 — sole networking library; required for all AI features
- `kotlinx-coroutines-android` 1.8.1 — required for async AI calls and non-blocking UI
- `androidx.compose.bom` 2024.09.00 — pins all Compose library versions

**Infrastructure:**
- `androidx.core:core-ktx` 1.10.1 — Kotlin extensions for Android framework APIs
- Android Gradle Plugin (AGP) 8.13.2 — build toolchain (`gradle/libs.versions.toml`)
- Kotlin Compose compiler plugin 2.0.21 — required for Compose with Kotlin 2.x

## Configuration

**Environment:**
- API key and model are read from `local.properties` at build time (file is git-ignored)
  - `OPENAI_API_KEY=sk-...` — required to enable AI features
  - `OPENAI_MODEL=gpt-4.1-mini` — optional override (default: `gpt-4.1-mini`)
- Both values are baked into the APK as `BuildConfig.OPENAI_API_KEY` and `BuildConfig.OPENAI_MODEL`
- `buildFeatures { buildConfig = true }` enabled in `app/build.gradle.kts`
- `INTERNET` permission declared in `app/src/main/AndroidManifest.xml`

**Runtime persistence:**
- `android.content.SharedPreferences` via `PreferencesManager` singleton
  - Prefs file: `vietforces_prefs`
  - Must be initialized via `PreferencesManager.init(context)` before use (see `MainActivity.kt`)
  - Stores: ELO rating, streaks, game stats, AI toggles, profile fields, notifications, mascot position, roleplay sessions, encountered items (spaced repetition)

**Build:**
- `app/build.gradle.kts` — app module build config (AGP plugins, dependencies, BuildConfig fields)
- `build.gradle.kts` — root build file (plugin declarations only)
- `gradle/libs.versions.toml` — version catalog (single source of truth for all versions)
- `gradle.properties` — JVM args (`-Xmx2048m`), AndroidX flag, non-transitive R classes

## Platform Requirements

**Development:**
- Android Studio (any recent version supporting AGP 8.13.2 and Kotlin 2.x)
- JDK 11+
- `local.properties` with `OPENAI_API_KEY` set for AI features to function
- Internet access for AI calls

**Production:**
- Android 7.0+ (API 24) minimum
- Internet permission required
- **Warning:** API key is embedded in APK binary — not safe for public distribution; must be proxied server-side for production releases (noted in `OpenAiClient.kt`)

---

*Stack analysis: 2026-07-22*
