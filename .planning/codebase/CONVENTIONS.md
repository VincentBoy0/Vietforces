# Coding Conventions

**Analysis Date:** 2026-07-22

## Naming Patterns

**Files:**
- Kotlin files use `PascalCase` matching the primary class/object/composable they contain
- Screen files: `[Name]Screen.kt` (e.g., `MainScreen.kt`, `FillBlankScreen.kt`)
- Component files: descriptive noun, `PascalCase` (e.g., `BottomNavigation.kt`, `DraggableMascot.kt`)
- Manager singletons: `[Domain]Manager.kt` (e.g., `AiManager.kt`, `UserProgressManager.kt`, `SettingsManager.kt`)
- Model files: plural or descriptive noun (e.g., `VocabularyItem.kt`, `AiModels.kt`, `GameMode.kt`)
- Repository: `[Domain]Repository.kt` (e.g., `VocabularyRepository.kt`)

**Classes / Objects / Enums:**
- `PascalCase` for all: `VocabularyItem`, `GameMode`, `EloRankUtils`, `OpenAiClient`
- Singleton managers use `object` (not class): `object AiManager`, `object SettingsManager`
- Sealed classes for navigation: `sealed class Screen(val route: String)`
- Sealed classes for navigation items: `sealed class BottomNavItem(...)` with nested `object` entries

**Functions:**
- `camelCase` for all functions: `loadFromPreferences()`, `gradeWriting()`, `getCurrentRank()`
- Boolean-returning functions prefixed with `is` or `has`: `isConfigured()`, `isAvailable()`
- Suspend functions follow the same naming — no `suspend` prefix in names
- Private helpers follow the same pattern: `sanitize()`, `parseMistakes()`, `extractContent()`

**Variables & Properties:**
- `camelCase` for all local variables and properties
- Compose state: `var [name] by remember { mutableStateOf(...) }` pattern
- Private set with `private set` modifier when state is publicly readable but only privately mutable
- Constants use `const val UPPER_SNAKE_CASE`: `const val BASE_MASCOT_SIZE = 70f`
- Color constants in theme: `PascalCase` with descriptive prefix (`VietRed`, `GameModeImageToWord`)

**Types / Data Classes:**
- `PascalCase`, named after the concept they represent
- Data class for value holders: `data class VocabularyItem(...)`, `data class FillBlankGameState(...)`
- Fields within data classes use `camelCase`

**Route Strings:**
- `snake_case` for route strings: `"main"`, `"writing_practice"`, `"game/image_to_word"`
- Game sub-routes use path prefix: `"game/[mode_id]"`
- Mode IDs also `snake_case`: `"image_to_word"`, `"fill_blank"`

**KDoc Comments:**
- All public classes, data models, and singleton managers have a KDoc header comment
- Parameters documented with `@param` for public data classes and complex functions
- Computed properties have one-line KDoc describing the transformation

## Code Style

**Formatting:**
- No explicit formatter config (no `.editorconfig`, no `ktlint`, no `detekt` detected)
- Standard Kotlin code style followed manually
- Trailing lambda style used throughout Compose calls
- Wildcard imports used for Compose namespaces: `import androidx.compose.material3.*`, `import androidx.compose.runtime.*`
- Per-class imports for business logic code (non-wildcard)

**Linting:**
- No dedicated lint/static analysis tool configured beyond Android Studio defaults
- `@OptIn(ExperimentalMaterial3Api::class)` applied at composable function level where needed

**Spacing & Layout:**
- 4-space indentation (Kotlin/Android standard)
- Closing `}` on its own line
- Blank line between logical sections within a composable
- Section separators as comment banners inside large files: `// ==================== §6.2 ... ====================`

## Import Organization

**Order (observed pattern):**
1. Android / platform imports (`android.*`, `androidx.*`)
2. Compose imports (`androidx.compose.*`)
3. Navigation imports
4. Project-internal imports (`com.example.vietforces.*`)
5. Third-party libraries (`okhttp3.*`, `kotlinx.*`, `org.json.*`)

**Path Aliases:**
- None configured — full package paths used everywhere

**Wildcard Usage:**
- Wildcard imports used for Compose UI namespaces in Screen/Component files:
  ```kotlin
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.foundation.layout.*
  import com.example.vietforces.ui.screens.*
  ```
- Non-wildcard for business logic files (managers, models, clients)

## Error Handling

**Patterns:**

