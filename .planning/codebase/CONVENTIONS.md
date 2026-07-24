# Coding Conventions

**Analysis Date:** 2026-07-24

## Android (Kotlin)

### Package Structure & Naming

- Root package: `com.example.vietforces`
- Layers are sub-packages: `ui.screens`, `ui.viewmodel`, `ui.components`, `ui.theme`, `data.repository`, `data.model`, `data.manager`, `data.remote`, `data.storage`, `di`, `navigation`
- File names match the primary class/object they contain, in PascalCase: `AuthViewModel.kt`, `DailyChallengeRepository.kt`
- Screen files are named `<Feature>Screen.kt` (e.g., `LoginScreen.kt`, `MainScreen.kt`)
- ViewModel files: `<Feature>ViewModel.kt`
- Repository implementations: `<Feature>RepositoryImpl.kt`, interface: `<Feature>Repository`
- DI modules: `<Name>Module.kt` (e.g., `RepositoryModule.kt`, `SupabaseModule.kt`)

### Class & Function Naming

- Classes: PascalCase (`AuthViewModel`, `DailyChallengeUiState`)
- Functions: camelCase (`signIn`, `loadChallenge`, `submitCompletion`)
- Constants / companion fields: camelCase private, `UPPER_SNAKE_CASE` for top-level constants
- Private backing StateFlow: `_uiState` (underscore prefix); public exposure: `uiState`
- Route strings in `Screen.kt`: snake_case string literals (e.g., `"auth/login"`, `"game/image_to_word"`)

### Architecture Pattern

MVVM + Repository + Hilt DI:

```
Screen (Composable)
  └── ViewModel (HiltViewModel + StateFlow)
        └── Repository (interface + Impl)
              └── SupabaseClient / remote source
```

- ViewModels hold `MutableStateFlow<UiState>` and expose `StateFlow<UiState>` via `.asStateFlow()`
- All async work in `viewModelScope.launch { }`
- Repositories return `Result<T>` — never throw to the ViewModel layer
- ViewModels use `.onSuccess { } / .onFailure { }` on `Result`

### UI State Pattern

Sealed classes are used for every feature's UI state:

```kotlin
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
```

- Always includes: `Loading`, at least one success state, `Error(val message: String)`
- `object` for states with no payload, `data class` for states with data
- Feature-specific states add extra variants (e.g., `DailyChallengeUiState.NoChallenge`, `DailyChallengeUiState.Completed`)

### DI Conventions (Hilt)

- ViewModels: `@HiltViewModel` + `@Inject constructor(...)`
- Singletons: `@Singleton` + `@Inject constructor(...)` on the `Impl` class
- Modules in `app/src/main/java/com/example/vietforces/di/`
- `@InstallIn(SingletonComponent::class)` for app-scoped bindings
- Interface bindings use `@Binds`; simple `@Inject constructor` singletons need no module entry

### Jetpack Compose Conventions

- All UI is Composable functions (no XML layouts)
- Screen composables receive navigation callbacks as lambdas: `onLoginSuccess: () -> Unit`
- ViewModels injected via `hiltViewModel()` with a default parameter
- State collected with `collectAsStateWithLifecycle()`
- Local UI state (text fields, toggles) uses `remember { mutableStateOf(...) }`
- Side effects in `LaunchedEffect(key)` — e.g., navigate on auth state change
- Wildcard imports are used for Compose packages (e.g., `import androidx.compose.material3.*`)

### Navigation

- Routes defined as `sealed class Screen(val route: String)` in `app/src/main/java/com/example/vietforces/navigation/Screen.kt`
- Route strings use `/` for hierarchy grouping: `"game/image_to_word"`, `"social/profile/{userId}"`
- Dynamic routes: `createRoute(userId: String)` companion method on the `Screen` object

### Comments

- KDoc `/** */` blocks on all public classes and interfaces
- Inline `//` comments reference ticket IDs (e.g., `// T-04-01`, `// DAILY-02`) that match the planning document
- Section separators: `// ── Section Name ─────────────────────` for long files

---

## TypeScript/React (Web)

### Project Structure

- Both `web-admin/` and `web-landing/` use **Next.js 15 App Router** with TypeScript
- Source lives in `src/`: `src/app/` (pages/layouts), `src/lib/` (actions, supabase clients), `src/types/` (shared types), `src/components/` (if present)
- Path alias `@/*` maps to `src/*` in both projects

### Component Patterns

- **Server Components** are the default — page files are `async function` that fetch data directly:

```tsx
export default async function VocabularyPage({ searchParams }) {
  const [{ words, total }, categories] = await Promise.all([
    listWords(category, page, 20),
    listCategories(),
  ])
  return <div>...</div>
}
```

- No `useState` / `useEffect` patterns observed in admin pages — all data fetching is server-side
- Client components not observed in `web-admin` (no `'use client'` directive found in explored files)
- `web-landing/src/app/page.tsx` is a single large static page component — no hooks, no data fetching

### Server Actions

- Placed in `src/lib/actions/<domain>.ts` — always start with `'use server'`
- Throw `Error(message)` on failure (e.g., `throw new Error(error.message)`)
- Call `revalidatePath('/admin/...')` after mutations
- Accept `FormData` for form-based mutations, typed arguments for read operations
- Use `createAdminClient()` (service role) for all admin mutations and reads

### Type Definitions

- Shared types live in `src/types/<domain>.ts` (e.g., `src/types/vocabulary.ts`, `src/types/users.ts`)
- Plain TypeScript `interface` for data shapes, `type` for aliases/unions
- Field names mirror database column names (snake_case): `image_url`, `created_at`, `elo_score`

### Naming

