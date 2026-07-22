# Architecture Patterns

**Domain:** Gamified language-learning app — Android Kotlin + Supabase backend + Next.js web
**Researched:** 2026-07-22
**Overall confidence:** HIGH (well-established patterns; all libraries are current stable releases)

---

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              Android App (Kotlin/Compose)                            │
│                                                                                      │
│  UI Layer (Composables)                                                              │
│  ├── Auth screens (Login, Register, Onboarding)                                      │
│  ├── Existing game screens (ImageToWord, WordSearch, etc.)                           │
│  ├── New: Leaderboard, DailyChallenge, Friends, Streak screens                       │
│  └── Shared: DraggableMascot, BottomNav, loading/empty states                       │
│                                                                                      │
│  ViewModel Layer (NEW — per-screen lifecycle-aware state)                            │
│  ├── AuthViewModel        ← SupabaseAuthRepository                                   │
│  ├── LeaderboardViewModel ← LeaderboardRepository (Realtime sub)                    │
│  ├── DailyChallengeViewModel ← ChallengeRepository                                  │
│  ├── ProgressViewModel    ← ProgressRepository (sync)                               │
│  └── [existing screens can stay on Manager singletons during migration]              │
│                                                                                      │
│  Repository Layer (NEW — clean seam between ViewModel and data)                     │
│  ├── SupabaseAuthRepository  → Supabase Auth (signUp, signIn, session)              │
│  ├── ProgressRepository      → SupabaseClient.from("user_progress") + SharedPrefs  │
│  ├── LeaderboardRepository   → SupabaseClient.from("leaderboard") + Realtime       │
│  ├── ChallengeRepository     → SupabaseClient.from("daily_challenges")              │
│  ├── VocabularyRepository    → unchanged (in-memory static data)                   │
│  └── FriendsRepository       → SupabaseClient.from("friendships")                  │
│                                                                                      │
│  Existing Manager Layer (retained, not rewritten)                                    │
│  └── UserProgressManager, AiManager, EncounteredItemsManager, etc.                 │
│                                                                                      │
│  Data Sources                                                                        │
│  ├── PreferencesManager (SharedPreferences — offline fallback, local cache)         │
│  ├── SupabaseClient (supabase-kt v3) — single instance in Application class        │
│  └── OpenAiClient (unchanged)                                                        │
└──────────────────────────────────────────────────────────────────────────────────────┘
                               │ HTTPS + WebSocket
                               ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              Supabase Platform                                        │
│                                                                                      │
│  Auth           PostgREST (REST API)       Realtime           Storage                │
│  ├── email/pw   ├── /rest/v1/users         ├── channel:       ├── vocabulary-images  │
│  └── Google     ├── /rest/v1/user_progress │  leaderboard     └── user-avatars       │
│     OAuth       ├── /rest/v1/leaderboard   ├── channel:                              │
│                 ├── /rest/v1/challenges     │  challenge-live                         │
│                 └── /rest/v1/friendships   └── postgres_changes                      │
│                                                                                      │
│  Edge Functions (Deno/TypeScript)          pg_cron (Scheduled Jobs)                  │
│  ├── send-fcm-notification                 ├── daily-challenge-rotate (midnight UTC) │
│  ├── calculate-leaderboard-rank            └── streak-check-warning (19:00 UTC)     │
│  └── moderate-ai-logs                                                                │
│                                                                                      │
│  PostgreSQL Database (Row Level Security on all user tables)                         │
└──────────────────────────────────────────────────────────────────────────────────────┘
                               │ HTTPS
                   ┌───────────┴──────────┐
                   ▼                      ▼
