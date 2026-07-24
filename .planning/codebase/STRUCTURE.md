# Directory Structure

**Analysis Date:** 2026-07-24

## Root Layout

```
vietforces/
├── app/                    # Android application (Kotlin, Jetpack Compose)
├── web-admin/              # Next.js 15 admin dashboard (TypeScript)
├── web-landing/            # Next.js 15 marketing landing page (TypeScript)
├── supabase/               # Backend: Supabase migrations + Edge Functions
├── .planning/              # GSD planning documents (phases, codebase maps)
├── .github/                # GitHub Actions workflows
├── build.gradle.kts        # Root Gradle build script
├── settings.gradle.kts     # Gradle settings (module declarations)
├── gradle.properties       # Gradle JVM / project-wide flags
├── local.properties        # Git-ignored: SUPABASE_URL, SUPABASE_ANON_KEY, OPENAI_API_KEY
├── gradlew / gradlew.bat   # Gradle wrapper scripts
└── README.md               # Project overview
```

## Android App (`app/`)

```
app/
├── build.gradle.kts        # App-level build config; reads local.properties into BuildConfig
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/example/vietforces/
    │   │   ├── MainActivity.kt               # Single Activity; NavHost + auth gate
    │   │   ├── VietForcesApplication.kt      # @HiltAndroidApp; WorkManager setup
    │   │   ├── VietForcesFirebaseMessagingService.kt  # FCM token + push handler
    │   │   ├── data/
    │   │   │   ├── manager/                  # Singleton in-memory state (SharedPrefs-backed)
    │   │   │   │   ├── AiManager.kt          # OpenAI conversation state
    │   │   │   │   ├── EncounteredItemsManager.kt  # Words seen in session
    │   │   │   │   ├── FCMTokenManager.kt    # FCM token cache + upload
    │   │   │   │   ├── NotificationManager.kt # Notification pref cache
    │   │   │   │   ├── ProfileManager.kt     # Username / avatar cache
    │   │   │   │   ├── SettingsManager.kt    # App settings (timezone, theme)
    │   │   │   │   └── UserProgressManager.kt  # ELO / streak local cache
    │   │   │   ├── model/                    # Pure data classes
    │   │   │   │   ├── AiModels.kt           # OpenAI request/response DTOs
    │   │   │   │   ├── EloRank.kt            # Rank tier enum (Newbie → Legendary GM)
    │   │   │   │   ├── GameMode.kt           # GameMode sealed class / enum
    │   │   │   │   ├── RoleplayScenario.kt   # AI roleplay scenario data
    │   │   │   │   ├── UserProgressDto.kt    # Supabase user_progress row DTO
    │   │   │   │   ├── UserSession.kt        # Current session state
    │   │   │   │   └── VocabularyItem.kt     # Word data class (id, word, classifier, drawableId)
    │   │   │   ├── remote/                   # Network clients
    │   │   │   │   ├── OpenAiClient.kt       # HTTP client for openai-proxy Edge Function
    │   │   │   │   └── RemoteProgressSource.kt  # Supabase Postgrest calls for progress sync
    │   │   │   ├── repository/               # Domain repositories (interface + impl)
    │   │   │   │   ├── AuthRepository.kt     # signIn / signUp / signOut / authState Flow
    │   │   │   │   ├── DailyChallengeRepository.kt  # Fetch today's challenge; award bonus
    │   │   │   │   ├── EloRepository.kt      # calculate_elo RPC wrapper
    │   │   │   │   ├── LeaderboardRepository.kt     # Global + weekly leaderboard queries
    │   │   │   │   ├── ProgressRepository.kt # Cloud progress sync (upsert / fetch)
    │   │   │   │   ├── SocialRepository.kt   # Follow / unfollow / search users
    │   │   │   │   ├── StreakRepository.kt   # update_streak RPC; streak history
    │   │   │   │   └── VocabularyRepository.kt  # Hardcoded 154-word list (object singleton)
    │   │   │   ├── service/
    │   │   │   │   └── MigrationService.kt   # One-time data migration at app start
    │   │   │   ├── storage/
    │   │   │   │   └── PreferencesManager.kt # SharedPreferences wrapper; init in Application
    │   │   │   └── worker/
    │   │   │       └── StreakDangerWorker.kt  # Hourly WorkManager job: streak danger alert
    │   │   ├── di/                           # Hilt dependency injection modules
    │   │   │   ├── AuthModule.kt             # Binds AuthRepositoryImpl → AuthRepository
    │   │   │   ├── GameModule.kt             # Game-scoped dependencies
    │   │   │   ├── RepositoryModule.kt       # Placeholder (auto-bound via @Inject)
    │   │   │   └── SupabaseModule.kt         # Singleton SupabaseClient (Auth+Postgrest+Realtime+Storage)
    │   │   ├── navigation/
    │   │   │   └── Screen.kt                 # Sealed class: 26 named routes
    │   │   └── ui/
    │   │       ├── components/               # Reusable Composables
    │   │       │   ├── BottomNavigation.kt   # Bottom nav bar (Main/Leaderboard/Profile/Settings)
    │   │       │   ├── DraggableMascot.kt    # Floating draggable rooster mascot
    │   │       │   ├── GameModeCard.kt       # Game mode selection card
    │   │       │   ├── RoosterMascot.kt      # Static rooster asset composable
    │   │       │   ├── StreakHeatmapComposable.kt  # GitHub-style streak heatmap
    │   │       │   └── UiComponents.kt       # Shared buttons, cards, loading indicators
    │   │       ├── screens/                  # Full-screen Composables
    │   │       │   ├── ActivityFeedScreen.kt
    │   │       │   ├── DailyChallengeScreen.kt
    │   │       │   ├── LeaderboardScreen.kt
    │   │       │   ├── LearningPathScreen.kt
    │   │       │   ├── LoginScreen.kt
    │   │       │   ├── MainScreen.kt         # Dashboard / home screen
    │   │       │   ├── NotificationScreen.kt
    │   │       │   ├── OnboardingScreen.kt
    │   │       │   ├── PerformanceScreen.kt
    │   │       │   ├── PlaceholderScreens.kt # Stub screens not yet implemented
    │   │       │   ├── ProfileScreen.kt
    │   │       │   ├── PublicProfileScreen.kt
    │   │       │   ├── RegisterScreen.kt
    │   │       │   ├── RoleplayScreen.kt     # AI-powered roleplay practice
    │   │       │   ├── SearchUsersScreen.kt
    │   │       │   ├── SettingsScreen.kt
    │   │       │   ├── WritingPracticeScreen.kt
    │   │       │   └── game/                 # Mini-game screens
    │   │       │       ├── FillBlankScreen.kt
    │   │       │       ├── GameCommonScreens.kt   # Shared game result / lobby screens
    │   │       │       ├── ImageToWordScreen.kt
    │   │       │       ├── SentenceOrderScreen.kt
    │   │       │       ├── SyllableMatchScreen.kt
    │   │       │       ├── WordChainScreen.kt
    │   │       │       ├── WordSearchScreen.kt
    │   │       │       └── WordToImageScreen.kt
    │   │       ├── theme/
    │   │       │   ├── Color.kt
    │   │       │   ├── Theme.kt              # VietforcesTheme (Material 3)
    │   │       │   └── Type.kt
    │   │       └── viewmodel/                # @HiltViewModel classes
    │   │           ├── ActivityFeedViewModel.kt
    │   │           ├── AuthViewModel.kt
    │   │           ├── DailyChallengeViewModel.kt
    │   │           ├── LeaderboardViewModel.kt
    │   │           ├── PublicProfileViewModel.kt
    │   │           └── SocialViewModel.kt
    │   └── res/                              # Android resources (drawables, mipmaps, values)
    ├── androidTest/                          # Instrumented tests (ExampleInstrumentedTest.kt)
    └── test/                                 # Unit tests (ExampleUnitTest.kt)
```

