# Codebase Concerns

**Analysis Date:** 2026-07-22

---

## Security Considerations

### API Key Baked Into APK via BuildConfig

- **Risk:** The OpenAI API key is read from `local.properties` and injected into `BuildConfig` as a plain string via `buildConfigField` in `app/build.gradle.kts` (line 44–45). Any APK built with the key set is trivially reversible using `apktool` or `strings`.
- **Files:** `app/build.gradle.kts`, `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`
- **Current mitigation:** `local.properties` is git-ignored. Code comment explicitly acknowledges this is "fine for a demo/coursework build, but such an APK must never be published." The `Authorization` header value is masked in the source file shown to reviewers (`"******"`), but the actual runtime value comes from `BuildConfig.OPENAI_API_KEY`.
- **Recommendations:** For any production distribution, proxy all OpenAI calls through a server-side endpoint that holds the key. On-device key storage (e.g., Android Keystore) does not help here because the key must be sent in the HTTP header — the only safe approach is a backend proxy.

### Release Build Has Code Shrinking Disabled

- **Risk:** `isMinifyEnabled = false` in the `release` block (`app/build.gradle.kts`, line 45) means R8/ProGuard does not run. In addition to preventing dead-code removal and size reduction, this means the `BuildConfig.OPENAI_API_KEY` string is never obfuscated in the release APK, making extraction trivial.
- **Files:** `app/build.gradle.kts`
- **Current mitigation:** None.
- **Recommendations:** Enable `isMinifyEnabled = true` for release builds and configure `proguard-rules.pro` appropriately. This does not make the key safe for distribution but adds a meaningful barrier for casual extraction.

### Backup Rules Not Configured — User Data Exposed to Cloud Backup

- **Risk:** `android:allowBackup="true"` is set in `AndroidManifest.xml`. The `backup_rules.xml` file is the unmodified Android Studio template with all `<include>` / `<exclude>` entries commented out. The `data_extraction_rules.xml` file also contains only a `<!-- TODO -->` comment. This means all SharedPreferences (including AI toggle states and user progress stored in `vietforces_prefs`) are backed up to Google account cloud backup without restriction.
- **Files:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
- **Current mitigation:** None — the sample templates are left as placeholders.
- **Recommendations:** Either set `android:allowBackup="false"` or define explicit `<include>`/`<exclude>` rules to prevent unintended data restoration on new devices.

### Default `com.example` Application ID

