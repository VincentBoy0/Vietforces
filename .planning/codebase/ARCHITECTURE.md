<!-- refreshed: 2026-07-22 -->
# Architecture

**Analysis Date:** 2026-07-22

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                          UI Layer (Jetpack Compose)                          │
├──────────────────┬──────────────────┬──────────────────┬────────────────────┤
│  Bottom Nav      │  Core Screens    │  Game Screens    │  UI Components     │
│  Screens         │  (Main, Profile, │  (7 modes under  │  (DraggableMascot, │
│  `ui/screens/`   │   Settings,      │  `ui/screens/    │   BottomNavigation,│
│                  │   Performance,   │    game/`)       │   GameModeCard,    │
│                  │   Notification,  │                  │   RoosterMascot)   │
│                  │   Roleplay,      │                  │  `ui/components/`  │
│                  │   Writing,       │                  │                    │
│                  │   LearningPath)  │                  │                    │
└──────────┬───────┴────────┬─────────┴──────────────────┴────────────────────┘
           │                │
           ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Manager Layer (Singleton objects)                      │
│  UserProgressManager · AiManager · SettingsManager · ProfileManager         │
│  NotificationManager · EncounteredItemsManager                               │
│  `data/manager/`                                                             │
└──────────┬──────────────────────────────────────────────────────────────────┘
           │
     ┌─────┴──────────────────────────────────────┐
     ▼                                            ▼
┌─────────────────────────┐        ┌──────────────────────────────────────────┐
│  Repository / Models    │        │  Remote / Storage                        │
│  VocabularyRepository   │        │  OpenAiClient (OkHttp → OpenAI API)      │
│  `data/repository/`     │        │  PreferencesManager (SharedPreferences)  │
│  `data/model/`          │        │  `data/remote/` · `data/storage/`        │
└─────────────────────────┘        └──────────────────────────────────────────┘
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

**Overall:** Single-Activity, Compose-Navigation + Manual Singleton Managers (no ViewModel/MVVM)

**Key Characteristics:**
- Single `MainActivity` hosts all UI; no Fragments
- All Managers are Kotlin `object` singletons — no DI framework
- UI screens read state directly from manager singletons (`UserProgressManager.getEloRating()`)
- Compose `mutableStateOf` inside singletons propagates state reactively to UI
- Navigation is flat: `NavHost` in `VietforcesApp`; all routes defined in `Screen` sealed class
- AI calls are always wrapped in `AiCallResult<T>` (sealed class) so the UI never crashes on AI failure

## Layers

**UI Layer:**
- Purpose: Render screens and respond to user input
- Location: `app/src/main/java/com/example/vietforces/ui/`
- Contains: Composable screen functions, shared UI components, theme (Color, Type, Theme)
- Depends on: Manager layer, VocabularyRepository, data models
- Used by: Nothing (top of the stack)

**Manager Layer:**
- Purpose: Business logic, state management, persistence orchestration
- Location: `app/src/main/java/com/example/vietforces/data/manager/`
- Contains: `UserProgressManager`, `AiManager`, `SettingsManager`, `ProfileManager`, `NotificationManager`, `EncounteredItemsManager`
- Depends on: `PreferencesManager`, `OpenAiClient`, `VocabularyRepository`, data models
- Used by: UI screens and components

**Repository / Data Layer:**
- Purpose: Static vocabulary data source; domain models
- Location: `app/src/main/java/com/example/vietforces/data/repository/` and `data/model/`
- Contains: `VocabularyRepository` (in-memory list of ~100 `VocabularyItem`s + `SentenceItem`s), all domain model data classes
- Depends on: Nothing except `R.drawable.*` for image resource IDs
- Used by: Game screens directly, Manager layer