┌─────────────────────────┐  ┌─────────────────────────────────────────────────────────┐
│     Next.js Admin        │  │                    FCM (Firebase)                       │
│  (App Router, Vercel)    │  │  ← Edge Function calls Google FCM HTTP v1 API          │
│  ├── /admin/vocab        │  │  ← Android app registers token → stored in users table │
│  ├── /admin/users        │  └─────────────────────────────────────────────────────────┘
│  ├── /admin/challenges   │
│  ├── /admin/analytics    │
│  └── /admin/moderation   │
│                          │
│  Auth: Supabase SSR      │
│  (@supabase/ssr package) │
└─────────────────────────┘
```

---

## Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `SupabaseClient` (Android singleton) | Single Supabase connection; initialized in `Application.onCreate()` | All Repository classes |
| `SupabaseAuthRepository` | signUp, signIn, signOut, session refresh, Google OAuth | `SupabaseClient.auth`, `PreferencesManager` (cache user id) |
| `ProgressRepository` | Read/write user_progress; local-first with cloud sync on resume | `SupabaseClient.postgrest`, `PreferencesManager` (offline cache) |
| `LeaderboardRepository` | Fetch top-N leaderboard rows; subscribe to Realtime postgres_changes | `SupabaseClient.postgrest`, `SupabaseClient.realtime` |
| `ChallengeRepository` | Fetch today's daily challenge; submit completion | `SupabaseClient.postgrest` |
| `FriendsRepository` | Follow/unfollow, fetch friend list + their progress | `SupabaseClient.postgrest` |
| `LeaderboardViewModel` | Expose `StateFlow<List<LeaderboardEntry>>`; manage Realtime subscription lifecycle | `LeaderboardRepository` |
| `AuthViewModel` | Auth state machine (unauthenticated → loading → authenticated → error) | `SupabaseAuthRepository` |
| `ProgressViewModel` | Merge local ELO/streak with cloud; trigger sync | `ProgressRepository`, `UserProgressManager` |
| Supabase Edge Functions | Server-side operations needing secrets (FCM, rank calculation) | FCM API, Supabase DB |
| pg_cron jobs | Scheduled tasks (rotate daily challenge, streak warnings) | Supabase DB, Edge Functions |
| Next.js Admin | CRUD for content + user management; analytics dashboards | Supabase via `@supabase/ssr`, PostgREST |
| Next.js Middleware | Auth guard on all `/admin/*` routes | Supabase session cookies |

---

## Database Schema

### Core Tables

```sql
-- Users table (extends Supabase auth.users)
CREATE TABLE public.users (
  id            UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username      TEXT UNIQUE NOT NULL,
  display_name  TEXT,
  avatar_url    TEXT,
  fcm_token     TEXT,                  -- updated on app launch
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- User progress (cloud mirror of local SharedPreferences UserSession)
CREATE TABLE public.user_progress (
  user_id        UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  elo_rating     INT NOT NULL DEFAULT 800,
  current_streak INT NOT NULL DEFAULT 0,
  longest_streak INT NOT NULL DEFAULT 0,
  last_played_at TIMESTAMPTZ,
  total_games    INT NOT NULL DEFAULT 0,
  words_learned  INT NOT NULL DEFAULT 0,
  heatmap_data   JSONB DEFAULT '{}',   -- { "2026-07-22": 3, ... }
  game_stats     JSONB DEFAULT '{}',   -- { "image_to_word": { correct: 42, wrong: 5 }, ... }
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

-- Leaderboard view (denormalized for fast reads)
-- Materialized from user_progress + users; refreshed by trigger or cron
CREATE TABLE public.leaderboard (
  user_id      UUID PRIMARY KEY REFERENCES public.users(id),
  username     TEXT NOT NULL,
  display_name TEXT,
  avatar_url   TEXT,
  elo_rating   INT NOT NULL,
  rank_title   TEXT,                   -- "Newbie", "Expert", etc.
  updated_at   TIMESTAMPTZ DEFAULT NOW()
);

-- Daily challenges
CREATE TABLE public.daily_challenges (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  date         DATE UNIQUE NOT NULL,  -- one challenge per calendar day
  game_mode    TEXT NOT NULL,         -- matches GameMode.id
  difficulty   TEXT NOT NULL DEFAULT 'normal',
  word_ids     JSONB NOT NULL,        -- array of vocabulary item IDs
  bonus_elo    INT NOT NULL DEFAULT 50,
  is_active    BOOLEAN DEFAULT TRUE,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- User challenge completions
CREATE TABLE public.challenge_completions (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES public.users(id),
  challenge_id UUID NOT NULL REFERENCES public.daily_challenges(id),
  score        INT NOT NULL,
  completed_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, challenge_id)        -- one attempt per challenge per user
);

-- Friendships (follows — asymmetric)
CREATE TABLE public.friendships (
  follower_id  UUID NOT NULL REFERENCES public.users(id),
  followee_id  UUID NOT NULL REFERENCES public.users(id),
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (follower_id, followee_id)
);

-- AI conversation logs (for moderation)
CREATE TABLE public.ai_logs (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID REFERENCES public.users(id),
  feature      TEXT NOT NULL,          -- "roleplay" | "writing" | "mascot"
  prompt       TEXT,
  response     TEXT,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### Row Level Security Policies

```sql
-- Users: only self can update; all authenticated can read
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_select" ON public.users FOR SELECT TO authenticated USING (true);
CREATE POLICY "users_update" ON public.users FOR UPDATE TO authenticated USING (auth.uid() = id);

-- User progress: only owner reads/writes
ALTER TABLE public.user_progress ENABLE ROW LEVEL SECURITY;
CREATE POLICY "progress_owner_only" ON public.user_progress
  FOR ALL TO authenticated USING (auth.uid() = user_id);

-- Leaderboard: all authenticated can read; system/service_role writes
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;
CREATE POLICY "leaderboard_read" ON public.leaderboard FOR SELECT TO authenticated USING (true);
-- No direct INSERT/UPDATE from client — updated by trigger or Edge Function

-- Daily challenges: all authenticated can read; admin inserts via service_role
ALTER TABLE public.daily_challenges ENABLE ROW LEVEL SECURITY;
CREATE POLICY "challenges_read" ON public.daily_challenges FOR SELECT TO authenticated USING (true);

-- Challenge completions: owner inserts/reads own; no delete
ALTER TABLE public.challenge_completions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "completions_owner" ON public.challenge_completions
  FOR ALL TO authenticated USING (auth.uid() = user_id);

-- Friendships: user manages own follows; authenticated can read all (for friend discovery)
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
CREATE POLICY "friendships_read" ON public.friendships FOR SELECT TO authenticated USING (true);
CREATE POLICY "friendships_insert" ON public.friendships
  FOR INSERT TO authenticated WITH CHECK (auth.uid() = follower_id);
CREATE POLICY "friendships_delete" ON public.friendships
  FOR DELETE TO authenticated USING (auth.uid() = follower_id);
```

---

## Data Flow

### 1. App Launch + Auth Flow

```
App Launch
    │
    ▼
SupabaseClient.init() in Application.onCreate()
    │
    ▼
AuthViewModel.checkSession()
    ├── session exists? → emit Authenticated(user) → navigate to Home
    └── no session?    → emit Unauthenticated → navigate to Login/Onboarding
                            │
                     User completes Onboarding / Login
                            │
                     SupabaseAuthRepository.signIn() / .signUp()
                            │
                     On success → upsert public.users row
                            │
                     emit Authenticated → navigate to Home
```

### 2. Progress Sync Flow (offline-first, last-write-wins)

```
User completes a game session
    │
    ▼
UserProgressManager.recordCorrectAnswer() [LOCAL — unchanged]
    → updates SharedPreferences immediately (zero latency)
    │
    ▼
ProgressViewModel.syncToCloud() [called on: app foreground, session end]
    │
    ├── if authenticated:
    │     SupabaseClient.from("user_progress").upsert(
    │       UserProgressDto.fromLocal(UserProgressManager),
    │       onConflict = "user_id"    ← upsert = last-write-wins
    │     )
    │     then: upsert leaderboard row (trigger or direct)
    │
    └── if offline:
          queue sync in SharedPreferences flag "pendingSyncRequired=true"
          → synced next time network + auth available (via ConnectivityManager listener)
```

### 3. Real-time Leaderboard Flow

```
LeaderboardScreen opens
    │
    ▼
LeaderboardViewModel.subscribe()
    │
    ├── initial fetch: SupabaseClient.from("leaderboard")
    │     .select("*").order("elo_rating", ascending=false).limit(50)
    │     → emit StateFlow<List<LeaderboardEntry>>
    │
    └── Realtime subscription:
          SupabaseClient.realtime.channel("leaderboard-changes")
            .postgresChangeFlow<PostgresAction.Update>(schema="public", table="leaderboard")
            .collect { change →
                updateEntryInList(change.record)    // in-place update
            }
            .subscribe()

LeaderboardScreen closes
    │
    ▼
LeaderboardViewModel.onCleared()
    → channel.unsubscribe()     ← CRITICAL: avoid memory leak + wasted connections
```

### 4. Daily Challenge Flow

```
[pg_cron — midnight UTC]
    │
    ├── DELETE FROM daily_challenges WHERE date < NOW() - INTERVAL '7 days'
    └── Edge Function: generate-daily-challenge
          → picks game_mode (round-robin), selects 10 word_ids
          → inserts into daily_challenges with date = CURRENT_DATE
          → calls send-fcm-notification Edge Function with topic "daily_challenge"

[Android App — DailyChallengeViewModel.load()]
    │
    ├── SupabaseClient.from("daily_challenges")
    │     .select("*").eq("date", LocalDate.now()).single()
    │     → cache in SharedPreferences (avoids re-fetch for same day)
    │
    └── on completion: insert challenge_completions row + sync ELO

[Streak Warning — pg_cron 19:00 UTC daily]
    → SELECT user_id FROM user_progress WHERE last_played_at < CURRENT_DATE
    → for each: call send-fcm-notification Edge Function
```

### 5. Push Notification Flow (FCM + Edge Function)

```
Android App
    │
    ├── FirebaseMessaging.getInstance().token.await()  → fcm_token string
    └── SupabaseClient.from("users").update { set("fcm_token", token) }
          .eq("id", currentUserId)   [on every app launch]

Supabase Edge Function: send-fcm-notification (Deno/TypeScript)
    │
    ├── receives: { user_ids: string[], title: string, body: string, data: object }
    ├── SELECT fcm_token FROM users WHERE id = ANY(user_ids)
    ├── POST to https://fcm.googleapis.com/v1/projects/{projectId}/messages:send
    │     Authorization: Bearer {GOOGLE_SERVICE_ACCOUNT_TOKEN}
    └── handles token refresh / expired token cleanup
```

---

## Patterns to Follow

### Pattern 1: SupabaseClient Singleton in Application

**What:** Create a single `SupabaseClient` in a custom `Application` class; inject via a simple service locator or Hilt.

**Why:** Supabase-kt client manages its own connection pool, auth state machine, and Realtime WebSocket. Multiple instances cause auth state desync.

```kotlin
// app/src/main/java/com/example/vietforces/VietforcesApplication.kt
class VietforcesApplication : Application() {
    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "vietforces"      // for OAuth deep link callback
                host = "login"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
// AndroidManifest.xml: android:name=".VietforcesApplication"
```

### Pattern 2: Repository with Local-First Fallback

**What:** Repository tries local cache first, falls back to network, syncs changes when online.

**Why:** Vietnamese learners may have spotty connectivity; local SharedPreferences is already the source of truth — Supabase is the cloud mirror.

```kotlin
class ProgressRepository(
    private val supabase: SupabaseClient,
    private val prefs: PreferencesManager
) {
    suspend fun syncProgress(localProgress: UserSession): Result<Unit> {
        return try {
            supabase.from("user_progress").upsert(
                UserProgressDto.from(localProgress),
                onConflict = "user_id"
            )
            prefs.clearPendingSync()
            Result.success(Unit)
        } catch (e: Exception) {
            prefs.markPendingSyncRequired()    // retry on next foreground
            Result.failure(e)
        }
    }

    suspend fun fetchFromCloud(): UserProgressDto? {
        return try {
            supabase.from("user_progress")
                .select().eq("user_id", supabase.auth.currentUserOrNull()?.id ?: return null)
                .decodeSingleOrNull()
        } catch (e: Exception) { null }   // return null → UI uses local data
    }
}
```

### Pattern 3: Realtime Subscription in ViewModel (with cleanup)

**What:** Start Realtime subscription in ViewModel `init{}`, cancel in `onCleared()`.

**Why:** Realtime channels are WebSocket-backed; must be cleaned up to avoid zombie connections on screen exit.

```kotlin
class LeaderboardViewModel(private val repo: LeaderboardRepository) : ViewModel() {
    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()
    private var channel: RealtimeChannel? = null

    init {
        viewModelScope.launch {
            _entries.value = repo.fetchTop50()          // initial load
            channel = repo.subscribeToChanges { change ->
                _entries.update { list -> list.mergeUpdate(change) }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.launch { channel?.unsubscribe() }
        super.onCleared()
    }
}
```

### Pattern 4: Supabase Auth Session Handling

**What:** Use `supabase.auth.sessionStatus` Flow to reactively drive navigation between auth and main app.

**Why:** The auth state machine handles token refresh, session expiry, and OAuth callbacks automatically when observed via Flow.

```kotlin
// In MainActivity or AuthViewModel
supabase.auth.sessionStatus
    .onEach { status ->
        when (status) {
            is SessionStatus.Authenticated    -> navController.navigate(Screen.Main.route)
            is SessionStatus.NotAuthenticated -> navController.navigate(Screen.Login.route)
            is SessionStatus.LoadingFromStorage -> { /* show splash */ }
            is SessionStatus.NetworkError     -> { /* show offline banner */ }
        }
    }
    .launchIn(lifecycleScope)
