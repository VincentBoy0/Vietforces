# Research Summary — VietForces

**Synthesized:** 2026-07-22
**Sources:** STACK.md · FEATURES.md · ARCHITECTURE.md · PITFALLS.md
**Overall Confidence:** HIGH

---

## Executive Summary

VietForces is a mature Android app (Kotlin + Jetpack Compose, 41 files, 8 game modes, ELO ranking, OpenAI AI features) transitioning from a fully local/offline product into a connected social platform with a Supabase backend, Next.js web admin dashboard, and landing page. The core challenge is not starting from scratch — it is carefully layering backend connectivity onto a working app without breaking existing gameplay. All four research streams converge on the same architecture: a clean Repository + ViewModel layer wrapping a single Supabase client, local-first data with cloud sync-on-session-end, and server-authoritative ELO. The existing Manager singletons are retained, not rewritten; new features get the full MVVM stack.

The recommended approach is phased, dependency-driven: stand up the Supabase schema + RLS first (everything depends on auth), then add auth + progress sync, then the engagement loops (leaderboard → daily challenge → push notifications), then social, then the web surfaces. The Next.js admin dashboard can be built in parallel with the social features since it is an independent project using the same Supabase instance. The landing page is last — purely presentational, zero blockers.

The top risks are security-first: the OpenAI API key is currently baked into the APK (`BuildConfig`) and must be proxied via a Supabase Edge Function in Phase 1 before any deployment. ELO must be server-authoritative from the start to prevent leaderboard cheating. Two pre-work items — fixing a `SimpleDateFormat` locale bug and renaming the `com.example` application ID — must happen before Phase 1 to avoid data corruption and FCM/Play Store blocks downstream.

---

## Key Findings

### From STACK.md

**Android additions (all HIGH confidence unless noted):**

| Library | Version | Role |
|---------|---------|------|
| `supabase-bom` (io.github.jan-tennant) | **3.7.0** | Pins auth-kt, postgrest-kt, realtime-kt, storage-kt together |
| `ktor-client-okhttp` | **3.5.1** | HTTP engine for supabase-kt (use OkHttp, not CIO, on Android) |
| `hilt-android` | **2.60.1** | DI — inject SupabaseClient singleton into ViewModels |
| `hilt-navigation-compose` / `hilt-work` | **1.2.0** | Compose + WorkManager integration |
| `lifecycle-viewmodel-ktx` / `lifecycle-runtime-compose` | **2.10.0** | ViewModel + `collectAsStateWithLifecycle()` |
| `kotlinx-coroutines-android` | **1.11.0** | Upgrade from 1.8.1; required for Realtime |
| `coil-compose` + `coil-network-okhttp` | **3.5.0** | Network image loading in Compose (not Glide/Picasso) |
| `datastore-preferences` | **1.1.7** | Replace SharedPreferences (async, coroutine-native) — MEDIUM confidence |
| `room-runtime` / `room-ktx` / `room-compiler` | **2.7.x** | Local vocabulary cache + offline game data — MEDIUM confidence |
| `firebase-messaging-ktx` (via BOM ~34.x) | via BOM | FCM push notifications — MEDIUM confidence on version |
| `navigation-compose` | **2.9.x** | Upgrade for type-safe routes + auth back-stack |
| `compose-bom` | **2025.06.00+** | Upgrade from 2024.09.00 for lifecycle/navigation compat |

**Web stack (all HIGH confidence):**

| Technology | Version | Role |
|------------|---------|------|
| `next` | **15.3.9** | Next.js LTS (not 16 — too new); App Router + Server Components |
| `@supabase/supabase-js` | **2.110.8** | Supabase client |
| `@supabase/ssr` | **0.12.3** | SSR auth for Next.js App Router (NOT `auth-helpers-nextjs` — deprecated) |
| `tailwindcss` | **4.3.3** | v4 (no `tailwind.config.js`; configure via `@theme` CSS directive) |
| `shadcn/ui` | CLI-based | Admin component library (Radix primitives, Tailwind v4-compatible) |
| `@tanstack/react-query` | **5.101.4** | Client-side data fetching for live admin data |
| `zustand` | **5.0.14** | Lightweight client state for admin UI |
| `@tanstack/react-table` | **8.21.3** | Headless table logic for CRUD screens |
| `recharts` | **3.10.0** | Analytics charts (SVG-based, React-native API) |
| `react-hook-form` | **7.82.0** | Admin forms |
| `zod` | **4.4.3** | Schema validation (v4 has breaking changes from v3 — start fresh) |

