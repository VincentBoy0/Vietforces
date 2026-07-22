# Technology Stack

**Project:** VietForces — Vietnamese Language Learning App
**Researched:** 2026-07-22
**Milestone:** Adding Supabase backend, Next.js web admin, social features to existing Android Kotlin/Compose app

---

## Context: What Exists vs. What's Being Added

| Layer | Existing | Adding |
|-------|----------|--------|
| Android UI | Jetpack Compose + Material3 | Auth screens, social, notifications UI |
| Android State | SharedPreferences + in-memory managers | ViewModel + StateFlow + Room + DataStore |
| Android Networking | Raw OkHttp (OpenAI only) | supabase-kt (auth, realtime, storage, postgrest) |
| Backend | None | Supabase (PostgreSQL + Auth + Realtime + Storage) |
| Web | None | Next.js admin dashboard + landing page |
| Notifications | None | FCM via Firebase |
| DI | None | Hilt |

---

## Recommended Stack

### 1. Android — Supabase Integration

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `io.github.jan-tennant:supabase-bom` | **3.7.0** | Supabase client BOM | Official Kotlin Multiplatform client; BOM pins all supabase-kt module versions together. Use BOM to avoid version conflicts across `auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt` |
| `io.github.jan-tennant:auth-kt` | (via BOM) | Supabase Auth | Email/password + Google OAuth. Handles token storage and refresh automatically |
| `io.github.jan-tennant:postgrest-kt` | (via BOM) | Database queries | Type-safe PostgREST wrapper for reading/writing users, progress, leaderboard |
| `io.github.jan-tennant:realtime-kt` | (via BOM) | Live leaderboard | WebSocket-based subscriptions for real-time ELO leaderboard updates |
| `io.github.jan-tennant:storage-kt` | (via BOM) | Vocabulary images | Upload/serve vocabulary images from Supabase Storage instead of bundled drawables |
| `io.ktor:ktor-client-okhttp` | **3.5.1** | HTTP engine for supabase-kt | supabase-kt requires a Ktor engine; use OkHttp-backed engine (not CIO) on Android — it reuses the OS TLS stack and handles Android's network quirks better than pure-Kotlin CIO |

**Gradle (libs.versions.toml additions):**
```toml
[versions]
supabase = "3.7.0"
ktor = "3.5.1"
coroutines = "1.11.0"
serialization = "1.11.0"

[libraries]
supabase-bom = { group = "io.github.jan-tennant", name = "supabase-bom", version.ref = "supabase" }
supabase-auth = { group = "io.github.jan-tennant", name = "auth-kt" }
supabase-postgrest = { group = "io.github.jan-tennant", name = "postgrest-kt" }
supabase-realtime = { group = "io.github.jan-tennant", name = "realtime-kt" }
supabase-storage = { group = "io.github.jan-tennant", name = "storage-kt" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
```

**app/build.gradle.kts:**
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
```

> **Confidence: HIGH** — versions sourced directly from supabase-kt 3.7.0 `libs.versions.toml` (GitHub API) and verified against Ktor's own latest release tag (2026-06-29).

---

### 2. Android — Dependency Injection

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `com.google.dagger:hilt-android` | **2.60.1** | DI container | Hilt is Google's first-class DI for Android; integrates with Jetpack ViewModel, WorkManager, and Navigation Compose. Supabase client singleton needs to be injected into ViewModels — Hilt makes this idiomatic. supabase-kt samples use Koin, but Hilt has better Compose + lifecycle integration |
| `com.google.dagger:hilt-android-compiler` | **2.60.1** | KSP annotation processor | Use KSP not kapt (kapt is deprecated in Kotlin 2.x) |
| `androidx.hilt:hilt-navigation-compose` | **1.2.0** | ViewModel injection in Compose | `hiltViewModel()` composable — required to use Hilt-provided ViewModels in Composable functions |
| `androidx.hilt:hilt-work` | **1.2.0** | WorkManager + Hilt | Inject dependencies into WorkManager workers (needed for notification workers) |

**libs.versions.toml:**
```toml
[versions]
hilt = "2.60.1"
hilt-androidx = "1.2.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-androidx" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hilt-androidx" }
hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hilt-androidx" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

> **Confidence: HIGH** — Hilt 2.60.1 release confirmed via GitHub API (google/dagger, released 2026-07-06).

---