**Remote Layer:**
- Purpose: HTTP communication with OpenAI API
- Location: `app/src/main/java/com/example/vietforces/data/remote/`
- Contains: `OpenAiClient` (OkHttp, JSON serialization, response extraction)
- Depends on: OkHttp, `BuildConfig.OPENAI_API_KEY`, `BuildConfig.OPENAI_MODEL`
- Used by: `AiManager` only

**Storage Layer:**
- Purpose: Persist app state across sessions
- Location: `app/src/main/java/com/example/vietforces/data/storage/`
- Contains: `PreferencesManager` (SharedPreferences wrapper with JSON helpers for complex objects)
- Depends on: Android `Context` (initialized once at app start in `MainActivity.onCreate`)
- Used by: All Manager singletons

## Data Flow

### Game Answer Flow (e.g., ImageToWord correct answer)

1. User taps correct answer chip → `ImageToWordScreen` composable (`ui/screens/game/ImageToWordScreen.kt`)
2. Screen calls `UserProgressManager.recordCorrectAnswer(wordDifficulty, timeTaken)` (`data/manager/UserProgressManager.kt`)
3. `UserProgressManager` recalculates Elo, updates streak/heatmap, saves via `PreferencesManager.saveUserSession()` (`data/storage/PreferencesManager.kt`)
4. Screen calls `EncounteredItemsManager.recordResult(gameMode, itemId, correct=true)` (`data/manager/EncounteredItemsManager.kt`)
5. Screen calls `MascotFeedbackManager.showCorrect(word)` → triggers instant hardcoded reaction + optional AI call to `AiManager.mascotReact()` (`ui/components/DraggableMascot.kt`)
6. `AiManager` calls `OpenAiClient.completeJson()` → HTTP POST to `https://api.openai.com/v1/chat/completions` (`data/remote/OpenAiClient.kt`)
7. Response parsed into `MascotFeedback`; `MascotFeedbackManager.feedbackMessage` (Compose state) updates; `DraggableMascot` recomposes with new message

### AI Writing Grading Flow

1. User submits paragraph in `WritingPracticeScreen` → calls `AiManager.gradeWriting(topic, text)` (`ui/screens/WritingPracticeScreen.kt`)
2. `AiManager` checks `aiFeedbackEnabled` toggle; builds Vietnamese system/user prompt
3. `OpenAiClient.completeJson()` called with `response_format=json_object` (`data/remote/OpenAiClient.kt`)
4. JSON response parsed into `WritingFeedback` model; returned as `AiCallResult.Success<WritingFeedback>`
5. Screen receives result and renders score, `mistakes` list, `correctedVersion`

### Navigation Flow

1. User interacts with `MainScreen` → callback invoked (e.g., `onGameModeClick(GameMode.IMAGE_TO_WORD)`)
2. `VietforcesApp` (in `MainActivity.kt`) maps `GameMode` → `Screen` route → `navController.navigate(route)`
3. `NavHost` matches route → renders target composable
4. Back navigation: `onBackClick` → `navController.popBackStack()`

**State Management:**
- Kotlin `object` singletons with `mutableStateOf` properties drive reactive UI without ViewModel
- Compose observes singleton state directly (e.g., `NotificationManager.unreadCount`, `SettingsManager.mascotSizeMultiplier`)
- No StateFlow / LiveData / ViewModel used

## Key Abstractions

**`VocabularyItem`:**
- Purpose: Core learning unit — Vietnamese word with image, classifier, distractors, category, difficulty
- Examples: `data/model/VocabularyItem.kt`
- Pattern: Immutable `data class`; `fullWord` and `syllables` computed properties used by multiple game modes

**`GameMode` (enum):**
- Purpose: Registry of all 7 game modes with metadata (id, title, description, icon, color, hasHardMode)
- Examples: `data/model/GameMode.kt`
- Pattern: Enum with companion `fromId()` and `getAllModes()` factory; `id` strings match `Screen` route segments