- Files: `kebab-case` for directories, `camelCase.ts` / `PascalCase.tsx` for components
- React components: PascalCase functions
- Server action functions: camelCase verbs (`listWords`, `createWord`, `updateWord`, `deleteWord`)
- Supabase client factories: `createClient()`, `createAdminClient()`

### Styling

- **TailwindCSS v4** (both `web-admin` and `web-landing`)
- Inline Tailwind utility classes; no separate CSS files observed
- Design tokens referenced as Tailwind classes (`text-muted-foreground`, `bg-primary`, `border-border`)
- `web-landing` uses inline `style={{}}` for complex gradients/dimensions alongside Tailwind

### TypeScript Configuration

- `strict: true` enabled in both projects (`web-admin/tsconfig.json`)
- `moduleResolution: "bundler"`, `jsx: "preserve"`, `incremental: true`
- Non-null assertion `!` used where env vars are guaranteed: `process.env.NEXT_PUBLIC_SUPABASE_URL!`

---

## Supabase / SQL

### Migration File Naming

- Numbered sequentially: `NNN_description.sql` (e.g., `001_initial_schema.sql`, `009_security_fixes.sql`)
- Stored in `supabase/migrations/`
- No timestamp prefix — purely sequential integers

### Migration Header Convention

Every migration file begins with a comment block:

```sql
-- ============================================================
-- Migration  : NNN_name
-- Date       : YYYY-MM-DD
-- Description: Short description of what this migration does
-- Idempotent : yes — CREATE TABLE IF NOT EXISTS throughout
-- ============================================================
```

- All migrations are idempotent: `CREATE TABLE IF NOT EXISTS`, `CREATE OR REPLACE FUNCTION/VIEW`

### RLS Policy Naming

Pattern: `tablename_action_scope`
- Examples: `users_select_own`, `users_update_own`, `progress_insert_own`
- `_own` suffix = `USING (id = auth.uid())` or `USING (user_id = auth.uid())`
- All tables have RLS enabled immediately after creation: `ALTER TABLE public.X ENABLE ROW LEVEL SECURITY`

### SQL Style

- Table names: `snake_case`, `public.` schema prefix
- Column names: `snake_case`
- Section separators: `-- ---------------------------------------------------------------------------` with `-- TABLE: name` label
- Constraint naming: `tablename_columnname_fk` pattern

### Edge Function Conventions

- All functions use `Deno.serve(async (req: Request) => { ... })`
- Import `@supabase/supabase-js` from `https://esm.sh/@supabase/supabase-js@2` (no npm installs)
- Every function defines `corsHeaders` as a top-level constant and handles `OPTIONS` preflight
- Environment secrets via `Deno.env.get("SECRET_NAME")`
- Auth checked at function entry; return `401` immediately on failure
- Error responses: `JSON.stringify({ error: message })` with appropriate HTTP status
- `console.log` for operational logging, `console.error` for errors

---

## Code Style Enforcement

### web-admin

- **ESLint**: `eslint-config-next` (configured via `package.json`'s `"lint": "next lint"`); no custom `.eslintrc` file — relies on Next.js built-in ESLint defaults
- **TypeScript**: `tsc --noEmit` via `"type-check"` script
- No Prettier configuration detected — formatting is unenforced

### web-landing

- No ESLint configured (no eslint dependency in `package.json`, no eslintrc)
- TypeScript strict mode enabled
- No Prettier configuration detected

### Android (Kotlin)

- No ktlint or detekt configuration found in the repository
- No `.editorconfig` detected
- Code style is consistent (likely IDE-enforced via Android Studio defaults), but no automated enforcement in CI

---

## Error Handling Patterns

### Android (Kotlin)

- Repositories always return `Result<T>` — never throw:
  ```kotlin
  return try {
      // supabase call
      Result.success(Unit)
  } catch (e: Exception) {
      Result.failure(e)
  }
  ```
- ViewModels unwrap with `.onSuccess { } / .onFailure { }` and update `_uiState` to an `Error` state
- User-facing error messages are mapped to friendly Vietnamese strings in the ViewModel (see `AuthViewModel.toFriendlyMessage()`)
- `AiManager` — returns `AiCallResult` with fallback message; never throws to UI
- `android.util.Log` used for debugging in manager/remote layer

### TypeScript Web (Next.js)

- Server actions throw `Error(message)` on failure
- Read actions (list/get) return empty arrays/null on error with `console.error` logging:
  ```ts
  if (error) {
    console.error('listWords error:', error)
    return { words: [], total: 0 }
  }
  ```
- Mutations throw to propagate errors to the calling page/form
- No global error boundary component observed

### Supabase Edge Functions

- `try/catch` wraps entire handler body
- Caught errors return `{ error: (error as Error).message }` with HTTP 500
- Specific guard failures (missing secret, bad auth) return early with 401/500 before the main try block

---

## State Management

### Android (Kotlin)

- **ViewModel-scoped `StateFlow`** is the primary state management mechanism
- `MutableStateFlow` private with `_` prefix; public `StateFlow` via `.asStateFlow()`
- `UserProgressManager` and `AiManager` are `object` singletons with `mutableStateOf` Compose state — used for app-wide data that doesn't need a ViewModel lifecycle
- `PreferencesManager` is a singleton for local persistence (SharedPreferences wrapper)
- No Redux/MVI — MVVM only
- `SharingStarted.WhileSubscribed(5000)` used when converting repository flows to StateFlow in ViewModels

### TypeScript Web (Next.js)

- **No client-side state management** — all pages are Server Components fetching data at render time
- URL search params used for filter/pagination state (e.g., `?category=...&page=...`)
- No `useState`, no Zustand, no Redux observed in the explored admin pages
- Form state handled by native HTML forms with `method="GET"` / Server Actions

---

*Convention analysis: 2026-07-24*