**Manager layer — never throw to UI:**
- All public `suspend` functions in `AiManager` return `AiCallResult<T>` (a sealed result type) instead of throwing
- `AiCallResult.Success(data)` on success; `AiCallResult.Error(message, isConfigError)` on failure
- Callers in UI receive a typed result and render accordingly — no try/catch in composables

**Singleton managers — safe preference access:**
- `try { ... } catch (e: Exception) { /* keep defaults */ }` pattern used in all `loadFromPreferences()` calls
- `runCatching { ... }` used for fire-and-forget save operations where failure is acceptable:
  ```kotlin
  runCatching { PreferencesManager.saveAiFeedbackEnabled(enabled) }
  ```

**Network layer (`OpenAiClient`):**
- `NotConfiguredException : IOException` thrown when API key is blank (callers catch this specifically)
- HTTP error codes converted to `IOException` with descriptive message
- Empty/blank AI content results in `IOException("AI trả về nội dung rỗng")`
- All network calls run on `Dispatchers.IO` via `withContext`

**Blank-reply retry pattern:**
- One automatic retry on blank AI reply before substituting a hardcoded fallback message:
  ```kotlin
  if (turn.reply.isBlank()) {
      turn = requestRoleplayTurn(messages)
  }
  if (turn.reply.isBlank()) {
      turn = turn.copy(reply = "Dạ, bạn nói lại giúp mình một chút nhé?")
  }
  ```

**Compose state guards:**
- Boolean flags like `isGrading`, `showResult`, `showGameOver` guard async operation transitions
- `LaunchedEffect` used to trigger side effects from state changes

## Logging

**Framework:** `android.util.Log`

**Patterns:**
- Tag matches the class/object name: `Log.w("AiManager", "...")`, `Log.e("AiManager", "...", e)`
- `Log.w` for recoverable anomalies (e.g., blank AI reply triggering retry)
- `Log.e` with exception for unexpected failures
- No structured logging — plain string messages used

## Comments

**When to Comment:**
- All singleton `object` managers have a multi-line KDoc at the top describing responsibilities
- All public data classes have KDoc with `@param` for each field
- Non-obvious computed properties have a one-line KDoc
- Inline section separators (`// ==== Section ====`) used in large files to separate feature areas
- Inline `//` comments for explaining intent in business logic (e.g., JSON sanitization, retry logic)
- `// TODO:` style comments not heavily used — only in placeholder/stub screens

## Function Design

**Size:**
- Composable screen functions can be long (100–300+ lines) when they contain local state and the full screen layout
- Business logic functions in managers are typically 10–40 lines
- Private parsing helpers are small (5–15 lines each)

**Parameters:**
- Composable screens receive navigation callbacks as lambda parameters: `onBackClick: () -> Unit`, `onGameModeClick: (GameMode) -> Unit`
- Default values used for optional callbacks: `onWritingPracticeClick: () -> Unit = {}`
- Data/config parameters go after callback parameters
- Manager functions use named parameters for AI config (e.g., `temperature: Double = 0.4`)

**Return Values:**
- Pure data functions return the model directly
- Fallible async operations return `AiCallResult<T>` (not nullable, not exceptions)
- Composables return `Unit` (implicit)

## Module / Object Design

**Singletons:**
- All data managers use Kotlin `object` (app-scoped singletons): `object AiManager`, `object VocabularyRepository`, `object OpenAiClient`
- State within singletons exposed as Compose-observable state using `mutableStateOf`/`mutableFloatStateOf` with `private set`
- No dependency injection framework — singletons are directly referenced

**Exports:**
- No barrel files — classes imported directly by package path
- `companion object` used for static factory methods: `GameMode.fromId()`, `GameMode.getAllModes()`, `Screen.getGameRoute()`

## Compose Patterns

**State hoisting:**
- Game state modelled as a single `data class` (e.g., `FillBlankGameState`) held with `by remember { mutableStateOf(...) }`
- Scope-based coroutine launches for AI/async work: `val scope = rememberCoroutineScope()`

**Theming:**
- All colors used via named constants from `ui/theme/Color.kt` — no inline hex literals in composables
- `MaterialTheme` wrapper at app root in `MainActivity.kt`; dynamic color enabled for Android 12+

**Navigation:**
- `NavHost` + `composable()` routes defined inline in `VietforcesApp()` in `MainActivity.kt`
- `popUpTo` + `launchSingleTop` + `restoreState` used on all bottom-nav navigations to avoid stack buildup

---

*Convention analysis: 2026-07-22*