### Package naming convention
`com.example.vietforces` — root package. Sub-packages follow layer names exactly: `data`, `di`, `navigation`, `ui`.

### Where to add new Android code

| What to add | Where |
|-------------|-------|
| New screen | `ui/screens/NewScreen.kt` + route in `Screen.kt` + `composable {}` block in `MainActivity.kt` |
| New game mode | `ui/screens/game/NewGameScreen.kt` + `Screen.GameNewMode` in `Screen.kt` |
| New ViewModel | `ui/viewmodel/NewViewModel.kt` annotated `@HiltViewModel` |
| New repository | `data/repository/NewRepository.kt` (interface + `@Singleton @Inject constructor` impl) |
| New Hilt binding (interface) | `di/RepositoryModule.kt` |
| New local state manager | `data/manager/NewManager.kt` backed by `PreferencesManager` |
| New model/DTO | `data/model/NewModel.kt` |
| Shared UI component | `ui/components/NewComponent.kt` |

## Web Admin (`web-admin/`)

```
web-admin/
├── package.json
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
└── src/
    ├── app/
    │   ├── layout.tsx              # Root layout (Geist font, globals.css)
    │   ├── page.tsx                # / → redirect to /admin/vocabulary
    │   ├── globals.css             # Tailwind base + custom CSS variables
    │   ├── login/
    │   │   ├── page.tsx            # Login page (email/password form)
    │   │   └── actions.ts          # signInAction / signOutAction (Server Actions)
    │   ├── unauthorized/
    │   │   └── page.tsx            # 403 page for non-admin authenticated users
    │   └── admin/
    │       ├── layout.tsx          # Admin shell: sidebar nav + auth check
    │       ├── page.tsx            # Redirect to /admin/vocabulary
    │       ├── vocabulary/
    │       │   ├── page.tsx        # Word list table (Server Component)
    │       │   ├── new/page.tsx    # Create word form
    │       │   └── [id]/edit/page.tsx  # Edit word form
    │       ├── users/
    │       │   └── page.tsx        # User management table
    │       ├── analytics/
    │       │   ├── page.tsx        # Stats dashboard (Server Component)
    │       │   └── charts.tsx      # Recharts client components
    │       └── daily-challenges/
    │           └── page.tsx        # Challenge list + manual trigger
    ├── lib/
    │   ├── supabase/
    │   │   ├── server.ts           # createServerClient — SSR cookie auth (anon key)
    │   │   ├── client.ts           # createBrowserClient — client components (anon key)
    │   │   └── admin.ts            # createAdminClient — service_role (server-only)
    │   └── actions/
    │       ├── vocabulary.ts       # createWord / updateWord / deleteWord Server Actions
    │       ├── users.ts            # banUser / unbanUser / listUsers Server Actions
    │       ├── analytics.ts        # getAnalytics Server Action
    │       └── daily-challenges.ts # listChallenges / triggerGenerate Server Actions
    └── types/
        ├── vocabulary.ts           # Word, WordFormData types
        └── users.ts                # AdminUser type
```