### 3. Android — State Management

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | **2.10.0** | ViewModel base | Replace the current singleton manager pattern with ViewModel + StateFlow. Each screen gets a ViewModel; Hilt injects the Supabase client into it |
| `androidx.lifecycle:lifecycle-runtime-compose` | **2.10.0** | `collectAsStateWithLifecycle()` | Lifecycle-safe StateFlow collection in Compose — avoids collecting when app is backgrounded |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | **1.11.0** | Coroutines runtime | Update from existing 1.8.1 — needed for Supabase realtime subscription support and structured concurrency in ViewModels |

> **Confidence: HIGH** — lifecycle 2.10.0 sourced from supabase-kt's own `libs.versions.toml`; coroutines 1.11.0 same source.

---

### 4. Android — Local Storage (Migration)

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `androidx.datastore:datastore-preferences` | **1.1.7** | Replace SharedPreferences | DataStore is the AndroidX successor to SharedPreferences — async, coroutine-native, crash-safe. Migrate user settings, profile, AI toggles from `PreferencesManager` to DataStore |
| `androidx.room:room-runtime` | **2.7.x** | Vocabulary caching + offline | Vocabulary is currently an in-memory list; adding Room enables caching Supabase data locally for offline play. Also cache ELO history, streaks for offline display |
| `androidx.room:room-ktx` | **2.7.x** | Coroutine/Flow support | `Flow<List<T>>` return types from DAO — streams DB updates to ViewModels without manual refresh |
| `androidx.room:room-compiler` | **2.7.x** | KSP annotation processor | Must use KSP (not kapt) with Kotlin 2.x |

**Migration strategy:** Keep `PreferencesManager` / `SharedPreferences` alive in parallel during migration. New features go to DataStore. Existing ELO/streak data migrated in a one-time migration function at app start.

> **Confidence: MEDIUM** — Room 2.7.x and DataStore 1.1.7 based on Firebase SDK's `libs.versions.toml` showing `datastore = "1.1.7"` (Firebase's own dependency). Exact Room patch version unconfirmed (Maven Central blocked); use `2.7.+` or check `developer.android.com/jetpack/androidx/releases/room`.

---

### 5. Android — Image Loading

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `io.coil-kt.coil3:coil-compose` | **3.5.0** | Load images from Supabase Storage | Coil 3 is Compose-native, Kotlin Multiplatform ready, OkHttp-backed. Use for displaying vocabulary images served from Supabase Storage URLs. Replaces bundled drawables for remotely-hosted images |
| `io.coil-kt.coil3:coil-network-okhttp` | **3.5.0** | HTTP image fetching | Required alongside `coil-compose` for network image loading (Coil 3 separates network engine) |

**Do NOT use Glide or Picasso** — both are Java-centric View-system libraries. Coil 3 has first-class Compose + Kotlin Coroutines support.

> **Confidence: HIGH** — Coil 3.5.0 confirmed via GitHub API (coil-kt/coil, released 2026-06-10).

---

### 6. Android — Push Notifications (FCM)

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `com.google.firebase:firebase-bom` | **~34.x** | Firebase dependency management | BOM pins all Firebase library versions. Add `google-services` plugin to receive FCM tokens |
| `com.google.firebase:firebase-messaging-ktx` | (via BOM) | FCM push notifications | Daily learning reminders, streak warnings, friend challenge notifications. Ktx variant adds Kotlin-friendly extensions |
| `androidx.work:work-runtime-ktx` | **2.10.x** | Notification scheduling | WorkManager for reliable background sync (daily challenge refresh, progress upload). Use with `hilt-work` for dependency injection inside workers |

**FCM integration pattern for VietForces:**
- `FirebaseMessagingService` subclass handles token registration (save to Supabase `users.fcm_token` column)
- Supabase Edge Functions trigger FCM via Google's FCM HTTP v1 API for server-side push
- `WorkManager` with periodic constraints handles daily reminder scheduling as fallback if server push fails

**Do NOT use** `AlarmManager` for primary notification scheduling — WorkManager is Doze-mode aware and survives process death. AlarmManager exact alarms require `SCHEDULE_EXACT_ALARM` permission (restricted API on Android 12+).

> **Confidence: MEDIUM** — Firebase BOM version ~34.x is an estimate for mid-2026; exact version unconfirmed (Maven Central blocked). Verify at `firebase.google.com/support/release-notes/android`. Architecture guidance is HIGH confidence.

---