- **Risk:** `applicationId = "com.example.vietforces"` in `app/build.gradle.kts` (line 30) uses the Android Studio default example namespace. This would collide with any other app using the same template. Publishing to the Play Store with a `com.example.*` package is rejected by Google.
- **Files:** `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- **Current mitigation:** App is not published.
- **Recommendations:** Rename to an owned reverse-domain identifier (e.g., `vn.edu.hcmus.vietforces`) before any distribution.

### No Certificate Pinning for OpenAI API

- **Risk:** `OpenAiClient` uses a plain `OkHttpClient` with no `CertificatePinner` configured (`app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`, lines 36–40). On devices with corporate proxies or user-installed CAs, traffic to `api.openai.com` can be intercepted, leaking the API key from request headers at runtime.
- **Files:** `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`
- **Current mitigation:** None.
- **Recommendations:** Add certificate pinning for `api.openai.com`, or (preferred) proxy calls through a backend that the app authenticates with separately.

---

## Tech Debt

### No ViewModel Layer — All Singletons Accessed Directly From Composables

- **Issue:** All state management is handled by seven `object` singletons: `UserProgressManager`, `AiManager`, `SettingsManager`, `ProfileManager`, `NotificationManager`, `EncounteredItemsManager`, and `PreferencesManager`. UI composables call these singletons directly — 68 direct accesses identified in `ui/` — instead of through a ViewModel. This violates the recommended Jetpack Compose architecture and makes screens impossible to unit-test in isolation.
- **Files:** `app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt`, `AiManager.kt`, `SettingsManager.kt`, `ProfileManager.kt`, `NotificationManager.kt`, `EncounteredItemsManager.kt`, all files in `app/src/main/java/com/example/vietforces/ui/screens/`
- **Impact:** Cannot write unit tests for any screen logic. State changes made in one screen are not reactively observed in another (screens use `remember(refreshKey)` workaround in `PerformanceScreen.kt` to force re-reads). Process death and configuration change handling is fragile.
- **Fix approach:** Introduce ViewModels that hold `StateFlow`/`MutableStateFlow` and delegate to the existing managers. Inject managers via constructor (or Hilt) for testability.

### Vocabulary Data Is a 1,328-Line Hardcoded Kotlin Object

- **Issue:** All vocabulary items, sentence items, and word chain data are hardcoded inside `VocabularyRepository.kt` (1,328 lines) as Kotlin list literals compiled into the `object`. The file itself documents: "In the future, this can be migrated to a database or loaded from JSON."
- **Files:** `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt`
- **Impact:** Adding or editing vocabulary requires a code change and app release. The entire vocabulary set is compiled into the DEX and loaded into the JVM heap at app startup. No pagination, lazy loading, or update mechanism is possible.
- **Fix approach:** Extract data to a bundled JSON asset file under `assets/`, load it lazily via a coroutine on first access, or migrate to a Room database with a pre-populated schema.

### `MascotFeedbackManager` Singleton Has an Unscoped `CoroutineScope`

- **Issue:** `MascotFeedbackManager` (`DraggableMascot.kt`, line 53–54) is a Kotlin `object` that creates `CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())` as a member. This scope is never cancelled because `object` singletons have no lifecycle. Individual AI `Job` instances are cancelled (`aiJob?.cancel()`), but the scope itself lives for the process lifetime and will accumulate if the singleton is accessed across activity recreations.
- **Files:** `app/src/main/java/com/example/vietforces/ui/components/DraggableMascot.kt`
- **Impact:** Potential for coroutine leaks if the scope ever fails to cancel jobs; the scope is never cleaned up on process termination beyond normal JVM GC.
- **Fix approach:** Move `MascotFeedbackManager` logic into a ViewModel with a `viewModelScope`, or limit the scope to the `DraggableMascot` composable's `rememberCoroutineScope()`.

### JSON Persistence Done Manually — No Schema Versioning

- **Issue:** All data serialization/deserialization in `PreferencesManager.kt` uses hand-written `JSONObject`/`JSONArray` code (~200 lines of bespoke serialization). There is no schema version field anywhere. If field names change or new required fields are added, old persisted data silently falls back to `optString("key", "")` defaults with no migration path.
- **Files:** `app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt`
- **Impact:** Data corruption or silent data loss after app updates that change model fields. Already observed: `EloHistoryEntry` stores a `change` field read with `optInt("change", 0)` — any entry persisted before that field existed will silently report a 0 change.
- **Fix approach:** Use `kotlinx.serialization` or `Gson` with explicit model classes; add a schema version key and a migration block in `loadUserSession()`.

### AI Coroutines Launched From `rememberCoroutineScope()` Inside Composables

- **Issue:** AI-dependent screens (`WritingPracticeScreen.kt`, `LearningPathScreen.kt`, `RoleplayScreen.kt`, `ImageToWordScreen.kt`, `FillBlankScreen.kt`) launch all AI coroutines via `rememberCoroutineScope()`. This scope is tied to the composable's lifecycle, meaning an in-flight AI request is cancelled if the user navigates away mid-call. There is no cancellation feedback to the user and no way to resume a pending call.
- **Files:** `app/src/main/java/com/example/vietforces/ui/screens/WritingPracticeScreen.kt`, `LearningPathScreen.kt`, `RoleplayScreen.kt`, `app/src/main/java/com/example/vietforces/ui/screens/game/ImageToWordScreen.kt`, `FillBlankScreen.kt`
- **Impact:** Partially-received AI results are discarded silently; `LearningPathScreen` re-fetches the AI plan on every `LaunchedEffect(Unit)` recomposition trigger, causing unnecessary API calls.
- **Fix approach:** Move AI calls into ViewModels with `viewModelScope` so calls survive recomposition and navigation events.

---

## Known Bugs

### Blank Roleplay Reply — Documented Retry Workaround

- **Symptoms:** The AI roleplay NPC occasionally returns an essentially empty JSON object with a blank `reply` field and no corrections.
- **Files:** `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt` (lines ~231–238)
- **Trigger:** Non-deterministic; more likely with the default `gpt-4.1-mini` model under load. The blank reply still counts as a `Success` result until the retry fires.
- **Workaround:** `AiManager.roleplayReply` retries once if `turn.reply.isBlank()`, and falls back to a hardcoded Vietnamese string `"Dạ, bạn nói lại giúp mình một chút nhé?"` if the retry also fails. This means users occasionally see a slight delay followed by a generic fallback line instead of contextual NPC dialogue.

### `SimpleDateFormat` Uses `Locale.getDefault()` for Date Keys

- **Symptoms:** Streak and heatmap calculations in `UserProgressManager.kt` (lines 84, 189) use `SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())`. On devices where the default locale uses non-Gregorian calendars (e.g., some Persian or Thai locale settings), date parsing may return unexpected results, breaking streak continuity.
- **Files:** `app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt`
- **Trigger:** Device locale set to a non-Gregorian calendar locale.
- **Workaround:** None currently. Fix: replace `Locale.getDefault()` with `Locale.US` (or `Locale.ROOT`) for internal date key formatting.

### `food_017.JPG` Has Uppercase Extension

- **Symptoms:** One image file `food_017.JPG` (uppercase extension) is present in the drawable folder. On case-sensitive Linux filesystems (used in CI/CD and some cloud build environments), `R.drawable.food_017` would not resolve to this file and would cause a compile-time resource error.
- **Files:** `app/src/main/res/drawable/food_017.JPG`
- **Trigger:** Building on a case-sensitive filesystem.
- **Workaround:** Rename the file to `food_017.jpg`.

---

## Performance Bottlenecks

### 50MB Raw JPEG/PNG Drawable Folder — No Density Buckets

- **Problem:** The `drawable/` directory contains 109 raw image files (91 `.jpg`, 12 `.jpeg`, 5 `.png`, 1 `.JPG`) totalling ~50MB. All images are placed in the undensity-qualified `drawable/` folder rather than `drawable-mdpi/`, `drawable-hdpi/`, etc. Every device downloads and installs the full 50MB regardless of screen density, and Android cannot select appropriately sized images at runtime.
- **Files:** `app/src/main/res/drawable/` (all image files)
- **Cause:** Images were added directly without a compression or density pipeline.
- **Improvement path:** Convert all images to WebP format (typically 25–35% smaller for these JPEG sources). Provide density-appropriate variants under `drawable-mdpi/`, `drawable-hdpi/`, `drawable-xhdpi/`, `drawable-xxhdpi/`. Consider using a bundled asset pack or on-demand download for less-frequently used categories.

### `VocabularyRepository` Object Fully Initialised at First Access

- **Problem:** `VocabularyRepository` is a Kotlin `object` with `allVocabulary`, `allSentences`, and `wordDataset` as top-level `val` properties initialized eagerly when the object is first touched. On lower-end devices this means several hundred `VocabularyItem` / `SentenceItem` data class allocations happen synchronously on whatever thread first accesses the object (typically the main thread during navigation).
- **Files:** `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt`
- **Cause:** Hardcoded list literal compiled into the `object` initializer.
- **Improvement path:** Migrate data to a JSON asset file and load via a `suspend` function on `Dispatchers.IO` before the first game screen opens.

---

## Fragile Areas

### `PreferencesManager` — Crash if Not Initialized Before Use

- **Files:** `app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt`
- **Why fragile:** `PreferencesManager.init(context)` must be called before any other method. If any manager's `loadFromPreferences()` is called before `PreferencesManager.init()`, `getPrefs()` throws `IllegalStateException`. The managers protect themselves with `try/catch` that silently swallows the exception and keeps defaults (`AiManager.loadFromPreferences()`, `UserProgressManager.loadFromPreferences()`), so initialization order bugs are invisible in testing.
- **Safe modification:** Always call `PreferencesManager.init(this)` as the first statement in `MainActivity.onCreate()` before any other manager initialisation. Never move manager init calls to a background thread or lazy delegate.
- **Test coverage:** No unit tests cover the initialization path or the fallback-to-defaults behaviour.

### AI Response JSON Parsing — No Schema Validation

- **Files:** `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`
- **Why fragile:** All AI response parsing uses `optString`/`optInt`/`optBoolean` with silent defaults. If the OpenAI model returns a structurally different JSON (e.g., wraps the object differently, or omits a required field due to a model update), the parser silently returns empty/default values rather than surfacing an error. The `sanitize()` function strips markdown fences but does not validate the resulting JSON structure.
- **Safe modification:** Add schema validation or at minimum log a warning when expected top-level keys are absent. Consider using a typed JSON library (`kotlinx.serialization`) so missing required fields cause explicit errors.
- **Test coverage:** No tests for any parser function in `AiManager`.

### Elo History Capped at 100 Entries — Silent Data Loss

- **Files:** `app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt` (lines 211–214)
- **Why fragile:** `updateEloHistory()` silently drops entries beyond 100 using `takeLast(100)`. A user who has practiced more than 100 sessions has their early Elo history permanently discarded on each save. The `PerformanceScreen` graph will show only the last 100 sessions with no indication that history was truncated.
- **Safe modification:** If truncation is intentional, document it in the UI. If historical data is important, store entries in a Room database with indexed queries.

---

## Missing Critical Features

### No User Authentication / Multi-Device Sync

- **Problem:** All user data (Elo, streaks, progress) is stored in `SharedPreferences` on-device only. There is no user account system, login flow, or backend sync. Data is lost on factory reset or new device.
- **Blocks:** Multi-device usage, leaderboard/social features, any server-side analytics.

### Unimplemented Game Modes Behind `else ->` Branch

- **Problem:** `MainActivity.kt` (line 155) has an `else ->` branch that shows a Toast `"Đang phát triển..."` for any `GameMode` enum entry not explicitly handled in the `when` block. The `PlaceholderScreen` component exists specifically for this purpose. If a new `GameMode` entry is added to the enum, it silently falls into the Toast branch with no compile-time error.
- **Files:** `app/src/main/java/com/example/vietforces/MainActivity.kt`, `app/src/main/java/com/example/vietforces/ui/screens/PlaceholderScreens.kt`
- **Blocks:** Future game mode additions are error-prone — a developer adding an enum entry will not get a compile warning that navigation is missing.
- **Fix approach:** Replace the `when` expression with a sealed hierarchy or use `when` with an exhaustive `is`-check so the Kotlin compiler enforces handling of all entries.

### Backup / Restore Rules Left as Uncommitted Template

- **Problem:** Both `backup_rules.xml` and `data_extraction_rules.xml` are unmodified Android Studio sample templates. No actual include/exclude rules have been defined.
- **Files:** `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
- **Blocks:** Predictable backup/restore behaviour; selective exclusion of sensitive prefs from cloud backup.