```

### Pattern 5: Next.js Admin with Supabase SSR

**What:** Use `@supabase/ssr` (not the deprecated `@supabase/auth-helpers`) with Next.js App Router. Server Components read data; Client Components subscribe to Realtime.

**Why:** Server Components have direct DB access via service-role key; no client exposure of admin credentials.

```typescript
// app/admin/layout.tsx — guard all admin routes
import { createServerClient } from '@supabase/ssr'
import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'

export default async function AdminLayout({ children }) {
  const cookieStore = cookies()
  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!,  // service role — server only
    { cookies: { get: (name) => cookieStore.get(name)?.value } }
  )
  const { data: { user } } = await supabase.auth.getUser()
  if (!user || !isAdmin(user)) redirect('/login')
  return <>{children}</>
}

// app/admin/vocab/page.tsx — Server Component fetches data
export default async function VocabPage() {
  const words = await supabase.from('vocabulary').select('*').order('category')
  return <VocabTable initialData={words.data} />
}
```

### Pattern 6: Edge Function for FCM (avoid client-side secrets)

**What:** Never call FCM directly from Android — route through Supabase Edge Function that holds the Google service account key.

**Why:** Service account keys cannot be embedded in APKs. Edge Functions run server-side with secrets in env vars.

```typescript
// supabase/functions/send-fcm-notification/index.ts
import { serve } from "https://deno.land/std/http/server.ts"