### 7. Android — Compose & Navigation Updates

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `androidx.compose:compose-bom` | **2025.06.00+** | Compose version management | Current project uses BOM 2024.09.00 — must upgrade for compatibility with Navigation 2.9.x and lifecycle 2.10.0. Check `developer.android.com/jetpack/compose/bom/bom-mapping` for exact mapping |
| `androidx.navigation:navigation-compose` | **2.9.x** | Type-safe navigation | Update from 2.7.7 — 2.8+ added type-safe routes (no more string paths), required for adding auth flow navigation with proper back-stack management |

> **Confidence: MEDIUM** — exact Compose BOM version for 2025.06 period unconfirmed; check the BOM mapping table before upgrading.

---

## Web Stack (Next.js Admin Dashboard + Landing Page)

### 8. Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `next` | **15.3.9** | Full-stack React framework | Use 15 (LTS track, tag `next-15-3`) over 16 (latest tag `16.2.11`). Next.js 15 is stable, well-documented, and has all features needed: Server Components, App Router, Server Actions. Next.js 16 introduces breaking changes not worth navigating for an academic project |
| `react` / `react-dom` | **19.x** | UI runtime | Ships with Next.js 15; do not install separately |
| `typescript` | **5.x** | Type safety | Use TypeScript project template (`--typescript`). Supabase generates TypeScript types from your schema via `supabase gen types` |

**Project init:**
```bash
npx create-next-app@15 web --typescript --tailwind --app --src-dir
```

> **Confidence: HIGH** — Next.js 15.3.9 confirmed as `next-15-3` stable tag on npm. Next.js 16.2.11 is `latest` but avoid for reasons above.

---

### 9. Supabase Web Client

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `@supabase/supabase-js` | **2.110.8** | Supabase client | Core client for all Supabase operations: auth, database queries, realtime, storage |
| `@supabase/ssr` | **0.12.3** | Server-side auth for Next.js | **Use this, NOT `@supabase/auth-helpers-nextjs`** (deprecated since 2024). `@supabase/ssr` works with Next.js App Router, Server Components, and middleware for cookie-based session management |

**Setup pattern (App Router):**
```typescript
// lib/supabase/server.ts — for Server Components & Server Actions
import { createServerClient } from '@supabase/ssr'
import { cookies } from 'next/headers'

// lib/supabase/client.ts — for Client Components
import { createBrowserClient } from '@supabase/ssr'
```

**Middleware** (`middleware.ts`) refreshes the session on every request using `@supabase/ssr`'s cookie utilities — required for persistent auth in the admin dashboard.

> **Confidence: HIGH** — versions confirmed via npm registry.

---

### 10. Styling

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `tailwindcss` | **4.3.3** | Utility-first CSS | v4 is the current standard (v3 is in maintenance mode). Key change: **no `tailwind.config.js`** — configure in CSS via `@theme` directive. CSS variables for design tokens. Significantly faster build times (Oxide engine) |
| `shadcn/ui` | CLI-based (no version) | Admin UI component library | Copy-paste components built on Radix UI primitives. Not a package — run `npx shadcn@latest init` then add components as needed. Generates accessible, Tailwind-v4-compatible components directly in your codebase. Use for: DataTable, Dialog, Form, Select, Badge, Card in admin dashboard |

**Tailwind v4 CSS config (no config file needed):**
```css
/* app/globals.css */
@import "tailwindcss";

@theme {
  --color-primary: oklch(0.7 0.2 142); /* VietForces green */
  --font-sans: "Geist", sans-serif;
}
```

**shadcn init:**
```bash
npx shadcn@latest init   # selects Tailwind v4 automatically
npx shadcn@latest add table form dialog badge card chart
```

**Do NOT use** Chakra UI, Ant Design, or MUI — all have heavier bundles, React-centric APIs that don't work with Server Components, and require Tailwind override wrestling.

> **Confidence: HIGH** — Tailwind 4.3.3 confirmed via npm. shadcn/ui is CLI-based; no package version to pin.

---

### 11. Data Fetching & State

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `@tanstack/react-query` | **5.101.4** | Client-side data fetching | For Client Components that need live data (leaderboard preview, analytics). Use with `@supabase/supabase-js` for caching Supabase responses. Server Components use `async/await` directly — no React Query needed there |
| `zustand` | **5.0.14** | Client state management | Lightweight global state for admin UI (selected filters, pagination state, notification toast queue). Do NOT use for server data — that's React Query's job |

> **Confidence: HIGH** — confirmed via npm.

---

