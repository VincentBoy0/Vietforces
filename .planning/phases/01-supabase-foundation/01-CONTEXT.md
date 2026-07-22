# Phase 1: Supabase Foundation — Context

**Gathered:** 2026-07-22
**Status:** Ready for planning
**Mode:** Auto-generated (autonomous mode)

<domain>
## Phase Boundary

Set up the complete Supabase backend infrastructure that all subsequent phases depend on:
1. Supabase project with 6-table schema + RLS policies
2. Android: `SupabaseClient` singleton via Hilt DI
3. Android: `supabase-kt` dependencies added to Gradle
4. OpenAI key proxy: Supabase Edge Function replaces direct APK key
5. Keepalive: prevent Supabase free tier pause (GitHub Actions cron)

This phase is **infrastructure only** — no user-facing UI changes. The app still works identically from the user's perspective, but the backend skeleton is in place.

</domain>

<decisions>
## Implementation Decisions

### Supabase Schema (6 tables)
```sql
-- users: extends Supabase auth.users
public.users (id uuid PK FK auth.users, username text UNIQUE, timezone text, avatar_url text, is_banned bool, fcm_token text, created_at timestamptz)

-- user_progress: cloud sync of local progress
public.user_progress (id uuid PK, user_id uuid FK users, elo_score int, streak_count int, streak_freeze_count int, last_practice_date date, total_games int, words_learned jsonb, updated_at timestamptz)

-- leaderboard: materialized view or denormalized for Realtime
public.leaderboard (user_id uuid PK FK users, username text, elo_score int, weekly_elo int, rank_tier text, updated_at timestamptz)

-- daily_challenges: server-generated
public.daily_challenges (id uuid PK, challenge_date date UNIQUE, game_mode text, vocabulary_ids jsonb, bonus_elo int, created_at timestamptz)

-- friendships: asymmetric follow
public.friendships (follower_id uuid FK users, following_id uuid FK users, created_at timestamptz, PRIMARY KEY (follower_id, following_id))

-- fcm_tokens: push notification tokens
public.fcm_tokens (user_id uuid PK FK users, token text NOT NULL, updated_at timestamptz)
```

### RLS Policies (per table)
- `users`: SELECT own row, UPDATE own row
- `user_progress`: SELECT/INSERT/UPDATE own row only
- `leaderboard`: SELECT all (public), INSERT/UPDATE via service role only
- `daily_challenges`: SELECT all (public), INSERT via service role only
- `friendships`: SELECT own follow relationships, INSERT/DELETE own
- `fcm_tokens`: SELECT/INSERT/UPDATE own token only

### Supabase-kt Version & Dependencies
- BOM: `io.github.jan-tennert.supabase:bom:3.7.0`
- Modules: `postgrest-kt`, `auth-kt`, `realtime-kt`, `storage-kt`
- HTTP engine: `io.ktor:ktor-client-okhttp` (Android-reliable)
- Add to `app/build.gradle.kts` dependencies block

### Hilt Setup
- Add Hilt plugin: `com.google.dagger:hilt-android-gradle-plugin:2.60.1`
- Add to root `build.gradle.kts` plugins
- Add `@HiltAndroidApp` to `Application` class (create `VietForcesApplication.kt` if not exists)
- Create `SupabaseModule.kt` in `di/` package — provides singleton `SupabaseClient`
- Add `android:name=".VietForcesApplication"` to `AndroidManifest.xml`

### Supabase Config
- Supabase URL and anon key stored in `local.properties` (git-ignored)
- Exposed via `BuildConfig` fields (same pattern as existing `OPENAI_API_KEY`)
- `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`

### OpenAI Proxy Edge Function
- Create `supabase/functions/openai-proxy/index.ts` (Deno Edge Function)
- Function receives the chat completion request, adds OpenAI key from Supabase env, forwards to OpenAI API
- Android `AiManager.kt` updated to call Supabase Edge Function URL instead of OpenAI directly
- `OPENAI_API_KEY` BuildConfig field set to empty string `""` (key lives only in Supabase secrets)

### Keepalive
- Create `.github/workflows/supabase-keepalive.yml`
- Runs on cron: `0 12 * * *` (daily at noon UTC)
- Makes a simple HTTP GET to the Supabase REST API health endpoint
- Prevents 7-day inactivity pause on free tier

</decisions>

<code_context>
## Existing Code Insights

- `AiManager.kt`: currently calls OpenAI API directly with `BuildConfig.OPENAI_API_KEY` — needs update to call Edge Function
- `VietForcesApplication` class: may not exist yet — check `AndroidManifest.xml` for `android:name`
- Package structure: `com/example/vietforces/` — add `di/` subdirectory for Hilt modules
- `local.properties`: already has `OPENAI_API_KEY` — add `SUPABASE_URL` and `SUPABASE_ANON_KEY` fields
- `app/build.gradle.kts`: already has `buildConfigField` pattern for API keys — reuse for Supabase config

</code_context>

<specifics>
## Specific Implementation Notes

- Do NOT add any UI screens in this phase — pure infrastructure
- The Edge Function should be a thin proxy — no business logic
- Hilt application component must be set up before any @Inject usage
- supabase-kt uses Kotlin coroutines throughout — existing coroutine scope patterns in codebase should be reused
- Keep `OPENAI_MODEL` BuildConfig field — only empty the key value

</specifics>

<deferred>
## Deferred

- Actual Supabase project creation (requires user credentials) — plans will include SQL migration files and instructions
- Google OAuth redirect URI configuration — deferred to Phase 2
- Any data migration from SharedPreferences — deferred to Phase 2
- FCM token registration logic — deferred to Phase 4

</deferred>