serve(async (req) => {
  const { user_ids, title, body, data } = await req.json()
  const supabaseAdmin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)

  const { data: tokens } = await supabaseAdmin
    .from("users").select("fcm_token").in("id", user_ids)

  // Call FCM HTTP v1 API using service account JWT
  for (const { fcm_token } of tokens ?? []) {
    await fetch(`https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${await getServiceAccountToken()}` },
      body: JSON.stringify({ message: { token: fcm_token, notification: { title, body }, data } })
    })
  }
  return new Response("ok")
})
```

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Supabase RLS Disabled / Service Role on Client

**What:** Using `SUPABASE_SERVICE_ROLE_KEY` in the Android APK or calling `supabase.auth.signIn` with service_role credentials.

**Why bad:** Service role bypasses all RLS — anyone who extracts it from the APK has full database access. Leaked service_role = full data breach.

**Instead:** Android always uses the `SUPABASE_ANON_KEY`. Service role used only in Edge Functions (env vars, never shipped in APK) and Next.js admin backend.

### Anti-Pattern 2: Opening a Realtime Channel and Never Closing It

**What:** Subscribing in a Composable directly (not in a ViewModel), or forgetting `channel.unsubscribe()` in `onCleared()`.

**Why bad:** Each open channel uses a WebSocket slot; Supabase free tier allows 200 concurrent realtime connections. An app with 50 users that doesn't unsubscribe will hit this limit.

**Instead:** Always subscribe in a ViewModel `init{}`, always unsubscribe in `onCleared()`. Use `rememberCoroutineScope()` + `DisposableEffect` only for lightweight UI concerns.

### Anti-Pattern 3: Syncing Every Keystroke / Answer

**What:** Calling `supabase.from("user_progress").upsert(...)` after every single game answer.

**Why bad:** Game sessions fire answers every 5–10 seconds. 100 users × 10 games/session × 20 answers = 20,000 DB writes/session. Quickly exhausts Supabase free tier row limits and adds 50–150ms latency per answer.

**Instead:** Keep local-first (SharedPreferences updates are instant); sync to Supabase only on: session end, app background, app foreground after >5 min gap. One upsert per game session.

### Anti-Pattern 4: Storing ELO Authoritatively on Client

**What:** Computing ELO on Android and trusting the client-sent value directly in the leaderboard.

**Why bad:** Users can modify SharedPreferences or intercept the API call and send arbitrary ELO values.

**Instead:** For the leaderboard specifically, ELO is recalculated server-side by a PostgreSQL function or Edge Function. Android sends raw game result metrics (correct/wrong/time); server computes new ELO.

```sql
-- Postgres function called by Edge Function (not direct client)
CREATE OR REPLACE FUNCTION update_user_elo(
  p_user_id UUID, p_correct_answers INT, p_wrong_answers INT, p_difficulty TEXT
) RETURNS INT AS $$
DECLARE new_elo INT;
BEGIN
  -- ELO calculation logic here
  UPDATE user_progress SET elo_rating = new_elo WHERE user_id = p_user_id;
  UPDATE leaderboard SET elo_rating = new_elo WHERE user_id = p_user_id;
  RETURN new_elo;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### Anti-Pattern 5: Next.js Pages Router Mixed with App Router