### Where to add new web-admin code

| What to add | Where |
|-------------|-------|
| New admin page | `src/app/admin/<section>/page.tsx` (Server Component) + link in `admin/layout.tsx` sidebar |
| New data mutation | `src/lib/actions/<section>.ts` (Server Action with `'use server'`) |
| New Supabase query | Add to appropriate `src/lib/actions/*.ts` using `createClient()` (anon) or `createAdminClient()` (privileged) |
| New TypeScript type | `src/types/<domain>.ts` |
| New client chart/widget | Mark file with `'use client'`, place alongside its page (e.g. `analytics/charts.tsx`) |

## Web Landing (`web-landing/`)

```
web-landing/
├── package.json
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
└── src/
    └── app/
        ├── layout.tsx      # Root layout (Geist font, metadata, globals.css)
        ├── page.tsx        # Full single-page site (Navbar, Hero, Features, Screenshots, Download)
        └── globals.css     # Tailwind directives + dark background variables
```

Single-page static site. No components directory — all sections are inline JSX in `page.tsx`. No API calls or authentication. Add new landing sections directly to `src/app/page.tsx`.

## Supabase Backend (`supabase/`)

```
supabase/
├── config.toml             # Supabase CLI project configuration
├── README.md               # Local dev setup instructions
├── migrations/             # Ordered SQL migrations (idempotent)
│   ├── 001_initial_schema.sql       # Core tables: users, user_progress, leaderboard,
│   │                                #   daily_challenges, friendships, fcm_tokens
│   ├── 002_elo_function.sql         # calculate_elo() RPC + get_rank_tier() helper
│   ├── 003_streak_function.sql      # update_streak() RPC + streak_history table
│   ├── 004_leaderboard_week.sql     # reset_weekly_elo() + pg_cron Monday job
│   ├── 005_daily_completions.sql    # daily_completions table
│   ├── 006_daily_bonus_elo.sql      # award_daily_bonus() SECURITY DEFINER RPC
│   ├── 007_activity_feed.sql        # activity_events table + on_daily_completion_insert trigger
│   │                                #   + RLS patches: users public search, progress public read
│   ├── 008_admin_schema.sql         # is_admin column on users + public.words table
│   ├── 009_security_fixes.sql       # Security hardening patches
│   ├── 010_notif_preferences.sql    # notif_streak_enabled / notif_daily_enabled columns
│   └── 011_handle_new_user_trigger.sql  # handle_new_user() trigger on auth.users INSERT
└── functions/              # Deno Edge Functions
    ├── generate-daily-challenge/
    │   └── index.ts        # pg_cron daily 00:00 UTC: insert daily_challenges row
    ├── openai-proxy/
    │   └── index.ts        # On-demand: proxy OpenAI chat completions (hides API key)
    ├── refresh-streak-freeze/
    │   └── index.ts        # pg_cron Monday 01:00 UTC: call grant_streak_freeze() RPC
    └── send-streak-reminder/
        └── index.ts        # pg_cron daily 19:00 UTC: FCM push to users without today's practice
```

### Migration ordering rules
- Migrations are numbered sequentially (`001`, `002`, …). Always increment the counter.
- Each migration must be idempotent (`CREATE TABLE IF NOT EXISTS`, `CREATE OR REPLACE FUNCTION`, `ADD COLUMN IF NOT EXISTS`).
- New tables requiring service_role-only writes must use RLS with `TO service_role WITH CHECK (TRUE)`.

### Where to add new Supabase code

| What to add | Where |
|-------------|-------|
| New table or column | New migration `0NN_description.sql` |
| New RPC / trigger function | New migration or append to relevant existing migration |
| New scheduled job | New Edge Function under `functions/<name>/index.ts` + pg_cron SQL comment in file |
| New RLS policy | Migration file; follow existing `CREATE POLICY "table_action_scope"` naming |

---

*Structure analysis: 2026-07-24*