### 12. Admin Dashboard — Data Display

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `@tanstack/react-table` | **8.21.3** | Data tables | Headless table logic for User Management, Vocabulary CRUD, Challenge management screens. Pair with shadcn `DataTable` component. Handles sorting, filtering, pagination without opinionated UI |
| `recharts` | **3.10.0** | Analytics charts | DAU, retention, game mode usage charts for the analytics dashboard. React-native, works with Server Components via dynamic import. Simpler than Chart.js or Victory |

> **Confidence: HIGH** — confirmed via npm.

---

### 13. Forms & Validation

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `react-hook-form` | **7.82.0** | Form state management | All admin forms (vocabulary CRUD, challenge creation). Minimal re-renders, native Zod integration |
| `zod` | **4.4.3** | Schema validation | Validate form inputs and Supabase response shapes. Note: Zod v4 has breaking changes from v3 (`.coerce` renamed, `z.input()` / `z.output()` changes) — initialize with v4 from the start |
| `@hookform/resolvers` | **3.x** | zod + react-hook-form bridge | `zodResolver(schema)` for type-safe form validation |

> **Confidence: HIGH** — confirmed via npm.

---

### 14. Supporting Web Libraries

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `lucide-react` | **1.25.0** | Icons | Default icon set for shadcn/ui; consistent across all admin components |
| `next-themes` | **0.4.6** | Dark/light mode | Admin dashboard should support dark mode; `next-themes` wraps the Tailwind `dark:` variant with system preference detection |
| `date-fns` | **4.x** | Date formatting | Analytics date ranges, streak calendar display. Tree-shakeable, no moment.js |

---

## Alternatives Considered & Rejected

| Category | Recommended | Rejected | Why Rejected |
|----------|-------------|----------|--------------|
| Android HTTP | Ktor (via supabase-kt) | Retrofit | supabase-kt already pulls Ktor as a transitive dep. Adding Retrofit creates a second HTTP client, increases APK size, and requires additional coroutine adapters. Use Ktor's `HttpClient` directly for any custom API calls if needed |
| Android DI | Hilt | Koin | supabase-kt samples use Koin, but Hilt has deeper Jetpack integration (hilt-work, hilt-navigation-compose) and is the official Android recommendation. Koin requires manual singleton management; Hilt's compiler catches injection errors at build time |
| Android image loading | Coil 3 | Glide / Picasso | Glide 5 still has a View-centric API. Coil 3 is purpose-built for Compose, uses Kotlin Coroutines natively, and supports multiplatform. Picasso is unmaintained for Compose |
| Android local storage | DataStore | SharedPreferences | SharedPreferences is synchronous I/O on the main thread — a lint warning in modern Android. DataStore is coroutine-native. Existing SharedPreferences data migrated via `SharedPreferencesMigration` |
| Web auth | `@supabase/ssr` | `@supabase/auth-helpers-nextjs` | auth-helpers is officially deprecated (announcement in 2024). `@supabase/ssr` is the documented replacement, works with App Router and Server Components |
| Web styling | Tailwind v4 | Tailwind v3 | v3 is in maintenance mode. v4 has better performance, CSS variables support, and no config file overhead. shadcn/ui now defaults to v4 in its CLI |
| Web framework | Next.js 15 | Next.js 16 | 16 is `latest` but still in active major release churn. 15 is LTS-tagged (`next-15-3`), extensively documented, and all required features (Server Components, App Router, Server Actions, Turbopack) are fully stable |
| Web charts | Recharts | Chart.js | Chart.js is Canvas-based with imperative React wrappers. Recharts is SVG-based, React-component API, better TypeScript support |
| Web tables | TanStack Table | AG Grid | AG Grid Community is too heavyweight for an admin dashboard with <10k rows. TanStack Table is headless and pairs perfectly with shadcn DataTable patterns |
| Web state | Zustand | Redux Toolkit | Redux Toolkit is over-engineered for a simple admin dashboard. Zustand provides the same predictable state with 1/10th the boilerplate |
| Notifications | FCM (Firebase) | OneSignal / Pusher | FCM is free, first-party Google service, deeply integrated with Android. OneSignal adds vendor lock-in. Pusher is overkill for simple push alerts |

---

## Installation

### Android (additions to existing project)

Add to `gradle/libs.versions.toml` and `app/build.gradle.kts` as shown in sections above. Also add to root `build.gradle.kts`:

```kotlin
// build.gradle.kts (root)
plugins {
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

Also requires:
- `google-services.json` from Firebase Console → placed in `app/`
- Supabase project URL + anon key → added to `local.properties`:
  ```
  SUPABASE_URL=https://xxxx.supabase.co
  SUPABASE_ANON_KEY=eyJ...
  ```

### Web (new project)

```bash
# Create Next.js 15 project
npx create-next-app@15 web --typescript --tailwind --app --src-dir --import-alias "@/*"
cd web