**Critical version constraint:** Use KSP (not kapt) for Hilt + Room + serialization annotation processing with Kotlin 2.x. kapt is deprecated.

---

### From FEATURES.md

**Must-have (table stakes):**
1. **Auth** — Google OAuth (one-tap) as primary CTA + email/password fallback + "try before signup" guest mode (Duolingo's highest-ROI acquisition pattern — do NOT gate gameplay behind registration)
2. **Onboarding** — max 4 screens (Welcome → Goal → Level → Name); every extra screen costs ~10% of users
3. **Streak system** — server-authoritative, with streak freeze (1/week free); losing a streak without freeze drives churn; streak-danger push notification is highest-ROI notification
4. **Cloud sync** — ELO + streak survive reinstall; last-write-wins for ELO, MAX(local, server) for streak
5. **Real-time leaderboard** — global top 100 + user's own rank/percentile; Supabase Realtime on leaderboard view
6. **Daily challenge** — 1/day server-generated, countdown timer, bonus ELO; completes streak credit
7. **Push notifications** — daily reminder + streak-danger (2/day cap; Android 13+ runtime permission)
8. **Empty states + skeleton screens** — perceived completeness; prevents jarring pop-in on data load

**Differentiators (already built — must be surfaced):**
- ELO with Vietnamese rank names ("Newbie → Huyền Thoại") + rank badge + rank-up animation
- AI writing/roleplay grading via OpenAI (already exists — needs social connection)
- Social follow system (asymmetric, Twitter-style) + friends leaderboard tab
- Achievement / badge system (category mastery, game mode mastery, social badges)

**Admin dashboard priorities:**
1. Vocabulary CRUD with image upload to Supabase Storage — must connect to Android app (currently hardcoded)
2. User management (view, ban, reset ELO)
3. Analytics (DAU/MAU, game mode usage, streak distribution)
4. Daily challenge management (view/create/override)
5. AI conversation moderation log viewer

**Landing page formula:**
Hero + screenshots + 3-4 feature highlights + Play Store CTA + FAQ. No email signup form. Mobile-optimised is critical (most visitors arrive from phone).

**Explicit anti-features to avoid:**
- Chat/messaging between users (moderation nightmare)
- Custom user-created vocabulary sets (content quality issues)
- Audio/pronunciation features (out of scope)
- Full cohort retention analytics (complex SQL, insufficient demo data)
- Admin user impersonation (security surface too large)
- Notifications > 2/day (causes uninstalls)

**MVP (minimum demo-worthy):**
Auth → Onboarding → Streak → Daily Challenge → Leaderboard → Cloud Sync → Admin Vocab CRUD → Landing Page. Friends/social and achievements defer if timeline is tight.

---

### From ARCHITECTURE.md

**Core architecture pattern:** Single `SupabaseClient` created in `VietforcesApplication.onCreate()`, injected via Hilt into Repository classes. Repositories wrap Supabase + SharedPreferences fallback. ViewModels expose `StateFlow<T>` to Composables using `collectAsStateWithLifecycle()`. Existing Manager singletons (UserProgressManager, AiManager, etc.) are **retained** — new features get MVVM, migration is incremental.

**Database schema (6 core tables):**
- `public.users` — extends `auth.users`; stores `username`, `display_name`, `avatar_url`, `fcm_token`
- `public.user_progress` — ELO, streaks, heatmap (JSONB), game stats (JSONB)
- `public.leaderboard` — denormalised view for fast reads; updated by trigger
- `public.daily_challenges` — one row per date; `word_ids` as JSONB
- `public.challenge_completions` — one row per (user, challenge); UNIQUE constraint
- `public.friendships` — asymmetric follow table (follower_id, followee_id)
- `public.ai_logs` — AI conversation log for moderation

**RLS: enabled on all tables from day one.** Never defer RLS policy creation after `CREATE TABLE`.

**Key patterns:**
1. **SupabaseClient singleton** in Application class — single connection pool, single auth state machine
2. **Local-first sync** — SharedPreferences updates immediately (zero latency gameplay); sync to Supabase on session end / app foreground; offline queue in DataStore
3. **Realtime in ViewModel** — subscribe in `init{}`, unsubscribe in `onCleared()`; subscribe-then-fetch ordering to avoid race condition on initial load
4. **Server-authoritative ELO** — Android sends raw game metrics; `SECURITY DEFINER` PostgreSQL function calculates new ELO; client cannot write ELO directly
5. **FCM via Edge Function** — service account key never touches Android APK; Edge Function fetches `fcm_token` from users table and fans out to FCM HTTP v1 API
6. **Next.js admin with @supabase/ssr** — Server Components use service-role key server-side; middleware protects `/admin/*` routes; **every API route also re-verifies JWT** (middleware alone is insufficient)
7. **pg_cron for scheduled jobs** — daily challenge rotation at midnight UTC, streak warning at 19:00 UTC (built into Supabase, no external scheduler needed)

**Anti-patterns explicitly called out:**
- Supabase service role key in Android app
- Realtime channel opened without `onCleared()` cleanup
- Syncing after every game answer (use session-end batch sync instead)
- Storing ELO client-side and trusting the client value
- Next.js `pages/` router mixed with App Router — use App Router exclusively
- `@supabase/auth-helpers-nextjs` (deprecated 2024 — use `@supabase/ssr`)

**Suggested build order (from ARCHITECTURE.md — 8 phases):**

```
Phase 1: Foundation       — Supabase schema + RLS + client singleton
Phase 2: Auth + Sync      — Login/Register + Onboarding + progress sync + FCM token
Phase 3: Leaderboard      — Realtime + ELO server function
Phase 4: Daily Challenges — pg_cron + Edge Function + challenge screen
Phase 5: Push Notifications — FCM integration + streak/challenge notifications
Phase 6: Social / Friends  — Follow system + friends leaderboard
Phase 7: Admin Dashboard   — Next.js App Router (parallel to 3-6)
Phase 8: Landing Page      — Marketing page (parallel to or after 7)
```

---

### From PITFALLS.md

**Critical (must not miss — cause rewrites, security incidents, data loss):**

| # | Pitfall | Prevention | Phase |
|---|---------|------------|-------|
| C-1 | OpenAI API key in APK (`BuildConfig.OPENAI_API_KEY`) | Proxy via Supabase Edge Function `/functions/ai-proxy`; key moves to server env var | Phase 1 — do BEFORE any deployment |
| C-2 | Supabase service role key in Android code | Android uses **only** anon key; service role only in Edge Functions + Next.js server env | Phase 1 setup |
| C-3 | RLS disabled or misconfigured (silent empty results, cross-user data leaks) | Enable RLS + policies for ALL operations (SELECT/INSERT/UPDATE/DELETE) immediately after each `CREATE TABLE` | Phase 1 schema |
| C-4 | Client-side ELO calculation — trivially cheatable | `SECURITY DEFINER` PostgreSQL function; RLS blocks direct ELO column writes from client | Phase 2 before leaderboard |
| C-5 | Daily challenge / streak timezone mismatch (UTC vs Asia/Ho_Chi_Minh) | Store/compare dates in `Asia/Ho_Chi_Minh` timezone; add 2-hour grace period after midnight | Phase 4 schema design |

**Moderate (cause regressions, bad UX, significant rework):**

| # | Pitfall | Prevention | Phase |
|---|---------|------------|-------|
| M-1 | Supabase auth token refresh failure (silent 401 loops) | Collect `supabase.auth.sessionStatus`; handle `RefreshFailure` → navigate to login | Phase 2 |
| M-2 | Realtime channel leaks in Jetpack Compose | Always create channel in ViewModel `init{}`, unsubscribe in `onCleared()` | Phase 3 |
| M-3 | SharedPreferences → Supabase migration double-write | Use server-side `local_data_migrated` flag (not local flag); idempotent upsert | Phase 2 |
| M-4 | Offline ELO sync overwrites server value | ELO server-authoritative; local = display only; streak uses MAX(local, server) | Phase 2 |
| M-5 | Next.js admin API routes unprotected (middleware-only auth) | Re-verify JWT in every `/api/admin/*` handler | Phase 7 |
| M-6 | Android 13+ notification permission silently dropped | Runtime `POST_NOTIFICATIONS` check; ask after first streak achievement | Phase 5 |
| M-7 | Supabase free tier project paused after 7 days inactivity | GitHub Action keepalive ping every 3 days; upgrade to Pro for demo week | Phase 1 day-one |

**Minor (debugging time, minor regressions):**

| # | Pitfall | Prevention | Phase |
|---|---------|------------|-------|
| m-1 | Google OAuth deep link misconfiguration | Correct `<intent-filter>` scheme in Manifest + Supabase redirect URL config | Phase 2 |
| m-2 | `SimpleDateFormat(Locale.getDefault())` corrupts streak dates post-migration | Fix to `Locale.ROOT` **before** writing any migration code | Phase 0 (pre-work) |
| m-3 | `VocabularyRepository` hardcoded → admin CRUD is a no-op | Migrate vocabulary to Supabase `vocabulary` table when building admin CRUD | Phase 7 |
| m-4 | `com.example` applicationId blocks Play Store + FCM | Rename to `vn.edu.hcmus.vietforces` before any Firebase setup | Phase 0 (pre-work) |
| m-5 | Realtime leaderboard race condition on initial load | Subscribe **then** fetch (not fetch then subscribe) | Phase 3 |
| m-6 | Android cloud backup exposes Supabase session tokens | Add `data_extraction_rules.xml` excluding session SharedPreferences | Phase 2 |

---

## Implications for Roadmap

### Suggested Phase Structure

The research across all four files converges on the same dependency graph. The roadmap should follow it closely.

**Phase 0 — Pre-work (no new features; fix blockers)**
- Rationale: Two bugs in existing code will corrupt migration data downstream; one app config will block FCM. Fix before writing a single line of Supabase code.
- Actions: `Locale.getDefault()` → `Locale.ROOT` in `UserProgressManager.kt`; rename `applicationId` from `com.example.vietforces` to `vn.edu.hcmus.vietforces`.
- Research flag: **NOT needed** — changes are straightforward and well-defined.

**Phase 1 — Foundation (Supabase setup + OpenAI proxy)**
- Rationale: All subsequent phases depend on the database schema and auth client. The OpenAI key exposure is a live security risk and must close here.
- Delivers: Supabase project, full schema (all 6 tables), RLS policies, `SupabaseClient` singleton in Application class, Hilt wiring, AI proxy Edge Function, keepalive cron, Supabase anon key in `local.properties`.
- Features from FEATURES.md: (infrastructure only; no user-facing features)
- Pitfalls to avoid: C-1 (OpenAI key), C-2 (service role key), C-3 (RLS), M-7 (free tier pause)
- Research flag: **NOT needed** — schema and setup patterns are well-documented.

**Phase 2 — Auth + Onboarding + Progress Sync**
- Rationale: Nothing that shows live data (leaderboard, challenges, social) works without user rows in the database. This phase creates those rows and migrates local progress.
- Delivers: Login/Register screens (email + Google OAuth), onboarding flow (4 screens), SharedPreferences → Supabase migration, `ProgressRepository.syncToCloud()`, FCM token registration on login, backup rules.
- Features: Auth table stakes, Onboarding flow, Cloud sync, Guest mode ("try first")
- Pitfalls: C-2, M-1 (token refresh), M-3 (migration double-write), M-4 (offline sync), m-1 (OAuth deep link), m-6 (cloud backup)
- Research flag: **Consider research phase** for Google OAuth deep link setup and FCM token registration flow — configuration details are platform-specific.

**Phase 3 — Streak System + Real-time Leaderboard**
- Rationale: These two features are the primary retention mechanics and the most demo-visible. Server-authoritative ELO must be established before the leaderboard can safely go live.
- Delivers: Streak counter + calendar heatmap (synced to Supabase), streak freeze, global leaderboard screen with Realtime updates, ELO PostgreSQL function, rank badge + rank-up animation.
- Features: Streak system, Leaderboard (global + own rank), ELO differentiator showcase
- Pitfalls: C-4 (client ELO), M-2 (Realtime leaks), m-5 (Realtime race condition)
- Research flag: **NOT needed** — Realtime subscription patterns are well-documented in ARCHITECTURE.md.

**Phase 4 — Daily Challenge + Push Notifications**
- Rationale: Together these form the "reason to return daily" loop. Daily challenge gives users something to do; push notifications re-engage users who forget.
- Delivers: `daily_challenges` schema, pg_cron Edge Function (midnight UTC rotation), challenge screen with countdown + reward animation, FCM integration, streak-danger notification (19:00 UTC), daily reminder.
- Features: Daily challenge (all sub-features), Push notifications (streak-danger + daily reminder)
- Pitfalls: C-5 (timezone), M-6 (Android 13 permission)
- Research flag: **Consider research phase** for pg_cron Edge Function syntax and FCM HTTP v1 API service account setup — these have non-obvious configuration steps.

**Phase 5 — Social + Friends** *(defer if timeline tight)*
- Rationale: Social accountability drives long-term retention but is not demo-critical. Core loop (streak + leaderboard + challenges) makes a complete demo without it.
- Delivers: Follow/unfollow system, friends leaderboard tab, activity feed (milestones), friend-activity push notifications.
- Features: Social follow, friends leaderboard, activity feed, invite link
- Pitfalls: M-2 (Realtime for activity feed), C-3 (friendships RLS)
- Research flag: **NOT needed** — standard friendship table pattern.

**Phase 6 — Admin Dashboard (Next.js)** *(parallel to Phase 3-5)*
- Rationale: Independent Next.js project; only requires the Supabase schema from Phase 1. Can be developed in parallel with Android phases.
- Delivers: Vocabulary CRUD (connects to Android app — fixes m-3), user management (ban/ELO reset), daily challenge management, analytics (DAU, game mode usage), AI moderation log viewer, auth middleware + per-route JWT verification.
- Features: All admin dashboard features; vocabulary migrated from hardcoded to Supabase
- Pitfalls: M-5 (API routes unprotected), m-3 (vocabulary hardcoded disconnect)
- Research flag: **NOT needed** for standard CRUD. Consider research for analytics SQL queries (cohort analysis).

**Phase 7 — Landing Page** *(last; after Play Store link available)*
- Rationale: Purely presentational; no backend dependencies except a public Supabase read for optional live leaderboard teaser.
- Delivers: Hero + app screenshots + feature highlights + Play Store CTA + FAQ + mobile-optimised layout.
- Features: All landing page must-have sections; language toggle (EN/VI)
- Research flag: **NOT needed** — formula is well-defined by FEATURES.md.

---

### Research Flags

| Phase | Research Needed? | Reason |
|-------|-----------------|--------|
| Phase 0 | No | Mechanical code fixes |
| Phase 1 | No | Schema + RLS patterns fully documented |
| Phase 2 | **Yes — consider** | Google OAuth deep link + FCM token flow have config-specific gotchas |
| Phase 3 | No | Realtime patterns fully documented in ARCHITECTURE.md |
| Phase 4 | **Yes — consider** | pg_cron Edge Function syntax; FCM HTTP v1 service account JWT generation |
| Phase 5 | No | Standard friendship table + Realtime patterns |
| Phase 6 | No (CRUD); maybe for analytics | Advanced SQL cohort queries if implementing D7/D30 retention |
| Phase 7 | No | Formulaic landing page |

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| **Android stack versions** | HIGH | supabase-kt 3.7.0, Ktor 3.5.1, Hilt 2.60.1, Coil 3.5.0 — all sourced from GitHub API release tags |
| **Android state management pattern** | HIGH | MVVM + StateFlow + Repository is the established Android architecture |
| **Web stack versions** | HIGH | Next.js 15.3.9, @supabase/ssr 0.12.3, Tailwind 4.3.3, all major libs — confirmed via npm |
| **Database schema** | HIGH | PostgreSQL + Supabase patterns are well-documented; schema matches PROJECT.md requirements |
| **Security architecture** | HIGH | RLS, service-role-never-on-client, Edge Function proxy — official Supabase best practices |
| **Feature priorities** | HIGH | Sourced from published Duolingo retention research + established gamification patterns |
| **Room 2.7.x** | MEDIUM | Maven Central was blocked during research; verify exact patch version at developer.android.com before using |
| **DataStore 1.1.7** | MEDIUM | Sourced indirectly from Firebase SDK's toml; check developer.android.com/jetpack/androidx/releases/datastore |
| **Firebase BOM ~34.x** | MEDIUM | Version estimate for mid-2026; verify at firebase.google.com/support/release-notes/android |
| **Compose BOM 2025.06.00+** | MEDIUM | Estimate; check BOM mapping table before upgrading |
| **Admin analytics SQL** | LOW-MEDIUM | D1/D7/D30 cohort queries are complex; recommend using simple DAU/MAU table view first for demo |

---

## Gaps to Address During Planning

1. **Room exact version** — Verify `2.7.x` patch version at developer.android.com before adding to `libs.versions.toml`.
2. **Firebase BOM exact version** — Verify current stable BOM at firebase.google.com before FCM integration.
3. **Compose BOM mapping** — Check the BOM ↔ library version mapping at developer.android.com when upgrading from `2024.09.00`.
4. **ELO PostgreSQL function details** — The ARCHITECTURE.md pattern for `update_user_elo` is solid but the exact game-mode weighting / opponent ELO for solo games needs a design decision before Phase 3.
5. **Play Store submission intent** — Pitfall m-4 assumes Play Store publishing is desired. If demo is APK-only, the `applicationId` rename is less urgent (but FCM still requires it).
6. **Admin user authentication strategy** — A dedicated `admin_users` table vs. a `role` claim on Supabase auth users needs a decision in Phase 6. The Supabase SSR pattern supports both.
7. **Vocabulary migration scope** — 1,328 lines of hardcoded vocabulary in `VocabularyRepository.kt` need to be seeded into Supabase before the admin CRUD is useful. Bulk CSV import tooling or a one-time seed script needs planning.

---

## Sources Aggregated

| Source | Confidence | Used In |
|--------|-----------|---------|
| supabase-kt v3.7.0 `libs.versions.toml` (GitHub API, 2026-07-22) | HIGH | STACK.md |
| Ktor v3.5.1 release tag (GitHub API, 2026-06-29) | HIGH | STACK.md |
| Hilt 2.60.1 release (GitHub API, 2026-07-06) | HIGH | STACK.md |
| Coil 3.5.0 release (GitHub API, 2026-06-10) | HIGH | STACK.md |
| npm registry — all web libraries (2026-07-22) | HIGH | STACK.md |
| Firebase Android SDK `libs.versions.toml` (DataStore 1.1.7) | MEDIUM | STACK.md |
| Supabase official docs — Auth, RLS, Realtime, Edge Functions | HIGH | ARCHITECTURE.md, PITFALLS.md |
| Duolingo public blog + retention research case studies | HIGH | FEATURES.md |
| Babbel / Memrise feature audits | HIGH | FEATURES.md |
| Android developer docs — notification permissions, backup rules | HIGH | PITFALLS.md |
| VietForces codebase (`app/` — 41 Kotlin files) | HIGH | All files |
| PROJECT.md — requirements, constraints, decisions | HIGH | All files |
