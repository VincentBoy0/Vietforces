# Tech Stack

**Analysis Date:** 2026-07-24

## Overview
VietForces is a Vietnamese-language learning app with three components: an Android app (Kotlin/Compose), a Next.js web admin panel, and a Next.js landing page — all backed by Supabase (PostgreSQL + Auth + Realtime + Edge Functions) with OpenAI and Firebase Cloud Messaging integrations.

---

## Android App

**Location:** `app/`

- **Language:** Kotlin 2.0.21
- **Min SDK:** 24 | **Target/Compile SDK:** 36 | **JVM target:** 17
- **Build system:** Android Gradle Plugin 8.13.2 with Gradle version catalog at `gradle/libs.versions.toml`
- **Application ID:** `com.vietforces.app`

### UI
- **Jetpack Compose BOM** 2024.09.00 — fully Compose-based UI; no XML layouts
- **Material 3** (`androidx.compose.material3`) — component library
- **Material Icons Extended** 1.6.1
- **Navigation Compose** 2.7.7 — single-activity navigation; screen routes defined in `app/src/main/java/com/example/vietforces/navigation/Screen.kt`

### Dependency Injection
- **Hilt** 2.56.2 — full DI framework via `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`
- **KSP** 2.0.21-1.0.28 — annotation processing for Hilt and serialization
- **Hilt Navigation Compose** 1.2.0 — `hiltViewModel()` for Compose ViewModels
- DI modules: `app/src/main/java/com/example/vietforces/di/SupabaseModule.kt`, `AuthModule.kt`, `RepositoryModule.kt`, `GameModule.kt`

### Networking & Backend Client
- **Supabase Kotlin SDK** 3.7.0 (BOM) — modules: `auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`
- **Ktor** 3.5.1 — HTTP engine (`ktor-client-okhttp`)
- **OkHttp** 4.12.0 — underlying HTTP client
- Supabase URL and anon key injected at build time via `BuildConfig` fields from `local.properties`

### Async & Serialization
- **Kotlin Coroutines** 1.11.0 (`kotlinx-coroutines-android`)
- **Kotlinx Serialization JSON** 1.11.0 — used for data models and Supabase DTOs

### AI Integration
- **OpenAI** (gpt-4.1-mini default, configurable via `OPENAI_MODEL` in `local.properties`) — called via `AiManager` → `OpenAiClient` which routes through the `openai-proxy` Edge Function; never calls OpenAI directly from the device
- `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`

### Push Notifications
- **Firebase Cloud Messaging** 24.1.1 (`firebase-messaging-ktx`)
- Service: `app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt`
- Token management: `app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt`

### Background Work
- **WorkManager** 2.9.1 (`work-runtime-ktx`)

### Lifecycle
- **Lifecycle Runtime Compose** 2.6.1 — `collectAsStateWithLifecycle`

### Architecture Pattern
- MVVM: `ui/screens/` → `ui/viewmodel/` → `data/repository/` → `data/manager/`
- Repository pattern for all data access; ViewModels in `app/src/main/java/com/example/vietforces/ui/viewmodel/`

### Testing
- JUnit 4.13.2 (unit tests)
- AndroidX JUnit 1.3.0 + Espresso Core 3.7.0 (instrumented tests)
- Compose UI Test JUnit4 (UI tests)

---

## Web — Landing Page

**Location:** `web-landing/`

- **Framework:** Next.js 15.3.9 (App Router)
- **Language:** TypeScript 5
- **React:** 19.0.0
- **Styling:** Tailwind CSS 4 (PostCSS plugin: `@tailwindcss/postcss`)
- **Dev port:** 3001 (`next dev --port 3001`)
- **Pages:** `web-landing/src/app/layout.tsx`, `web-landing/src/app/page.tsx`
- **No Supabase integration** — purely static/presentational landing page
- **No ESLint config** — no linting configured (unlike web-admin)
- **Build:** `next build`; starts on port 3001 (`next start --port 3001`)

---

## Web — Admin Panel

**Location:** `web-admin/`

- **Framework:** Next.js 15.3.9 (App Router)
- **Language:** TypeScript 5
- **React:** 19.0.0
- **Styling:** Tailwind CSS 4 (PostCSS plugin: `@tailwindcss/postcss`)
- **Linting:** ESLint 9 + `eslint-config-next` 15.3.9

