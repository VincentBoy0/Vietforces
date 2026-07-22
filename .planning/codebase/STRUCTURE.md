# Codebase Structure

**Analysis Date:** 2026-07-22

## Directory Layout

```
vietforces/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/vietforces/
│   │       │   ├── MainActivity.kt               # App entry point + NavHost
│   │       │   ├── navigation/
│   │       │   │   └── Screen.kt                 # Sealed class: all nav routes
│   │       │   ├── data/
│   │       │   │   ├── manager/                  # Singleton business-logic objects
│   │       │   │   │   ├── AiManager.kt
│   │       │   │   │   ├── EncounteredItemsManager.kt
│   │       │   │   │   ├── NotificationManager.kt
│   │       │   │   │   ├── ProfileManager.kt
│   │       │   │   │   ├── SettingsManager.kt
│   │       │   │   │   └── UserProgressManager.kt
│   │       │   │   ├── model/                    # Domain data classes + enums
│   │       │   │   │   ├── AiModels.kt
│   │       │   │   │   ├── EloRank.kt
│   │       │   │   │   ├── GameMode.kt
│   │       │   │   │   ├── RoleplayScenario.kt
│   │       │   │   │   ├── UserSession.kt
│   │       │   │   │   └── VocabularyItem.kt
│   │       │   │   ├── remote/                   # OpenAI HTTP client
│   │       │   │   │   └── OpenAiClient.kt
│   │       │   │   ├── repository/               # In-memory vocabulary dataset
│   │       │   │   │   └── VocabularyRepository.kt
│   │       │   │   └── storage/                  # SharedPreferences facade
│   │       │   │       └── PreferencesManager.kt
│   │       │   └── ui/
│   │       │       ├── components/               # Reusable Compose components
│   │       │       │   ├── BottomNavigation.kt
│   │       │       │   ├── DraggableMascot.kt    # Also contains MascotFeedbackManager
│   │       │       │   ├── GameModeCard.kt
│   │       │       │   └── RoosterMascot.kt
│   │       │       ├── screens/                  # Top-level screen composables
│   │       │       │   ├── LearningPathScreen.kt
│   │       │       │   ├── MainScreen.kt
│   │       │       │   ├── NotificationScreen.kt
│   │       │       │   ├── PerformanceScreen.kt
│   │       │       │   ├── PlaceholderScreens.kt
│   │       │       │   ├── ProfileScreen.kt
│   │       │       │   ├── RoleplayScreen.kt
│   │       │       │   ├── SettingsScreen.kt
│   │       │       │   ├── WritingPracticeScreen.kt
│   │       │       │   └── game/                 # One file per game mode
│   │       │       │       ├── FillBlankScreen.kt
│   │       │       │       ├── GameCommonScreens.kt   # DifficultySelectionScreen, GameOverScreen
│   │       │       │       ├── ImageToWordScreen.kt
│   │       │       │       ├── SentenceOrderScreen.kt
│   │       │       │       ├── SyllableMatchScreen.kt
│   │       │       │       ├── WordChainScreen.kt
│   │       │       │       ├── WordSearchScreen.kt
│   │       │       │       └── WordToImageScreen.kt
│   │       │       └── theme/
│   │       │           ├── Color.kt              # Brand + game-mode colors
│   │       │           ├── Theme.kt              # VietforcesTheme (Material3)
│   │       │           └── Type.kt               # Typography
│   │       ├── res/
│   │       │   ├── drawable/                     # ~100 vocabulary images
│   │       │   │   ├── animal_001.jpg … animal_020.jpg
│   │       │   │   ├── body_001.jpg … body_002.jpg
│   │       │   │   ├── clothing_001.jpg … clothing_009.jpg
│   │       │   │   ├── food_001.png … food_020.jpg
│   │       │   │   ├── household_001.jpg … household_013.jpg
│   │       │   │   ├── kitchen_001.jpg … kitchen_012.jpg
│   │       │   │   ├── place_001.jpg … place_015.jpg
│   │       │   │   ├── school_001.jpg … school_012.jpg
│   │       │   │   ├── vehicle_001.jpeg … vehicle_006.jpg
│   │       │   │   └── ic_launcher_*.xml
│   │       │   ├── mipmap-*/                     # Launcher icons (hdpi→xxxhdpi)
│   │       │   ├── values/
│   │       │   │   ├── colors.xml
│   │       │   │   ├── strings.xml
│   │       │   │   └── themes.xml
│   │       │   └── xml/
│   │       │       ├── backup_rules.xml
│   │       │       └── data_extraction_rules.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Directory Purposes

**`data/manager/`:**
- Purpose: All application business logic; Kotlin `object` singletons that hold reactive state
- Contains: Progress tracking, AI orchestration, settings, profile, notifications, spaced-repetition
- Key files: `UserProgressManager.kt` (Elo/streaks), `AiManager.kt` (all AI feature calls), `EncounteredItemsManager.kt` (spaced repetition weights)

**`data/model/`:**
- Purpose: Domain data classes and enums — no business logic (except computed properties)
- Contains: `VocabularyItem`, `SentenceItem`, `UserProgress`, `GameMode`, `EloRank`, all AI result models, `RoleplayScenario`
- Key files: `VocabularyItem.kt` (core learning unit), `AiModels.kt` (all AI request/response types), `GameMode.kt` (7-mode enum with icons/colors)

**`data/remote/`:**
- Purpose: Network communication layer — single file, single responsibility
- Contains: `OpenAiClient.kt` — OkHttp client calling `https://api.openai.com/v1/chat/completions`; three public methods: `completeJson`, `completeJsonChat`, `completeChat`