**`AiCallResult<T>` (sealed class):**
- Purpose: Universal AI result wrapper — `Success(data)` or `Error(message, isConfigError)`
- Examples: `data/model/AiModels.kt`
- Pattern: All `AiManager` suspend functions return `AiCallResult<T>`; UI pattern-matches without try/catch

**`Screen` (sealed class):**
- Purpose: Type-safe navigation route registry
- Examples: `navigation/Screen.kt`
- Pattern: Each screen is an `object` with a `route` string; game routes prefixed with `"game/"`

**`EncounteredItem`:**
- Purpose: Spaced repetition weight data per vocabulary item per game mode
- Examples: `data/manager/EncounteredItemsManager.kt`
- Pattern: SM-2-inspired `calculateWeight()` combining time-since-last, encounter count, and error rate

**`EloRank` / `EloRankUtils`:**
- Purpose: Codeforces-style Elo ranking (Newbie → Legendary Grandmaster) with Vietnamese display names
- Examples: `data/model/EloRank.kt`
- Pattern: Static utility object with rank lookup; used by `UserProgressManager` and `PerformanceScreen`

## Entry Points

**`MainActivity.onCreate()`:**
- Location: `MainActivity.kt`
- Triggers: App launch (single launcher activity per `AndroidManifest.xml`)
- Responsibilities: Initialize `PreferencesManager`, load all Manager singletons from SharedPreferences, call `enableEdgeToEdge()`, set Compose content with `VietforcesTheme { VietforcesApp() }`

**`VietforcesApp()` composable:**
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

**What happens:** Manager singletons hold `mutableStateOf` state as `object` properties — state is tied to process lifetime, not Activity/Fragment lifecycle
**Why it's wrong:** State survives configuration changes (fine) but also creates untestable global singletons; manager initialization order in `MainActivity.onCreate()` is fragile
**Do this instead:** Introduce ViewModels scoped to each screen; inject managers via Hilt; expose state as `StateFlow`; reference `data/manager/UserProgressManager.kt` as migration target

### GameMode enum duplicated in two packages

**What happens:** `com.example.vietforces.data.model.GameMode` (the UI-facing enum with icons/colors) and `com.example.vietforces.data.manager.GameMode` (the spaced-repetition key enum) coexist; game screens must import the correct one
**Why it's wrong:** Name collision requires careful import management; easy to use the wrong `GameMode` type in game screens
**Do this instead:** Consolidate into a single `GameMode` enum in `data/model/GameMode.kt`; add a `key` property matching the spaced-repetition key strings

### UI screens access managers and repository directly

**What happens:** Game screens (`ui/screens/game/*.kt`) call `VocabularyRepository.allVocabulary`, `UserProgressManager.recordCorrectAnswer()`, `EncounteredItemsManager` all in the same composable
**Why it's wrong:** Composables carry business logic; hard to test; violates separation of concerns
**Do this instead:** Move game logic into ViewModels; composables receive only UI state and emit events

## Error Handling

**Strategy:** Defensive — AI errors are caught and surfaced as `AiCallResult.Error`; all manager `loadFromPreferences()` calls wrap in `try/catch` to handle uninitialized state

**Patterns:**
- `AiManager` wraps every AI call in `try/catch`; maps `NotConfiguredException` to `isConfigError=true` so UI can show a configuration hint rather than a generic error
- `PreferencesManager` accessed via `getPrefs()` which throws `IllegalStateException` if not initialized; all manager callers wrap in `try/catch`
- Game screens show `Toast` for unimplemented modes (fallback in `MainActivity.kt` `else` branch of `onGameModeClick`)

## Cross-Cutting Concerns

**Logging:** `android.util.Log` with tag `"AiManager"` for AI warnings/errors; no structured logging framework
**Validation:** Input validated at the AI layer (score clamped with `coerceIn`, blank reply retried once in `roleplayReply`)
**Authentication:** None — app is offline-first; OpenAI API key embedded in `BuildConfig` at build time

---

*Architecture analysis: 2026-07-22*
