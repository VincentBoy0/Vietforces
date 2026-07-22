<!-- GSD:project-start source:PROJECT.md -->

## Project

**VietForces**

VietForces là ứng dụng Android học từ vựng tiếng Việt dạng gamified, nhắm đến người học nước ngoài và trẻ em. App dùng hình ảnh + nhiều game modes (ImageToWord, WordToImage, SyllableMatch, WordSearch, WordChain, FillBlank, SentenceOrder) kết hợp AI (OpenAI) cho roleplay và writing practice. Dự án đang được hoàn thiện thành sản phẩm đầy đủ với backend Supabase, web admin dashboard, landing page và đầy đủ social features.

**Core Value:** Người dùng học tiếng Việt hiệu quả qua gameplay thú vị — mỗi phiên chơi phải cảm thấy có tiến bộ rõ ràng và muốn quay lại ngày hôm sau.

### Constraints

- **Platform**: Android only (minSdk 24+) cho app chính
- **Backend**: Supabase — không tự host server
- **AI**: OpenAI API — giữ nguyên, không thay thế
- **Timeline**: Đồ án tốt nghiệp — cần hoàn thiện để bảo vệ
- **Codebase**: Giữ nguyên package name `com.example.vietforces`, không refactor căn bản
- **UI**: Giữ Jetpack Compose + theme hiện tại, không redesign toàn bộ

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Kotlin 2.0.21 - All application source code (`app/src/main/java/com/example/vietforces/`)
- XML - Android resources (layouts, drawables, themes, manifest) in `app/src/main/res/`
- Kotlin DSL (`.kts`) - Build scripts: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`

## Runtime

- Android SDK — minSdk 24 (Android 7.0 Nougat), targetSdk / compileSdk 36
- JVM target: Java 11 (`sourceCompatibility = JavaVersion.VERSION_11`)
- Gradle 8.13 (wrapper: `gradle/wrapper/gradle-wrapper.properties`)
- Version catalog: `gradle/libs.versions.toml` — all dependency versions centralized here
- Lockfile: Not present (no `gradle.lockfile`)

## Frameworks

- Jetpack Compose BOM 2024.09.00 — declarative UI toolkit
- `androidx.navigation:navigation-compose` 2.7.7 — Compose Navigation with typed routes
- Route definitions: `app/src/main/java/com/example/vietforces/navigation/Screen.kt`
- Screens: bottom nav (Main, Settings, Profile, Performance) + modal screens (Notification, WritingPractice, LearningPath, Roleplay) + 7 game screens under `game/`
- `androidx.lifecycle:lifecycle-runtime-ktx` 2.6.1 — lifecycle-aware coroutine scopes
- `androidx.activity:activity-compose` 1.12.2 — `ComponentActivity` + Compose integration
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.8.1 — coroutines for async AI calls
- All OpenAI network calls dispatched on `Dispatchers.IO` via `withContext`
- `com.squareup.okhttp3:okhttp` 4.12.0 — HTTP client for OpenAI REST API calls
- No Retrofit or other HTTP abstraction; raw `OkHttpClient` used directly in `OpenAiClient`
- JUnit 4.13.2 — unit tests (`app/src/test/`)
- `androidx.test.ext:junit` 1.3.0 — AndroidX JUnit extension
- `androidx.test.espresso:espresso-core` 3.7.0 — UI instrumentation tests (`app/src/androidTest/`)
- `androidx.compose.ui:ui-test-junit4` — Compose UI testing

## Key Dependencies

- `okhttp` 4.12.0 — sole networking library; required for all AI features
- `kotlinx-coroutines-android` 1.8.1 — required for async AI calls and non-blocking UI
- `androidx.compose.bom` 2024.09.00 — pins all Compose library versions
- `androidx.core:core-ktx` 1.10.1 — Kotlin extensions for Android framework APIs
- Android Gradle Plugin (AGP) 8.13.2 — build toolchain (`gradle/libs.versions.toml`)
- Kotlin Compose compiler plugin 2.0.21 — required for Compose with Kotlin 2.x

## Configuration

- API key and model are read from `local.properties` at build time (file is git-ignored)
- Both values are baked into the APK as `BuildConfig.OPENAI_API_KEY` and `BuildConfig.OPENAI_MODEL`
- `buildFeatures { buildConfig = true }` enabled in `app/build.gradle.kts`
- `INTERNET` permission declared in `app/src/main/AndroidManifest.xml`
- `android.content.SharedPreferences` via `PreferencesManager` singleton
- `app/build.gradle.kts` — app module build config (AGP plugins, dependencies, BuildConfig fields)
- `build.gradle.kts` — root build file (plugin declarations only)
- `gradle/libs.versions.toml` — version catalog (single source of truth for all versions)
- `gradle.properties` — JVM args (`-Xmx2048m`), AndroidX flag, non-transitive R classes

## Platform Requirements

- Android Studio (any recent version supporting AGP 8.13.2 and Kotlin 2.x)
- JDK 11+
- `local.properties` with `OPENAI_API_KEY` set for AI features to function
- Internet access for AI calls
- Android 7.0+ (API 24) minimum
- Internet permission required
- **Warning:** API key is embedded in APK binary — not safe for public distribution; must be proxied server-side for production releases (noted in `OpenAiClient.kt`)

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Naming Patterns

- Kotlin files use `PascalCase` matching the primary class/object/composable they contain
- Screen files: `[Name]Screen.kt` (e.g., `MainScreen.kt`, `FillBlankScreen.kt`)
- Component files: descriptive noun, `PascalCase` (e.g., `BottomNavigation.kt`, `DraggableMascot.kt`)
- Manager singletons: `[Domain]Manager.kt` (e.g., `AiManager.kt`, `UserProgressManager.kt`, `SettingsManager.kt`)
- Model files: plural or descriptive noun (e.g., `VocabularyItem.kt`, `AiModels.kt`, `GameMode.kt`)
- Repository: `[Domain]Repository.kt` (e.g., `VocabularyRepository.kt`)
- `PascalCase` for all: `VocabularyItem`, `GameMode`, `EloRankUtils`, `OpenAiClient`
- Singleton managers use `object` (not class): `object AiManager`, `object SettingsManager`
- Sealed classes for navigation: `sealed class Screen(val route: String)`
- Sealed classes for navigation items: `sealed class BottomNavItem(...)` with nested `object` entries
- `camelCase` for all functions: `loadFromPreferences()`, `gradeWriting()`, `getCurrentRank()`
- Boolean-returning functions prefixed with `is` or `has`: `isConfigured()`, `isAvailable()`
- Suspend functions follow the same naming — no `suspend` prefix in names
- Private helpers follow the same pattern: `sanitize()`, `parseMistakes()`, `extractContent()`
- `camelCase` for all local variables and properties
- Compose state: `var [name] by remember { mutableStateOf(...) }` pattern
- Private set with `private set` modifier when state is publicly readable but only privately mutable
- Constants use `const val UPPER_SNAKE_CASE`: `const val BASE_MASCOT_SIZE = 70f`
- Color constants in theme: `PascalCase` with descriptive prefix (`VietRed`, `GameModeImageToWord`)
- `PascalCase`, named after the concept they represent
- Data class for value holders: `data class VocabularyItem(...)`, `data class FillBlankGameState(...)`
- Fields within data classes use `camelCase`
- `snake_case` for route strings: `"main"`, `"writing_practice"`, `"game/image_to_word"`
- Game sub-routes use path prefix: `"game/[mode_id]"`
- Mode IDs also `snake_case`: `"image_to_word"`, `"fill_blank"`
- All public classes, data models, and singleton managers have a KDoc header comment
- Parameters documented with `@param` for public data classes and complex functions
- Computed properties have one-line KDoc describing the transformation

## Code Style

- No explicit formatter config (no `.editorconfig`, no `ktlint`, no `detekt` detected)
- Standard Kotlin code style followed manually
- Trailing lambda style used throughout Compose calls
- Wildcard imports used for Compose namespaces: `import androidx.compose.material3.*`, `import androidx.compose.runtime.*`
- Per-class imports for business logic code (non-wildcard)
- No dedicated lint/static analysis tool configured beyond Android Studio defaults
- `@OptIn(ExperimentalMaterial3Api::class)` applied at composable function level where needed
- 4-space indentation (Kotlin/Android standard)
- Closing `}` on its own line
- Blank line between logical sections within a composable
- Section separators as comment banners inside large files: `// ==================== §6.2 ... ====================`