**What:** Starting the admin dashboard with `pages/` directory because it's more familiar.

**Why bad:** `pages/` router lacks built-in server components, middleware chaining for auth, and doesn't support the Supabase SSR cookie helper patterns cleanly. Mixing is messy.

**Instead:** Use App Router (`app/`) exclusively. All admin pages are Server Components by default; add `"use client"` only for interactive tables/charts.

### Anti-Pattern 6: Using `supabase-js` v1 / `auth-helpers` (deprecated)

**What:** Installing `@supabase/auth-helpers-nextjs` which appears in many tutorials.

**Why bad:** Deprecated since late 2023 in favour of `@supabase/ssr`. Auth Helpers have known cookie-handling bugs in App Router and are no longer maintained.

**Instead:** Use `@supabase/ssr` + `@supabase/supabase-js` v2 / v2.39+.

---

## Scalability Considerations

| Concern | At 100 users (demo/thesis) | At 10K users | At 1M users |
|---------|---------------------------|--------------|-------------|
| Leaderboard reads | Direct table select is fine | Materialised view + index on elo_rating | Redis cache in front |
| Realtime connections | Free tier 200 concurrent — more than enough | Supabase Pro (500 concurrent) | Ably / Pusher sidecar |
| Progress syncs | Batch at session end | Same — no change needed | Partitioning user_progress by user_id hash |
| Daily challenge generation | Edge Function + pg_cron | Same | Pre-generate in bulk |
| FCM delivery | Edge Function fan-out loop | Batch FCM sends (up to 500/request) | Dedicated notification microservice |
| DB row budget | Free tier: 500MB — ~1M game sessions | Pro tier | Multiple projects |

