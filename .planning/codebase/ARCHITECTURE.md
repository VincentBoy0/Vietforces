<!-- refreshed: 2026-07-24 -->
# Architecture

**Analysis Date:** 2026-07-24

## System Overview

VietForces is a Vietnamese vocabulary-learning platform composed of four distinct components that share a single Supabase backend:

```
┌────────────────────────────────────────────────────────────────────┐
│                     Client Layer                                    │
├───────────────────────┬───────────────────┬────────────────────────┤
│   Android App         │   Web Admin        │   Web Landing          │
│   (Kotlin/Compose)    │   (Next.js 15)     │   (Next.js 15)         │
│   `app/`              │   `web-admin/`     │   `web-landing/`       │
└──────────┬────────────┴────────┬──────────┴────────────────────────┘
           │                     │
           ▼                     ▼
┌────────────────────────────────────────────────────────────────────┐
│                   Supabase Backend                                  │
│   Auth · Postgres (RLS) · Realtime · Storage · Edge Functions       │
│   `supabase/`                                                       │
└────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│   External Services                                                 │
│   OpenAI API (chat completions)  ·  Firebase Cloud Messaging (FCM) │
└────────────────────────────────────────────────────────────────────┘
```

## Android App Architecture

**Pattern:** MVVM + Repository + Hilt dependency injection, with Jetpack Compose as the sole UI toolkit.

### Layers

**UI Layer — `app/src/main/java/com/example/vietforces/ui/`**
- `screens/` — Full-screen Composable functions (one file per screen). Each screen accepts a ViewModel via `hiltViewModel()`.
- `screens/game/` — Seven mini-game screens (FillBlank, ImageToWord, WordToImage, SentenceOrder, SyllableMatch, WordSearch, WordChain).
- `components/` — Reusable Composables (BottomNavigation, DraggableMascot, GameModeCard, RoosterMascot, StreakHeatmap, UiComponents).
- `viewmodel/` — `@HiltViewModel` classes exposing `StateFlow`s consumed by screens. Current ViewModels: `AuthViewModel`, `DailyChallengeViewModel`, `LeaderboardViewModel`, `SocialViewModel`, `PublicProfileViewModel`, `ActivityFeedViewModel`.
- `theme/` — Material 3 theming (`Color.kt`, `Theme.kt`, `Type.kt`).

**Data Layer — `app/src/main/java/com/example/vietforces/data/`**
- `repository/` — Interfaces + Supabase-backed implementations for all domain areas: `AuthRepository`, `VocabularyRepository`, `ProgressRepository`, `StreakRepository`, `EloRepository`, `DailyChallengeRepository`, `LeaderboardRepository`, `SocialRepository`.
- `remote/` — Low-level network clients: `OpenAiClient` (wraps the Supabase `openai-proxy` Edge Function), `RemoteProgressSource` (Supabase Postgrest calls for progress sync).
- `model/` — Pure Kotlin data classes / sealed classes: `VocabularyItem`, `UserSession`, `UserProgressDto`, `GameMode`, `EloRank`, `RoleplayScenario`, `AiModels`.
- `manager/` — Singleton managers backed by `PreferencesManager` (SharedPreferences) for local state: `UserProgressManager`, `ProfileManager`, `SettingsManager`, `NotificationManager`, `FCMTokenManager`, `AiManager`, `EncounteredItemsManager`.
- `storage/` — `PreferencesManager`: SharedPreferences wrapper, initialized once in `VietForcesApplication.onCreate()`.
- `service/` — `MigrationService`: one-time data migration helper run at app start.
- `worker/` — `StreakDangerWorker`: `CoroutineWorker` scheduled hourly via WorkManager to fire local streak-danger notifications.

**DI Layer — `app/src/main/java/com/example/vietforces/di/`**
- `SupabaseModule` — provides a singleton `SupabaseClient` (Auth, Postgrest, Realtime, Storage plugins installed). Credentials read from `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`.
- `AuthModule` — binds `AuthRepositoryImpl` to `AuthRepository` interface.
- `GameModule` — provides game-related dependencies.
- `RepositoryModule` — empty module; repositories use `@Inject constructor` for Hilt auto-binding.