**`data/repository/`:**
- Purpose: Static vocabulary dataset (single source of truth for all word/sentence data)
- Contains: `VocabularyRepository.kt` — one large `object` with `allVocabulary: List<VocabularyItem>` and sentence lists per category; ~100 words across 9 categories
- Note: File is 62KB — the largest in the project

**`data/storage/`:**
- Purpose: All SharedPreferences I/O; JSON serialization helpers for complex objects
- Contains: `PreferencesManager.kt` — must be initialized via `PreferencesManager.init(context)` in `MainActivity.onCreate()` before any manager calls

**`navigation/`:**
- Purpose: Centralized route definitions
- Contains: `Screen.kt` — sealed class; bottom-nav routes at top level, game routes under `"game/"` prefix

**`ui/components/`:**
- Purpose: Reusable Compose components shared across multiple screens
- Key files: `DraggableMascot.kt` (rooster mascot with drag, AI reactions, `MascotFeedbackManager` singleton), `BottomNavigation.kt`, `GameModeCard.kt`

**`ui/screens/`:**
- Purpose: Top-level screen composables (one file per screen)
- Key files: `MainScreen.kt` (home dashboard), `PerformanceScreen.kt` (Elo chart, heatmap), `RoleplayScreen.kt` (AI chat tutor), `WritingPracticeScreen.kt` (AI graded writing), `LearningPathScreen.kt` (AI personalized plan)

**`ui/screens/game/`:**
- Purpose: One composable file per game mode
- Contains: Each file defines a `GameState` data class + main `Screen` composable + supporting composables
- Shared: `GameCommonScreens.kt` provides `DifficultySelectionScreen` and `GameOverScreen` reused by all game modes that support difficulty selection

**`ui/theme/`:**
- Purpose: Material3 theme, brand colors, typography
- Key files: `Color.kt` (Vietnamese flag colors + 7 game-mode colors), `Theme.kt` (`VietforcesTheme` wrapper)