---

## Suggested Build Order

Dependencies drive this order — each phase unblocks the next.

```
Phase 1: Foundation
  1a. Supabase project setup + schema (users, user_progress, leaderboard tables)
  1b. RLS policies on all tables
  1c. SupabaseClient singleton in VietforcesApplication.kt
  1d. BuildConfig fields: SUPABASE_URL, SUPABASE_ANON_KEY
  → Unblocks everything else (all features need auth + DB)

Phase 2: Auth + Profile Sync
  2a. AuthViewModel + SupabaseAuthRepository
  2b. Login / Register screens (email + Google OAuth)
  2c. Onboarding flow → creates public.users row on first login
  2d. ProgressRepository.syncToCloud() — sync existing SharedPreferences ELO/streak
  2e. FCM token registration on login
  → Unblocks leaderboard (needs user rows), challenges (needs auth), notifications (needs token)

Phase 3: Leaderboard (Realtime)
  3a. LeaderboardRepository + LeaderboardViewModel
  3b. Leaderboard screen (replaces placeholder)
  3c. Realtime subscription setup + unsubscribe lifecycle
  3d. ELO trigger: on user_progress UPDATE → sync leaderboard row
  → Unblocks friends (friends need leaderboard-style queries)

Phase 4: Daily Challenges
  4a. daily_challenges + challenge_completions schema
  4b. pg_cron + Edge Function: generate-daily-challenge (midnight UTC)
  4c. ChallengeRepository + DailyChallengeViewModel
  4d. DailyChallenge screen (UI, countdown timer, reward animation)
  → Unblocks streak warnings (needs challenge completion data)

Phase 5: Push Notifications
  5a. FCM project setup, google-services.json in app
  5b. Edge Function: send-fcm-notification
  5c. pg_cron: streak-warning-notification (19:00 UTC)
  5d. Android: FirebaseMessagingService (foreground + background handling)
  → Unblocks smart notification settings UI

Phase 6: Social / Friends
  6a. friendships schema + RLS
  6b. FriendsRepository, FriendsViewModel
  6c. Friends screen (follow/unfollow, friend leaderboard)
  → Optional for MVP thesis demo

Phase 7: Next.js Admin Dashboard
  7a. Next.js App Router project init + Supabase SSR wiring
  7b. Auth middleware (guard /admin/*)
  7c. Vocabulary CRUD page (image upload to Supabase Storage)
  7d. User management page (view, ban, reset ELO)
  7e. Daily challenge management (create/schedule)
  7f. Analytics page (DAU chart, game mode usage — Supabase queries)
  → Can run in parallel with Phase 3-6 (independent web project)

Phase 8: Landing Page
  8a. Add to same Next.js project under app/(marketing)/ route group
  8b. Hero, features, screenshots, download link
  → Last; purely presentational
```