## Import Organization

- None configured — full package paths used everywhere
- Wildcard imports used for Compose UI namespaces in Screen/Component files:
- Non-wildcard for business logic files (managers, models, clients)

## Error Handling

- All public `suspend` functions in `AiManager` return `AiCallResult<T>` (a sealed result type) instead of throwing
- `AiCallResult.Success(data)` on success; `AiCallResult.Error(message, isConfigError)` on failure
- Callers in UI receive a typed result and render accordingly — no try/catch in composables
- `try { ... } catch (e: Exception) { /* keep defaults */ }` pattern used in all `loadFromPreferences()` calls
- `runCatching { ... }` used for fire-and-forget save operations where failure is acceptable:
- `NotConfiguredException : IOException` thrown when API key is blank (callers catch this specifically)
- HTTP error codes converted to `IOException` with descriptive message
- Empty/blank AI content results in `IOException("AI trả về nội dung rỗng")`
- All network calls run on `Dispatchers.IO` via `withContext`
- One automatic retry on blank AI reply before substituting a hardcoded fallback message:
- Boolean flags like `isGrading`, `showResult`, `showGameOver` guard async operation transitions
- `LaunchedEffect` used to trigger side effects from state changes

## Logging

- Tag matches the class/object name: `Log.w("AiManager", "...")`, `Log.e("AiManager", "...", e)`
- `Log.w` for recoverable anomalies (e.g., blank AI reply triggering retry)
- `Log.e` with exception for unexpected failures
- No structured logging — plain string messages used