**Navigation — `app/src/main/java/com/example/vietforces/navigation/`**
- `Screen.kt` — Sealed class defining all 26 named routes (auth, onboarding, bottom-nav, game modes, social).
- Navigation host lives in `MainActivity.kt`; route decisions are driven by `AuthViewModel.authState` (`StateFlow<AuthState>`).

**Entry Point**
- `VietForcesApplication.kt` — `@HiltAndroidApp`, initializes `PreferencesManager` and enqueues `StreakDangerWorker` periodic job.
- `MainActivity.kt` — `@AndroidEntryPoint`, sets up Compose content, handles deep-links (Supabase OAuth), requests FCM notification permission.
- `VietForcesFirebaseMessagingService.kt` — FCM token refresh + incoming push handler.

### Data Flow (Android)

```
User Action (UI composable)
  → ViewModel (viewModelScope.launch)
    → Repository (suspend fun)
      → Supabase SDK (Postgrest / Auth / Realtime)
        ← Supabase Cloud (RPC / table read)
      ← Result<T> or Flow<T>
    ← StateFlow update (_uiState.value = …)
  ← Compose recomposition (collectAsStateWithLifecycle)
```

Local progress is also persisted to `PreferencesManager` via manager singletons for offline/fast access.

## Web Architecture

### Web Admin — `web-admin/`

**Pattern:** Next.js 15 App Router with React Server Components. Mutations go through Server Actions; all Supabase calls are server-side.

**Directory layout:**
```
web-admin/src/
├── app/
│   ├── layout.tsx              # Root layout (font, globals)
│   ├── page.tsx                # Redirect → /admin/vocabulary
│   ├── login/
│   │   ├── page.tsx            # Login form (client component)
│   │   └── actions.ts          # signIn / signOut Server Actions
│   ├── unauthorized/page.tsx   # Shown when is_admin = false
│   └── admin/
│       ├── layout.tsx          # Admin shell: sidebar nav + auth guard
│       ├── page.tsx            # Redirect → /admin/vocabulary
│       ├── vocabulary/
│       │   ├── page.tsx        # List all words (Server Component)
│       │   ├── new/page.tsx    # Create word form
│       │   └── [id]/edit/page.tsx  # Edit word form
│       ├── users/page.tsx      # User list + ban/unban actions
│       ├── analytics/
│       │   ├── page.tsx        # Stats dashboard (Server Component)
│       │   └── charts.tsx      # Client-side chart components
│       └── daily-challenges/page.tsx  # View / trigger daily challenges
├── lib/
│   ├── supabase/
│   │   ├── server.ts           # createServerClient (cookie-based SSR auth)
│   │   ├── client.ts           # createBrowserClient (client components)
│   │   └── admin.ts            # createAdminClient (service_role, server-only)
│   └── actions/
│       ├── vocabulary.ts       # CRUD Server Actions for public.words
│       ├── users.ts            # User management Server Actions
│       ├── analytics.ts        # Analytics query Server Actions
│       └── daily-challenges.ts # Daily challenge Server Actions
└── types/
    ├── vocabulary.ts           # Word / form TypeScript types
    └── users.ts                # User row types
```

**Auth guard:** `app/admin/layout.tsx` is a Server Component that calls `supabase.auth.getUser()` and checks `is_admin` on the `public.users` row. Unauthenticated users are redirected to `/login`; authenticated non-admins to `/unauthorized`.

**Supabase clients:**
- `server.ts` — cookie-based session for RSC / Route Handlers (anon key, user-scoped).
- `admin.ts` — service_role key for privileged mutations (never imported from `'use client'` files).

### Web Landing — `web-landing/`

A single-page Next.js 15 app with no backend calls. The entire site is one Server Component (`src/app/page.tsx`) rendered as a static page with Tailwind CSS utility classes. Sections: Navbar, Hero, Features, Screenshots, Download CTA.

## Backend Architecture

### Database Schema (`supabase/migrations/`)