# Supabase
npm install @supabase/supabase-js@2.110.8 @supabase/ssr@0.12.3

# Data fetching & state
npm install @tanstack/react-query@5.101.4 zustand@5.0.14

# Tables & charts
npm install @tanstack/react-table@8.21.3 recharts@3.10.0

# Forms & validation
npm install react-hook-form@7.82.0 zod@4.4.3 @hookform/resolvers

# UI utilities
npm install lucide-react@1.25.0 next-themes@0.4.6 date-fns

# shadcn/ui (run after project init)
npx shadcn@latest init
npx shadcn@latest add button card dialog form input label select table badge
```

---

## Environment Variables

### Android (`local.properties` — git-ignored)
```
OPENAI_API_KEY=sk-...          # existing
OPENAI_MODEL=gpt-4.1-mini      # existing
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
```

### Web (`.env.local` — git-ignored)
```
NEXT_PUBLIC_SUPABASE_URL=https://xxxx.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJ...
SUPABASE_SERVICE_ROLE_KEY=eyJ...   # server-only, never NEXT_PUBLIC_
```

> **Security note:** `SUPABASE_SERVICE_ROLE_KEY` is for admin operations only (user ban, ELO reset). Never expose in client code. The Android app uses `SUPABASE_ANON_KEY` with Row Level Security policies — do not use service role key on Android.

---

## Confidence Assessment

| Area | Confidence | Source | Notes |
|------|------------|--------|-------|
| supabase-kt 3.7.0 | HIGH | GitHub API (supabase-community/supabase-kt releases) | Released 2026-07-20 |
| Ktor 3.5.1 | HIGH | GitHub API (ktorio/ktor) + supabase-kt libs.versions.toml | Released 2026-06-29 |
| Hilt 2.60.1 | HIGH | GitHub API (google/dagger releases) | Released 2026-07-06 |
| Coil 3.5.0 | HIGH | GitHub API (coil-kt/coil releases) | Released 2026-06-10 |
| Kotlin Coroutines 1.11.0 | HIGH | supabase-kt libs.versions.toml | Cross-verified |
| kotlinx.serialization 1.11.0 | HIGH | supabase-kt libs.versions.toml | Cross-verified |
| Lifecycle 2.10.0 | HIGH | supabase-kt libs.versions.toml | Cross-verified |
| DataStore 1.1.7 | MEDIUM | Firebase SDK libs.versions.toml (indirect source) | Check developer.android.com |
| Room 2.7.x | MEDIUM | Knowledge-based estimate | Maven Central blocked; verify before using |
| Firebase BOM ~34.x | MEDIUM | Estimate for mid-2026 | Check firebase.google.com/support/release-notes/android |
| Compose BOM 2025.06+ | MEDIUM | Estimate; current project uses 2024.09.00 | Check developer.android.com/jetpack/compose/bom |
| Next.js 15.3.9 | HIGH | npm registry (tag: next-15-3) | Stable LTS track |
| @supabase/supabase-js 2.110.8 | HIGH | npm registry | — |
| @supabase/ssr 0.12.3 | HIGH | npm registry | — |
| Tailwind CSS 4.3.3 | HIGH | npm registry | — |
| TanStack Query 5.101.4 | HIGH | npm registry | — |
| Zod 4.4.3 | HIGH | npm registry | — |
| React Hook Form 7.82.0 | HIGH | npm registry | — |
| Recharts 3.10.0 | HIGH | npm registry | — |
| TanStack Table 8.21.3 | HIGH | npm registry | — |

---

## Sources

- supabase-kt v3.7.0 `libs.versions.toml`: `github.com/supabase-community/supabase-kt` (GitHub API, 2026-07-22)
- Ktor latest release: `github.com/ktorio/ktor/releases` (tag `3.5.1`, 2026-06-29)
- Hilt latest release: `github.com/google/dagger/releases` (tag `dagger-2.60.1`, 2026-07-06)
- Coil latest release: `github.com/coil-kt/coil/releases` (tag `3.5.0`, 2026-06-10)
- npm registry: `registry.npmjs.org` — all web library versions (queried 2026-07-22)
- Firebase Android SDK `libs.versions.toml`: `github.com/firebase/firebase-android-sdk` (DataStore 1.1.7)
- Supabase SSR migration guide: `supabase.com/docs/guides/auth/server-side/nextjs`