## Comments

- All singleton `object` managers have a multi-line KDoc at the top describing responsibilities
- All public data classes have KDoc with `@param` for each field
- Non-obvious computed properties have a one-line KDoc
- Inline section separators (`// ==== Section ====`) used in large files to separate feature areas
- Inline `//` comments for explaining intent in business logic (e.g., JSON sanitization, retry logic)
- `// TODO:` style comments not heavily used — only in placeholder/stub screens

## Function Design

- Composable screen functions can be long (100–300+ lines) when they contain local state and the full screen layout
- Business logic functions in managers are typically 10–40 lines
- Private parsing helpers are small (5–15 lines each)
- Composable screens receive navigation callbacks as lambda parameters: `onBackClick: () -> Unit`, `onGameModeClick: (GameMode) -> Unit`
- Default values used for optional callbacks: `onWritingPracticeClick: () -> Unit = {}`
- Data/config parameters go after callback parameters
- Manager functions use named parameters for AI config (e.g., `temperature: Double = 0.4`)
- Pure data functions return the model directly
- Fallible async operations return `AiCallResult<T>` (not nullable, not exceptions)
- Composables return `Unit` (implicit)

## Module / Object Design

- All data managers use Kotlin `object` (app-scoped singletons): `object AiManager`, `object VocabularyRepository`, `object OpenAiClient`
- State within singletons exposed as Compose-observable state using `mutableStateOf`/`mutableFloatStateOf` with `private set`
- No dependency injection framework — singletons are directly referenced
- No barrel files — classes imported directly by package path
- `companion object` used for static factory methods: `GameMode.fromId()`, `GameMode.getAllModes()`, `Screen.getGameRoute()`

## Compose Patterns

- Game state modelled as a single `data class` (e.g., `FillBlankGameState`) held with `by remember { mutableStateOf(...) }`
- Scope-based coroutine launches for AI/async work: `val scope = rememberCoroutineScope()`
- All colors used via named constants from `ui/theme/Color.kt` — no inline hex literals in composables
- `MaterialTheme` wrapper at app root in `MainActivity.kt`; dynamic color enabled for Android 12+
- `NavHost` + `composable()` routes defined inline in `VietforcesApp()` in `MainActivity.kt`
- `popUpTo` + `launchSingleTop` + `restoreState` used on all bottom-nav navigations to avoid stack buildup

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## System Overview

