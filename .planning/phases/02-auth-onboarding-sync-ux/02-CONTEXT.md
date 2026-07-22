# Phase 2: Auth + Onboarding + Progress Sync + UX Polish — Context

**Gathered:** 2026-07-22
**Status:** Ready for planning
**Mode:** Auto-generated (autonomous mode)

<domain>
## Phase Boundary

Deliver a complete user journey from first launch to authenticated cloud-synced account:
1. **Login/Register screens** — email/password + Google OAuth (Supabase Auth)
2. **Guest mode** — user plays ≥1 full game before seeing auth prompts
3. **Onboarding flow** — 4-screen Welcome → Level → Goal → Name/Avatar
4. **Progress migration** — SharedPreferences → Supabase on first login
5. **Cloud sync** — sync after each game session ends
6. **UX Polish** — empty states, skeleton loaders, error+retry, dark mode on all screens

</domain>

<decisions>
## Implementation Decisions

### Auth Architecture
- Use `supabase-kt auth-kt` module (already in Gradle from Phase 1)
- Login screen: email/password with "Continue with Google" button
- Google OAuth: deep link scheme `vietforces://login` registered in AndroidManifest
- Session persistence: supabase-kt handles token refresh automatically
- Auth state as Kotlin Flow in a new `AuthRepository`

### Guest Mode
- On first launch, skip auth and show onboarding → game
- Store `isGuest = true` in SharedPreferences
- Show "Create Account" banner in Profile screen
- After guest completes first game session, show soft prompt to register
- On register: migrate local SharedPreferences data to Supabase

### Onboarding Flow (4 screens)
- Screen 1: Welcome — app logo, tagline, "Get Started" CTA
- Screen 2: Choose Level — Beginner / Intermediate / Advanced (stored in preferences)
- Screen 3: Choose Daily Goal — 5 / 10 / 15 words per day (stored in preferences)
- Screen 4: Set Name + Avatar selection (emoji avatars, no photo upload in this phase)
- Navigation: OnboardingActivity or Compose NavHost overlay
- Show only on first launch (flag in SharedPreferences: `onboarding_completed`)

### Progress Sync Strategy
- `ProgressRepository` with two sources: `LocalProgressSource` (SharedPreferences) + `RemoteProgressSource` (Supabase postgrest-kt)
- Sync trigger: on game session end (`GameViewModel.onGameComplete()`) AND on app foreground (if user is logged in)
- Conflict resolution: last-write-wins based on `updated_at` timestamp
- Sync function: upsert to `user_progress` table

### UX Polish Implementation
- Empty states: custom `EmptyStateComposable(illustration, message, cta)` composable
- Skeleton loaders: `ShimmerBox` composable using animated alpha (no external library)
- Error states: `ErrorStateComposable(message, onRetry)` composable
- Dark mode: `isSystemInDarkTheme()` already used in Theme.kt — wire `dynamicColor` and test each screen
- Loading states: `CircularProgressIndicator` replacement with skeleton in all list/card areas

### New Screens
- `LoginScreen.kt` — email/password + Google OAuth button
- `RegisterScreen.kt` — email/password + confirm password
- `OnboardingScreen.kt` — 4-step pager with progress indicator
- Navigation: add `AUTH_GRAPH` to existing `Screen.kt` navigation

### SharedPreferences Migration
- `MigrationService.kt` — reads all local managers (UserProgressManager, EncounteredItemsManager, SettingsManager) and upserts to Supabase
- Runs once on first login (flag: `migration_completed` in SharedPreferences)
- Non-blocking: runs in background coroutine, doesn't block game play

</decisions>

<code_context>
## Existing Code Insights

- `MainActivity.kt`: already has `@AndroidEntryPoint`, navigation is Compose NavHost
- `Screen.kt`: sealed class with existing routes — add `Login`, `Register`, `Onboarding`
- `BottomNavigation.kt`: shows 5 tabs — hide when on auth/onboarding screens
- `ProfileScreen.kt`: already exists, needs "Create Account" banner for guest users
- `UserProgressManager.kt`: local progress source — wrap in Repository pattern
- `PreferencesManager.kt`: stores all local settings — source for migration
- `EncounteredItemsManager.kt`: stores spaced repetition state — include in migration
- `Theme.kt`: already has dark mode support — needs testing and polish

</code_context>

<specifics>
## Specific Implementation Notes

- Google OAuth requires: (1) register app in Google Cloud Console, (2) add SHA-1 to Supabase Auth settings, (3) add redirect URL `vietforces://login` — plans should include setup instructions
- `supabase.auth.signInWithGoogle()` uses Custom Tabs in supabase-kt Android
- Keep `object AiManager` pattern but note that ViewModel injection via Hilt is preferred going forward
- All new screens use existing theme (VietForcesTheme wrapper)
- Skeleton shimmer: use `InfiniteTransition` with `animateFloat` — no Shimmer library needed

</specifics>

<deferred>
## Deferred

- Password reset email flow (AUTH-05) — include but mark as basic (just call `supabase.auth.resetPasswordForEmail()`)
- Spaced repetition weight migration (complex JSON) — simplified: migrate only ELO + streak + words_learned count
- Photo avatar upload — emoji avatars only in this phase
- Detailed session analytics — deferred to Phase 6 admin dashboard

</deferred>