---

## Component Dependency Graph

```
SupabaseClient ──────────────────────────────────────────────┐
       │                                                       │
       ├── SupabaseAuthRepository ─── AuthViewModel           │
       │         │                         │                  │
       │         ▼                         ▼                  │
       │   public.users              Login / Register         │
       │         │                   Onboarding screens       │
       │         │                                            │
       ├── ProgressRepository ─── ProgressViewModel ──── UserProgressManager (existing)
       │         │                                            │
       │         ▼                                            ▼
       │   user_progress table                        SharedPreferences (offline)
       │
       ├── LeaderboardRepository ─── LeaderboardViewModel ─── LeaderboardScreen
       │         │
       │         ├── postgrest (initial fetch)
       │         └── realtime (postgres_changes subscription)
       │
       ├── ChallengeRepository ─── DailyChallengeViewModel ─── ChallengeScreen
       │         │
       │         └── daily_challenges / challenge_completions
       │
       ├── FriendsRepository ─── FriendsViewModel ─── FriendsScreen
       │         └── friendships table
       │
       └── Edge Functions (server-side only)
                 ├── send-fcm-notification ←── FCM API
                 ├── generate-daily-challenge ←── pg_cron (midnight UTC)
                 └── streak-warning ←── pg_cron (19:00 UTC)

Next.js Admin (independent project, same Supabase)
       ├── @supabase/ssr (server client — service_role)
       ├── Middleware → auth.getUser() → redirect if not admin
       ├── /admin/vocab → VocabCrudPage (Server Component)
       ├── /admin/users → UserManagementPage
       ├── /admin/challenges → ChallengeManagementPage
       └── /admin/analytics → AnalyticsPage (recharts / Chart.js)
```