```text

```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `MainActivity` | App entry point; initializes all Managers; hosts NavController + Scaffold | `MainActivity.kt` |
| `VietforcesApp` | Root composable; NavHost wiring; bottom-nav visibility logic | `MainActivity.kt` |
| `Screen` | Sealed class enumerating all navigation routes | `navigation/Screen.kt` |
| `VocabularyRepository` | Static in-memory dataset of all vocabulary items and sentences | `data/repository/VocabularyRepository.kt` |
| `UserProgressManager` | Elo rating, streak, heatmap, game-mode stats; persists via PreferencesManager | `data/manager/UserProgressManager.kt` |
| `AiManager` | Builds prompts, calls OpenAiClient, parses typed AI responses; feature toggles | `data/manager/AiManager.kt` |
| `EncounteredItemsManager` | Spaced-repetition weighting for each game mode | `data/manager/EncounteredItemsManager.kt` |
| `SettingsManager` | Mascot size/text multipliers; persists via PreferencesManager | `data/manager/SettingsManager.kt` |
| `ProfileManager` | User name/phone/address; persists via PreferencesManager | `data/manager/ProfileManager.kt` |
| `NotificationManager` | In-app achievement/milestone notifications; persists via PreferencesManager | `data/manager/NotificationManager.kt` |
| `PreferencesManager` | Single SharedPreferences facade; JSON helpers for complex objects | `data/storage/PreferencesManager.kt` |
| `OpenAiClient` | OkHttp client for OpenAI Chat Completions API (gpt-4o-mini) | `data/remote/OpenAiClient.kt` |
| `MascotFeedbackManager` | Singleton that drives the floating mascot's messages across all screens | `ui/components/DraggableMascot.kt` |

## Pattern Overview

- Single `MainActivity` hosts all UI; no Fragments
- All Managers are Kotlin `object` singletons — no DI framework
- UI screens read state directly from manager singletons (`UserProgressManager.getEloRating()`)
- Compose `mutableStateOf` inside singletons propagates state reactively to UI
- Navigation is flat: `NavHost` in `VietforcesApp`; all routes defined in `Screen` sealed class
- AI calls are always wrapped in `AiCallResult<T>` (sealed class) so the UI never crashes on AI failure

## Layers

- Purpose: Render screens and respond to user input
- Location: `app/src/main/java/com/example/vietforces/ui/`
- Contains: Composable screen functions, shared UI components, theme (Color, Type, Theme)
- Depends on: Manager layer, VocabularyRepository, data models
- Used by: Nothing (top of the stack)
- Purpose: Business logic, state management, persistence orchestration
- Location: `app/src/main/java/com/example/vietforces/data/manager/`
- Contains: `UserProgressManager`, `AiManager`, `SettingsManager`, `ProfileManager`, `NotificationManager`, `EncounteredItemsManager`
- Depends on: `PreferencesManager`, `OpenAiClient`, `VocabularyRepository`, data models
- Used by: UI screens and components
- Purpose: Static vocabulary data source; domain models
- Location: `app/src/main/java/com/example/vietforces/data/repository/` and `data/model/`
- Contains: `VocabularyRepository` (in-memory list of ~100 `VocabularyItem`s + `SentenceItem`s), all domain model data classes
- Depends on: Nothing except `R.drawable.*` for image resource IDs
- Used by: Game screens directly, Manager layer
- Purpose: HTTP communication with OpenAI API
- Location: `app/src/main/java/com/example/vietforces/data/remote/`
- Contains: `OpenAiClient` (OkHttp, JSON serialization, response extraction)
- Depends on: OkHttp, `BuildConfig.OPENAI_API_KEY`, `BuildConfig.OPENAI_MODEL`
- Used by: `AiManager` only
- Purpose: Persist app state across sessions
- Location: `app/src/main/java/com/example/vietforces/data/storage/`
- Contains: `PreferencesManager` (SharedPreferences wrapper with JSON helpers for complex objects)
- Depends on: Android `Context` (initialized once at app start in `MainActivity.onCreate`)
- Used by: All Manager singletons