| Table | Purpose | RLS |
|-------|---------|-----|
| `public.users` | Profile extension of `auth.users` | Own read/update; public username search |
| `public.user_progress` | ELO, streak, words_learned per user | Own write; public read for leaderboard |
| `public.leaderboard` | Denormalized leaderboard (Realtime-enabled) | Public read; service_role write only |
| `public.daily_challenges` | One row per calendar day | Public read; service_role insert |
| `public.daily_completions` | Per-user completion record per day | Own read; SECURITY DEFINER insert |
| `public.friendships` | Asymmetric follow graph (follower→following) | Own read/insert/delete |
| `public.fcm_tokens` | FCM push token per user | Own CRUD |
| `public.streak_history` | Per-(user, date) practice record | Own read; SECURITY DEFINER insert |
| `public.activity_events` | Social feed events (daily_completion, elo_milestone) | Own + following read; trigger insert |
| `public.words` | Admin-managed vocabulary entries | Public read; is_admin write |
| `public.notification_preferences` | Per-user notification opt-outs | Own CRUD |

**RLS strategy:** All tables have RLS enabled. Sensitive writes (streak updates, ELO calculation, daily bonus award, activity events) are routed through `SECURITY DEFINER` functions with `SET search_path = public` to prevent privilege escalation. Clients never send computed deltas — only raw inputs.

**Key Postgres functions (RPCs):**
- `calculate_elo(p_user_id, p_correct, p_total, p_time_ms)` — computes new ELO from game metrics (`002_elo_function.sql`)
- `update_streak(p_user_id, p_today_date)` — validates gap, applies freeze, writes `streak_history` (`003_streak_function.sql`)
- `award_daily_bonus(p_user_id, p_challenge_date)` — idempotent daily ELO grant (`006_daily_bonus_elo.sql`)
- `reset_weekly_elo()` — resets `leaderboard.weekly_elo`, called by pg_cron every Monday 00:00 UTC (`004_leaderboard_week.sql`)
- `grant_streak_freeze()` — grants 1 freeze to users with none, called by Edge Function (`003_streak_function.sql`)
- `handle_new_user()` — trigger on `auth.users INSERT`, auto-creates `public.users` row (`011_handle_new_user_trigger.sql`)

**Realtime:** `public.leaderboard` is added to `supabase_realtime` publication so the Android app receives live rank updates.

### Edge Functions (`supabase/functions/`)

All Edge Functions run on Deno and use `@supabase/supabase-js@2` from `esm.sh`.

| Function | Schedule | Purpose |
|----------|----------|---------|
| `generate-daily-challenge` | pg_cron 00:00 UTC daily | Selects 10 random vocabulary IDs + random game_mode, inserts into `daily_challenges`. Idempotent (SELECT before INSERT). Auth: `CRON_SECRET` header. |
| `send-streak-reminder` | pg_cron 19:00 UTC daily | Queries users with FCM tokens who haven't practiced today, sends FCM HTTP v1 push notifications. Auth: `CRON_SECRET` + Firebase service account JWT. |
| `refresh-streak-freeze` | pg_cron Monday 01:00 UTC | Calls `grant_streak_freeze()` RPC to top up streak freezes weekly. Uses service_role key. |
| `openai-proxy` | On-demand (Android client) | Proxies chat completion requests to OpenAI, attaching `OPENAI_API_KEY` from secrets. Prevents key exposure to Android client. |

## Data Flow

### Game Session (Android → Supabase)

```
1. User completes game screen (e.g. FillBlankScreen)
2. Screen calls ViewModel.onGameComplete(correct, total, timeMs)
3. ViewModel calls EloRepository.calculateElo(userId, correct, total, timeMs)
   → Supabase RPC: calculate_elo() [SECURITY DEFINER]
   ← new elo_score, rank_tier returned
4. ViewModel calls StreakRepository.updateStreak(userId, todayDate)
   → Supabase RPC: update_streak() [SECURITY DEFINER]
   ← streak_count, was_freeze_used returned
5. Results written to UserProgressManager (in-memory) + PreferencesManager (disk)
6. If daily challenge: ProgressRepository.awardDailyBonus()
   → Supabase RPC: award_daily_bonus() [SECURITY DEFINER]
   → Trigger fires: on_daily_completion_insert() → activity_events row
7. leaderboard table updated by calculate_elo() (service_role internally)
8. Android Realtime subscription fires → LeaderboardViewModel refreshes
```