---

## Key Technology Choices

| Concern | Choice | Version | Rationale |
|---------|--------|---------|-----------|
| Supabase Android SDK | `io.github.jan-tennert.supabase:postgrest-kt` | 3.x | Official Kotlin-first SDK; coroutine-native; all plugins modular |
| Supabase Realtime (Android) | `io.github.jan-tennert.supabase:realtime-kt` | 3.x | Same SDK, modular plugin; handles WebSocket reconnection |
| Supabase Auth (Android) | `io.github.jan-tennert.supabase:auth-kt` | 3.x | OAuth deep links + session persistence built in |
| Android HTTP | Ktor (bundled with supabase-kt) | 2.x | supabase-kt uses Ktor engine; no separate OkHttp needed for Supabase calls |
| FCM (Android) | `com.google.firebase:firebase-messaging-ktx` | 24.x | Official Google; KTX extensions for coroutines |
| Next.js | App Router | 14.x | Server Components + Middleware = clean auth guard; Vercel deploy |
| Supabase JS (web) | `@supabase/supabase-js` + `@supabase/ssr` | 2.x | SSR = server-side session; replaces deprecated auth-helpers |
| Admin UI components | shadcn/ui + Tailwind | latest | Radix primitives, accessible, no lock-in |
| Analytics charts | Recharts | 2.x | React-native charting, simple API, sufficient for admin |
| Scheduled jobs | pg_cron (built into Supabase) | — | No external scheduler needed; runs inside Postgres |
| Notification routing | Supabase Edge Functions (Deno) | — | Secrets management; avoids client-side FCM service account |

---

## Sources

- Supabase Kotlin SDK documentation: https://supabase.com/docs/reference/kotlin/introduction (HIGH confidence — official)
- Supabase Realtime documentation: https://supabase.com/docs/guides/realtime (HIGH confidence — official)
- Supabase RLS guide: https://supabase.com/docs/guides/database/postgres/row-level-security (HIGH confidence — official)
- Supabase Edge Functions: https://supabase.com/docs/guides/functions (HIGH confidence — official)
- Next.js + Supabase SSR: https://supabase.com/docs/guides/auth/server-side/nextjs (HIGH confidence — official)
- pg_cron in Supabase: https://supabase.com/docs/guides/database/extensions/pgcron (HIGH confidence — official)
- FCM HTTP v1 API: https://firebase.google.com/docs/cloud-messaging/send-message (HIGH confidence — official)
- supabase-kt GitHub: https://github.com/supabase-community/supabase-kt (HIGH confidence — primary source)

---

*Architecture analysis: 2026-07-22*