**`res/drawable/`:**
- Purpose: Vocabulary images referenced by `VocabularyItem.imageResId`
- Naming: `{category}_{NNN}.{jpg|jpeg|png}` — e.g., `animal_001.jpg`, `food_016.png`
- Categories: `animal` (20), `food` (20), `household` (13), `place` (15), `school` (12), `kitchen` (12), `clothing` (9), `vehicle` (6), `body` (2)

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/example/vietforces/MainActivity.kt`: App launch, manager init, NavHost
- `app/src/main/AndroidManifest.xml`: Single activity declaration, INTERNET permission

**Configuration:**
- `app/build.gradle.kts`: Dependency declarations, `BuildConfig` field injection for `OPENAI_API_KEY`
- `gradle.properties`: Kotlin/Gradle configuration
- `app/src/main/res/values/strings.xml`: App name string resource

**Core Logic:**
- `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt`: All vocabulary data
- `app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt`: Elo + streak logic
- `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`: All AI feature orchestration
- `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`: OpenAI HTTP client

**Navigation:**
- `app/src/main/java/com/example/vietforces/navigation/Screen.kt`: All route strings

**Testing:**
- No test files detected (`app/src/test/` and `app/src/androidTest/` not present)

## Naming Conventions

**Files:**
- Screen composables: `PascalCase` + `Screen.kt` suffix — e.g., `MainScreen.kt`, `ImageToWordScreen.kt`
- Manager singletons: `PascalCase` + `Manager.kt` suffix — e.g., `UserProgressManager.kt`
- Model files: `PascalCase.kt` named after primary class — e.g., `VocabularyItem.kt`, `AiModels.kt`
- Drawable resources: `{category}_{NNN}.{ext}` snake_case — e.g., `animal_001.jpg`

**Directories:**
- Source: `lowercase/` following Android conventions (`manager`, `model`, `remote`, `repository`, `storage`)
- UI: `screens/` for full screens, `components/` for reusable widgets, `theme/` for styling

**Classes/Objects:**
- Manager singletons: `object XxxManager`
- Screen composables: `@Composable fun XxxScreen(...)`
- Data classes: `data class Xxx(...)`
- Sealed navigation: `sealed class Screen(val route: String)` with `object` children

**Game State:**
- Each game screen declares a local `data class XxxGameState(...)` at the top of its file, e.g., `ImageToWordGameState` in `ImageToWordScreen.kt`

## Where to Add New Code

**New Vocabulary Words:**
- Add `VocabularyItem(...)` entries to `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt`
- Add corresponding drawable image to `app/src/main/res/drawable/` following `{category}_{NNN}.jpg` naming
- New categories need a new `category` string in `VocabularyItem`; no other registration required

**New Game Mode:**
1. Add enum entry to `com.example.vietforces.data.model.GameMode` in `data/model/GameMode.kt` (with id, title, description, icon, color)
2. Add enum entry to `com.example.vietforces.data.manager.GameMode` in `data/manager/EncounteredItemsManager.kt` (with key string)
3. Create `app/src/main/java/com/example/vietforces/ui/screens/game/XxxScreen.kt`
4. Add `object Xxx : Screen("game/xxx")` to `navigation/Screen.kt`
5. Wire route in `MainActivity.kt` `NavHost` block and `onGameModeClick` when-expression

**New Top-Level Screen:**
1. Create `app/src/main/java/com/example/vietforces/ui/screens/XxxScreen.kt`
2. Add `object Xxx : Screen("xxx")` to `navigation/Screen.kt`
3. Wire `composable(Screen.Xxx.route) { XxxScreen(...) }` in `MainActivity.kt` NavHost

**New Manager:**
1. Create `app/src/main/java/com/example/vietforces/data/manager/XxxManager.kt` as `object XxxManager`
2. Add `loadFromPreferences()` function; call it in `MainActivity.onCreate()` after `PreferencesManager.init()`
3. Add persistence keys and helpers to `PreferencesManager.kt`

**New AI Feature:**
1. Add data models to `data/model/AiModels.kt`
2. Add a `suspend fun xxx(...)` to `AiManager.kt` returning `AiCallResult<YourModel>`
3. Follow existing pattern: check toggle → build Vietnamese prompt → call `OpenAiClient.completeJson()` → parse JSON → return `AiCallResult.Success`

**New Reusable UI Component:**
- Create `app/src/main/java/com/example/vietforces/ui/components/XxxComponent.kt`

**New Colors:**
- Add to `app/src/main/java/com/example/vietforces/ui/theme/Color.kt`

## Special Directories

**`res/drawable/`:**
- Purpose: Vocabulary reference images (JPG/PNG/JPEG)
- Generated: No — manually added
- Committed: Yes — images are bundled in APK

**`res/mipmap-*/`:**
- Purpose: Adaptive launcher icon assets
- Generated: Yes (via Android Studio Image Asset wizard)
- Committed: Yes

**`.planning/codebase/`:**
- Purpose: Architecture analysis documents (this file)
- Generated: Yes (by GSD mapper)
- Committed: Yes

---

*Structure analysis: 2026-07-22*