## Data Flow

### Game Answer Flow (e.g., ImageToWord correct answer)

### AI Writing Grading Flow

### Navigation Flow

- Kotlin `object` singletons with `mutableStateOf` properties drive reactive UI without ViewModel
- Compose observes singleton state directly (e.g., `NotificationManager.unreadCount`, `SettingsManager.mascotSizeMultiplier`)
- No StateFlow / LiveData / ViewModel used

## Key Abstractions

- Purpose: Core learning unit — Vietnamese word with image, classifier, distractors, category, difficulty
- Examples: `data/model/VocabularyItem.kt`
- Pattern: Immutable `data class`; `fullWord` and `syllables` computed properties used by multiple game modes
- Purpose: Registry of all 7 game modes with metadata (id, title, description, icon, color, hasHardMode)
- Examples: `data/model/GameMode.kt`
- Pattern: Enum with companion `fromId()` and `getAllModes()` factory; `id` strings match `Screen` route segments
- Purpose: Universal AI result wrapper — `Success(data)` or `Error(message, isConfigError)`
- Examples: `data/model/AiModels.kt`
- Pattern: All `AiManager` suspend functions return `AiCallResult<T>`; UI pattern-matches without try/catch
- Purpose: Type-safe navigation route registry
- Examples: `navigation/Screen.kt`
- Pattern: Each screen is an `object` with a `route` string; game routes prefixed with `"game/"`
- Purpose: Spaced repetition weight data per vocabulary item per game mode
- Examples: `data/manager/EncounteredItemsManager.kt`
- Pattern: SM-2-inspired `calculateWeight()` combining time-since-last, encounter count, and error rate
- Purpose: Codeforces-style Elo ranking (Newbie → Legendary Grandmaster) with Vietnamese display names
- Examples: `data/model/EloRank.kt`
- Pattern: Static utility object with rank lookup; used by `UserProgressManager` and `PerformanceScreen`

## Entry Points

- Location: `MainActivity.kt`
- Triggers: App launch (single launcher activity per `AndroidManifest.xml`)
- Responsibilities: Initialize `PreferencesManager`, load all Manager singletons from SharedPreferences, call `enableEdgeToEdge()`, set Compose content with `VietforcesTheme { VietforcesApp() }`
- Location: `MainActivity.kt`
- Triggers: Called from `MainActivity.setContent`
- Responsibilities: Create `NavController`, build `Scaffold` with conditional `BottomNavigation`, wire all `NavHost` composable routes, render `DraggableMascot` as top-layer overlay

## Architectural Constraints

- **Threading:** All AI HTTP calls execute on `Dispatchers.IO` inside `OpenAiClient.send()`; coroutines launched from UI via `rememberCoroutineScope()` in each game screen
- **Global state:** Six `object` singletons hold all app state (`UserProgressManager`, `AiManager`, `SettingsManager`, `ProfileManager`, `NotificationManager`, `EncounteredItemsManager`); each backed by `mutableStateOf` for Compose reactivity
- **No ViewModel:** State is not lifecycle-aware via ViewModel; all state lives in process-level singletons initialized in `MainActivity.onCreate()`
- **No DI framework:** All dependencies obtained directly via singleton references; no Hilt/Koin
- **API key at build time:** `BuildConfig.OPENAI_API_KEY` injected from `local.properties`; must not be published in production APK
- **Static vocabulary data:** `VocabularyRepository` is an in-memory list — no Room database, no network vocabulary fetch

## Anti-Patterns

### No ViewModel / lifecycle-aware state

### GameMode enum duplicated in two packages

### UI screens access managers and repository directly

## Error Handling

- `AiManager` wraps every AI call in `try/catch`; maps `NotConfiguredException` to `isConfigError=true` so UI can show a configuration hint rather than a generic error
- `PreferencesManager` accessed via `getPrefs()` which throws `IllegalStateException` if not initialized; all manager callers wrap in `try/catch`
- Game screens show `Toast` for unimplemented modes (fallback in `MainActivity.kt` `else` branch of `onGameModeClick`)

## Cross-Cutting Concerns

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.github/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