---

## Test Coverage Gaps

### No Application Logic Tests

- **What's not tested:** All business logic — Elo calculation in `UserProgressManager`, streak updating, AI response parsing in `AiManager`, JSON serialization/deserialization in `PreferencesManager`, spaced-repetition weighting in `EncounteredItemsManager`, word matching in `VocabularyRepository.matchesWord()`.
- **Files:** `app/src/test/java/com/example/vietforces/ExampleUnitTest.kt` (contains only the scaffold `2 + 2 == 4` assertion)
- **Risk:** Regressions in Elo math, date parsing, or vocabulary matching are undetected until manual testing.
- **Priority:** High — the Elo and streak logic are core to the learning loop.

### No UI / Compose Tests

- **What's not tested:** All Compose screens, navigation flows, game interaction states.
- **Files:** `app/src/androidTest/java/com/example/vietforces/ExampleInstrumentedTest.kt` (contains only package name assertion)
- **Risk:** UI regressions in game screens, navigation back-stack issues, or incorrect state display are undetectable in CI.
- **Priority:** Medium — at minimum, game result screen transitions and AI error state rendering should be covered.

### No Network / AI Integration Tests

- **What's not tested:** `OpenAiClient.send()` error paths (non-200 responses, malformed JSON, empty `choices`), `AiManager` fallback paths when AI is disabled or unconfigured.
- **Files:** `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`, `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`
- **Risk:** Silent regressions in error handling when OpenAI changes its error response format.
- **Priority:** Medium — mock `OkHttpClient` tests for error paths would prevent regressions without needing a live API key.

---

*Concerns audit: 2026-07-22*