### Supabase Integration
- `@supabase/ssr` 0.12.3 — SSR-aware Supabase client (server components + server actions)
- `@supabase/supabase-js` ^2.49.0 — admin client (service role) for privileged operations
- Three client variants:
  - `web-admin/src/lib/supabase/client.ts` — browser client
  - `web-admin/src/lib/supabase/server.ts` — server component client (cookie-based session via `@supabase/ssr`)
  - `web-admin/src/lib/supabase/admin.ts` — service role admin client (server-only)

### Charts
- **Recharts** ^2.15.0 — analytics dashboard charts in `web-admin/src/app/admin/analytics/charts.tsx`

### Admin Sections
- `web-admin/src/app/admin/users/` — user management
- `web-admin/src/app/admin/vocabulary/` — vocabulary CRUD
- `web-admin/src/app/admin/daily-challenges/` — daily challenge management
- `web-admin/src/app/admin/analytics/` — analytics dashboards
- Server Actions in `web-admin/src/lib/actions/` (users, vocabulary, daily-challenges, analytics)

### Auth
- Cookie-based Supabase Auth session managed by `@supabase/ssr`
- Login at `web-admin/src/app/login/` with server actions in `web-admin/src/app/login/actions.ts`
- Unauthorized redirect at `web-admin/src/app/unauthorized/page.tsx`

---

## Backend — Supabase

**Location:** `supabase/`

### Platform
- **Supabase** — managed PostgreSQL + Auth + Realtime + Storage + Edge Functions
- **Local dev:** Supabase CLI; API port 54321, DB port 54322 (`supabase/config.toml`)

### Database
- **PostgreSQL** with Row Level Security (RLS) on all tables
- **pg_cron** — used for scheduled Edge Function invocations
- **11 migrations** in `supabase/migrations/`:
  - `001_initial_schema.sql` — users, user_progress, leaderboard, daily_challenges tables
  - `002_elo_function.sql` — ELO scoring function
  - `003_streak_function.sql` — streak logic
  - `004_leaderboard_week.sql` — weekly ELO reset
  - `005_daily_completions.sql` — daily completion tracking
  - `006_daily_bonus_elo.sql` — bonus ELO for daily challenges
  - `007_activity_feed.sql` — social activity feed
  - `008_admin_schema.sql` — admin role/privilege schema
  - `009_security_fixes.sql` — RLS policy hardening
  - `010_notif_preferences.sql` — per-user notification opt-out (`notif_streak_enabled`)
  - `011_handle_new_user_trigger.sql` — auto-creates user row on auth.users insert

### Auth
- **Supabase Auth** — email/password and Google OAuth
- Android uses `supabase-auth-kt` SDK; web-admin uses `@supabase/ssr` cookie sessions

### Realtime
- `supabase-realtime-kt` installed in Android app — used for leaderboard live updates

### Storage
- `supabase-storage-kt` installed in Android app — for avatar/media storage

### Edge Functions (Deno TypeScript)
All functions located in `supabase/functions/`:

| Function | Trigger | Purpose |
|---|---|---|
| `openai-proxy/index.ts` | HTTP POST (from Android app) | Forwards chat completion requests to OpenAI API; keeps `OPENAI_API_KEY` server-side |
| `generate-daily-challenge/index.ts` | pg_cron `0 0 * * *` (00:00 UTC daily) | Inserts one row into `daily_challenges` with 10 random vocabulary IDs and a game mode |
| `send-streak-reminder/index.ts` | pg_cron `0 19 * * *` (19:00 UTC daily) | Queries users who haven't practiced today and sends FCM push notifications |
| `refresh-streak-freeze/index.ts` | pg_cron `0 1 * * 1` (Monday 01:00 UTC) | Grants 1 streak freeze to users with `streak_freeze_count < 1` via `grant_streak_freeze` RPC |

- Runtime: Deno (latest Supabase-managed Deno)
- Dependencies imported from `https://esm.sh/` (no npm installs in functions)
- Auth: `CRON_SECRET` bearer token for cron-invoked functions; Supabase anon key for Android-invoked functions

---

## Tooling & CI

- **Gradle Wrapper** — `gradlew` / `gradlew.bat` for Android builds
- **Supabase CLI** — local dev and migration management
- **TypeScript** — `tsc --noEmit` type-check scripts in both web apps
- **ESLint** — configured only in `web-admin` (Next.js ESLint config)
- **No CI pipeline detected** — no `.github/workflows/` or CI config files found
- **`local.properties`** (git-ignored) — holds `OPENAI_API_KEY`, `OPENAI_MODEL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY` for Android development

---

*Stack analysis: 2026-07-24*