### Admin Vocabulary CRUD (Web Admin → Supabase)

```
1. Admin fills form at /admin/vocabulary/new
2. Form submit → Server Action in lib/actions/vocabulary.ts
3. Action calls createAdminClient() (service_role key, server-only)
4. Supabase INSERT to public.words (bypasses RLS, service_role)
5. revalidatePath('/admin/vocabulary') → Server Component re-renders list
```

### Daily Challenge Generation (Scheduled)

```
1. pg_cron fires at 00:00 UTC
2. HTTP POST to generate-daily-challenge Edge Function with CRON_SECRET
3. Function selects 10 random IDs from 154-word pool (Fisher-Yates)
4. INSERT into daily_challenges (idempotent — skips if today already exists)
5. Android app fetches via DailyChallengeRepository.getTodayChallenge()
```

## Key Design Decisions

1. **SECURITY DEFINER RPCs for all sensitive writes** — ELO, streak, and daily bonus are computed server-side. Clients send only raw metrics; the database enforces all business logic. Prevents client-side score manipulation.

2. **Vocabulary is static on Android, managed dynamically in DB** — `VocabularyRepository` (`data/repository/VocabularyRepository.kt`) contains a hardcoded list of 154 `VocabularyItem` objects with local drawable references. The admin panel manages `public.words` (the cloud copy) separately. The Edge Function bridges both via a shared ID pool.

3. **Hilt `SingletonComponent` for all repositories** — All repositories and the `SupabaseClient` are application-scoped singletons. No per-screen or per-ViewModel scoping.

4. **Server Actions over API routes in web-admin** — All mutations use Next.js Server Actions (`'use server'`) rather than custom API routes, keeping Supabase client-side code minimal and auth handled entirely server-side.

5. **Asymmetric follow model** — `public.friendships` is a directed follow graph (not mutual friends), matching Codeforces-style social mechanics. RLS on `activity_events` uses this to scope the social feed.

6. **ELO tiers mirror Codeforces ranks** — `get_rank_tier()` SQL function and `EloRank.kt` both implement the same 10-tier ladder (Newbie → Legendary Grandmaster) for thematic consistency.

7. **PreferencesManager as singleton bootstrapped at Application.onCreate()** — All manager singletons (`UserProgressManager`, `ProfileManager`, etc.) depend on `PreferencesManager` being initialized before any Activity or Worker accesses them. The initialization order is enforced in `VietForcesApplication.onCreate()`.

## Anti-Patterns

### Direct manager mutation from screens
**What happens:** Some screens access `UserProgressManager` / `SettingsManager` directly without going through a ViewModel.
**Why it's wrong:** Bypasses the MVVM boundary, making UI testing difficult and creating implicit state coupling.
**Do this instead:** Route all state reads/writes through a ViewModel `StateFlow`; the ViewModel accesses managers internally.

### Hardcoded vocabulary in VocabularyRepository
**What happens:** `data/repository/VocabularyRepository.kt` is an `object` with 154 hardcoded `VocabularyItem` entries and local drawable IDs.
**Why it's wrong:** Adding new words requires an app update; the cloud `public.words` table and Android vocabulary are separately maintained and can drift.
**Do this instead:** Fetch vocabulary from Supabase `public.words` and cache locally (Room or DataStore), falling back to bundled assets offline.

## Error Handling

**Android strategy:** Repositories return `Result<T>`. ViewModels map failures to `sealed class UiState.Error(message)` and expose them via `StateFlow`. Screens render error states inline or via `Toast`.

**Web Admin:** Server Actions return typed `ActionResult` objects; error messages rendered in the page component.

**Edge Functions:** All handlers wrap logic in `try/catch` and return structured `{ error: message }` JSON with appropriate HTTP status codes.

---

*Architecture analysis: 2026-07-24*
