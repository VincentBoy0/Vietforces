# Technical Concerns

**Analysis Date:** 2026-07-24

---

## Critical Issues

### 1. openai-proxy Edge Function Has No JWT/Authentication Verification

- **Issue:** `supabase/functions/openai-proxy/index.ts` accepts any HTTP request that carries the Supabase anon key in the `apikey` header. It does not validate a caller JWT or require an authenticated session. The anon key is baked into the compiled APK via `BuildConfig.SUPABASE_ANON_KEY` (set in `app/src/main/java/com/example/vietforces/di/SupabaseModule.kt`) and is trivially extractable via APK decompilation.
- **Files:** `supabase/functions/openai-proxy/index.ts`, `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`
- **Impact:** Any attacker who decompiles the APK and extracts the anon key can send unlimited chat-completion requests to OpenAI through your proxy, incurring unbounded API costs. The other two scheduled functions (`generate-daily-challenge`, `send-streak-reminder`) each verify a `CRON_SECRET` header, but the openai-proxy has no equivalent guard.
- **Fix approach:** Add a JWT verification block at the top of the handler. Use `createClient` with the caller's JWT to verify the session via `supabase.auth.getUser()` and return 401 if the user is not authenticated:
  ```ts
  const authHeader = req.headers.get('Authorization') ?? ''
  const supabase = createClient(supabaseUrl, anonKey, { global: { headers: { Authorization: authHeader } } })
  const { data: { user }, error } = await supabase.auth.getUser()
  if (error || !user) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 })
  ```
- **Severity:** CRITICAL

---

## High Priority

### 2. refresh-streak-freeze Edge Function Has No Authorization Check

- **Issue:** `supabase/functions/refresh-streak-freeze/index.ts` has no `CRON_SECRET` verification or any authentication check. The handler accepts any incoming POST request and immediately calls `supabase.rpc("grant_streak_freeze")` with the service-role key, granting streak freezes to all users.
- **Files:** `supabase/functions/refresh-streak-freeze/index.ts`
- **Impact:** Any unauthenticated HTTP caller can trigger unlimited streak-freeze grants to all users, undermining the weekly streak-freeze economy. The sibling functions `generate-daily-challenge/index.ts` (lines 54–70) and `send-streak-reminder/index.ts` (~lines 140–155) both implement `CRON_SECRET` checks correctly — this one was not updated.
- **Fix approach:** Add the same CRON_SECRET pattern used in `generate-daily-challenge/index.ts`. Verify `req.headers.get('Authorization') === Bearer ${cronSecret}` before processing.
- **Severity:** HIGH

### 3. `users_select_public_username` RLS Policy Exposes All users Table Columns

- **Issue:** Migration `supabase/migrations/007_activity_feed.sql` adds `CREATE POLICY "users_select_public_username" ON public.users FOR SELECT USING (TRUE)`. Because Supabase RLS policies are OR'd, any authenticated user can now SELECT all columns of `public.users` — including `fcm_token`, `is_admin`, `is_banned`, `notif_streak_enabled`, `notif_daily_enabled`. The narrow `users_public` view created in `supabase/migrations/009_security_fixes.sql` (exposing only `id, username`) does not prevent direct base-table access.
- **Files:** `supabase/migrations/007_activity_feed.sql` (lines ~95–105), `supabase/migrations/009_security_fixes.sql`
- **Impact:** An authenticated user querying `public.users` directly can enumerate all FCM push tokens (enabling unauthorized push notification targeting), identify admin accounts via `is_admin`, and read notification preference flags for all users.
- **Fix approach:** Replace `USING (TRUE)` with column-level restrictions. Either: (a) revoke direct SELECT on `public.users` for the `authenticated` role and grant SELECT only on the `users_public` view, or (b) create a column-level security grant excluding `fcm_token` and `is_admin` from public visibility.
- **Severity:** HIGH

### 4. `progress_select_public` RLS Policy Exposes All user_progress Columns

- **Issue:** `supabase/migrations/007_activity_feed.sql` adds `CREATE POLICY "progress_select_public" ON public.user_progress FOR SELECT USING (TRUE)`. This allows any authenticated user to read all columns for any user's progress record — including `words_learned` (full JSONB vocabulary history array), `streak_freeze_count`, and `last_practice_date`.
- **Files:** `supabase/migrations/007_activity_feed.sql` (lines ~110–120)
- **Impact:** An authenticated user can enumerate all other users' full learning histories. The `user_progress_public` view (migration 009) exposes only `user_id, elo_score, streak_count, total_games` but does not prevent access to the base table's additional columns.
- **Fix approach:** Replace `USING (TRUE)` with a policy scoped to own row or followed users: `USING (user_id = auth.uid() OR EXISTS (SELECT 1 FROM public.friendships WHERE follower_id = auth.uid() AND following_id = user_progress.user_id))`. New code referencing public profile stats should use the `user_progress_public` view.
- **Severity:** HIGH

### 5. Web-Admin Server Actions Do Not Independently Verify is_admin

- **Issue:** All server actions in `web-admin/src/lib/actions/` (`vocabulary.ts`, `users.ts`, `analytics.ts`, `daily-challenges.ts`) use `createAdminClient()` which initializes a service-role Supabase client that bypasses RLS entirely. These actions perform no self-contained `is_admin` check — authorization is delegated entirely to `web-admin/middleware.ts`, which matches only the `/admin/:path*` and `/login` page routes.
- **Files:** `web-admin/src/lib/supabase/admin.ts`, `web-admin/src/lib/actions/vocabulary.ts`, `web-admin/src/lib/actions/users.ts`, `web-admin/middleware.ts`
- **Impact:** Next.js Server Actions are invocable via HTTP POST. An authenticated non-admin user who possesses a valid session cookie and the action ID could execute privileged mutations (banning users, modifying vocabulary) because the middleware `matcher` does not intercept `/_next/action` POST requests in all Next.js versions.
- **Fix approach:** Add an `is_admin` check at the start of each mutating server action:
  ```ts
  const supabase = await createClient()  // anon client with caller's session
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) throw new Error('Unauthenticated')
  const { data: profile } = await supabase.from('users').select('is_admin').eq('id', user.id).single()
  if (!profile?.is_admin) throw new Error('Forbidden')
  ```
- **Severity:** HIGH

---

## Medium Priority

### 6. update_streak RPC Accepts Client-Supplied Future Date

- **Issue:** `supabase/migrations/003_streak_function.sql` and its patch in `009_security_fixes.sql` define `update_streak(p_user_id UUID, p_today_date DATE)`. The function is directly callable by authenticated users (`GRANT EXECUTE ... TO authenticated`). While `award_daily_bonus` correctly passes `CURRENT_DATE` when calling `update_streak` internally, the Android client also calls `update_streak` directly from `StreakRepository.updateStreak()` passing `todayUtcString()`. A malicious caller invoking the RPC directly could pass a future date to pre-claim a streak day.
- **Files:** `supabase/migrations/003_streak_function.sql`, `supabase/migrations/009_security_fixes.sql`, `app/src/main/java/com/example/vietforces/data/repository/StreakRepository.kt` (lines 89–107)
- **Impact:** A user can call the RPC with a future date, making the server record a streak day for a day they haven't practiced. This allows streak preloading without actual practice.
- **Fix approach:** Add server-side validation: `IF p_today_date > CURRENT_DATE THEN RAISE EXCEPTION 'invalid_date' USING HINT = 'Date cannot be in the future'; END IF;`
- **Severity:** MEDIUM

### 7. Username Uniqueness Race Condition in handle_new_user Trigger

- **Issue:** `supabase/migrations/011_handle_new_user_trigger.sql` (lines ~14–27) uses a non-atomic SELECT-check-then-INSERT for username collision handling: if `v_username` already exists, it appends a 4-char UUID suffix. Two concurrent registrations with the same email prefix can both pass the EXISTS check simultaneously and both attempt to INSERT the same derived username, causing one registration to fail with a UNIQUE constraint violation.
- **Files:** `supabase/migrations/011_handle_new_user_trigger.sql`
- **Impact:** Simultaneous registrations from users with identical email prefixes will fail. The `ON CONFLICT (id) DO NOTHING` handles the PK collision but does not handle the UNIQUE constraint on `username`.
- **Fix approach:** Generate the username deterministically from the UUID without a conditional check — since `auth.users.id` is always unique, `username = 'user_' || SUBSTR(NEW.id::TEXT, 1, 12)` will always be unique. The current email-prefix preference can still be tried first with an `ON CONFLICT (username) DO UPDATE SET username = 'user_' || SUBSTR(NEW.id::TEXT, 1, 12)`.
- **Severity:** MEDIUM

### 8. VocabularyRepository.kt Is 1328 Lines — Monolithic

- **Issue:** `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt` is 1,328 lines, while all other repositories range from 78–226 lines. The file appears to contain inline hardcoded vocabulary data arrays alongside repository logic.
- **Files:** `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt`
- **Impact:** Vocabulary data maintenance requires modifying a large Kotlin class. Words added to the `public.words` database table are not reflected in the in-memory vocabulary unless the repository is updated. Inflates APK size and build times.
- **Fix approach:** Extract vocabulary data to JSON asset files under `app/src/main/assets/`. Repository reads from assets on first access and queries DB for dynamic words. The `generate-daily-challenge` edge function also duplicates vocabulary IDs — consolidating to DB as the single source of truth removes this duplication.
- **Severity:** MEDIUM

### 9. generate-daily-challenge Hardcoded Vocabulary Pool Out of Sync with DB

- **Issue:** `supabase/functions/generate-daily-challenge/index.ts` contains a hardcoded `VOCABULARY_POOL` array of 154 IDs (lines ~55–115) that "mirrors VocabularyRepository.allVocabulary". The IDs use an internal naming scheme (`animal_001`, `food_003`, etc.) that must be kept in sync across three artifacts: the Kotlin repository, the edge function, and `public.words`.
- **Files:** `supabase/functions/generate-daily-challenge/index.ts`
- **Impact:** Daily challenges will never include newly added vocabulary until the function is manually updated and redeployed.
- **Fix approach:** Replace the static pool with a dynamic DB query: `const { data } = await supabase.from('words').select('id').order('RANDOM()').limit(10)`. This keeps challenges in sync automatically.
- **Severity:** MEDIUM

### 10. LeaderboardEntry Model Declares a rank Column That Does Not Exist in DB

- **Issue:** `app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt:22` declares `@SerialName("rank") val rank: Int? = null`. The `public.leaderboard` table has no `rank` column — it has `rank_tier` (TEXT). The comment on line 14 also incorrectly lists the columns as "user_id, username, elo_score, weekly_elo, **rank**, last_updated" (should be `rank_tier`, `updated_at`).
- **Files:** `app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt` (lines 14–23)
- **Impact:** The `rank` field is always `null` at runtime. While harmless, it misleads developers into assuming rank is server-provided. Actual rank is computed client-side in `LeaderboardViewModel` via `indexOfFirst`.
- **Fix approach:** Remove the `rank` field. Add `@SerialName("rank_tier") val rankTier: String = "Newbie"` to correctly map the DB column. Update the comment on line 14.
- **Severity:** MEDIUM

### 11. No Next.js Error Boundaries (error.tsx) in web-admin

- **Issue:** Neither `web-admin/src/app/` nor any subdirectory contains an `error.tsx` file. Without App Router error boundaries, any unhandled exception in a Server Component (e.g., Supabase query failure) propagates to the Next.js default error page with no recovery path.
- **Files:** `web-admin/src/app/admin/` (all pages), `web-admin/src/app/layout.tsx`
- **Impact:** If a Supabase query fails (network timeout, schema mismatch), the admin user sees a full error screen with no navigation or retry. All admin workflows are blocked until the user navigates back manually.
- **Fix approach:** Add `web-admin/src/app/admin/error.tsx` with a recovery UI (retry button + link back to dashboard), following Next.js App Router conventions.
- **Severity:** MEDIUM

---

## Low Priority / Nice-to-Fix

### 12. MascotFeedbackManager Coroutine Scope Is Never Cancelled

- **Issue:** `app/src/main/java/com/example/vietforces/ui/components/DraggableMascot.kt:54` defines `MascotFeedbackManager` (Kotlin `object`) with `private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())`. As a process-scoped singleton, this scope is never cancelled. AI network requests can complete after the user has exited any screen.
- **Files:** `app/src/main/java/com/example/vietforces/ui/components/DraggableMascot.kt` (line 54)
- **Impact:** Not a classic memory leak (no Activity/Context references). However, AI jobs fire network calls even when no screen displays the mascot, consuming bandwidth and updating shared Compose state during recomposition.
- **Fix approach:** Add AI job cancellation when `hideFeedback()` is called: `aiJob?.cancel()`. This is sufficient since the UI won't observe the result anyway.
- **Severity:** LOW

### 13. LeaderboardViewModel Does Full Reload on Every Realtime Event Without Debouncing

- **Issue:** `app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt` merges INSERT and UPDATE Realtime flows and calls `loadLeaderboard()` on every event (lines 130–140). A burst of score submissions (end-of-day play) triggers one full Supabase query per event.
- **Files:** `app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt`
- **Impact:** Currently low impact. At scale, burst events cause redundant queries and visible UI flicker.
- **Fix approach:** Add `.debounce(500)` or `.conflate()` between the merged flow and `onEach { loadLeaderboard() }`.
- **Severity:** LOW

### 14. send-streak-reminder Sends FCM Notifications Sequentially

- **Issue:** `supabase/functions/send-streak-reminder/index.ts` iterates eligible users in a sequential `for` loop, issuing one HTTP call to FCM per user.
- **Files:** `supabase/functions/send-streak-reminder/index.ts` (lines ~185–205)
- **Impact:** With hundreds of eligible users, the function could hit Supabase Edge Function execution time limits (default 150s).
- **Fix approach:** Replace the sequential loop with `Promise.allSettled()` with a concurrency cap of ~10 simultaneous requests.
- **Severity:** LOW

### 15. supabase/config.toml Is Nearly Empty

- **Issue:** `supabase/config.toml` contains only `[api] port = 54321` and `[db] port = 54322`. No JWT expiry, email confirmation, password policy, or auth settings are specified.
- **Files:** `supabase/config.toml`
- **Impact:** All production security settings must be configured manually in the Supabase Dashboard and are not version-controlled, preventing environment parity checks from the repository.
- **Fix approach:** Expand with `[auth]` section covering `jwt_expiry`, `enable_signup`, `minimum_password_length`, `email.enable_confirmations` per Supabase CLI config schema.
- **Severity:** LOW

### 16. No Index on users.username for Admin ILIKE Search

- **Issue:** `web-admin/src/lib/actions/users.ts:listUsers()` runs `.ilike('username', '%${search}%')`. No GIN trigram index exists on `users.username` — the B-tree UNIQUE index does not accelerate `%...%` ILIKE pattern matching.
- **Files:** `web-admin/src/lib/actions/users.ts`, `supabase/migrations/001_initial_schema.sql`
- **Fix approach:** Add a migration with `CREATE INDEX idx_users_username_trgm ON public.users USING GIN (username gin_trgm_ops);` (requires `pg_trgm` extension).
- **Severity:** LOW

### 17. No Indexes on leaderboard.elo_score or weekly_elo

- **Issue:** Leaderboard ORDER BY queries (`ORDER BY elo_score DESC LIMIT 50`, `ORDER BY weekly_elo DESC LIMIT 50`) have no supporting indexes. Only the PK index on `user_id` exists.
- **Files:** `supabase/migrations/001_initial_schema.sql`, `app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt`
- **Fix approach:** Add `CREATE INDEX idx_leaderboard_elo_score ON public.leaderboard (elo_score DESC)` and `CREATE INDEX idx_leaderboard_weekly_elo ON public.leaderboard (weekly_elo DESC)`.
- **Severity:** LOW

### 18. PlaceholderScreen Is Reachable from Live Navigation

- **Issue:** `app/src/main/java/com/example/vietforces/ui/screens/PlaceholderScreens.kt` exists and routes in the navigation graph point to it, displaying "🚧 Đang phát triển..." to users.
- **Files:** `app/src/main/java/com/example/vietforces/ui/screens/PlaceholderScreens.kt`, `app/src/main/java/com/example/vietforces/ui/screens/MainScreen.kt`
- **Fix approach:** Hide navigation items that lead to placeholders until features are implemented.
- **Severity:** LOW

### 19. No Test Coverage

- **Issue:** No test files (`*.test.*`, `*.spec.*`, `*Test.kt`) were found in any of the four sub-projects (`app/`, `web-admin/`, `web-landing/`, `supabase/`).
- **Files:** Entire codebase
- **Impact:** Regressions in critical paths (ELO calculation, streak logic, auth flows) are caught only manually. The ELO algorithm and streak freeze logic in particular are complex state machines with no automated verification.
- **Fix approach:** Start with unit tests for `EloRepository.calculateElo()` logic (mock Supabase) and SQL function tests using `pgTAP` or Supabase's built-in test runner.
- **Severity:** LOW

---

## Security Assessment

### Overall Posture

The codebase shows deliberate security thinking, especially in the Supabase layer. Key mitigations in place:

- **RLS enabled on all 9 tables** — every table has `ENABLE ROW LEVEL SECURITY`
- **SECURITY DEFINER RPCs for all score mutations** — ELO, streak, and daily bonus computed server-side; clients cannot write scores directly
- **auth.uid() identity guard on all three core RPCs** — `calculate_elo`, `update_streak`, `award_daily_bonus` all reject `p_user_id <> auth.uid()` (CR-02 patch, migration 009)
- **CRON_SECRET on most scheduled Edge Functions** — `generate-daily-challenge` and `send-streak-reminder` verify Authorization header
- **OpenAI key never in APK** — routes through `openai-proxy` Edge Function; key stored only in Supabase secrets
- **Admin auth layered** — middleware verifies `is_admin` on every `/admin/*` page request; service-role key used only in SSR server actions
- **Trigger-only inserts for activity_events and streak_history** — no direct client INSERT policy; SECURITY DEFINER functions write these tables
- **SET search_path = public** on all SECURITY DEFINER functions prevents search_path injection

### Remaining Gaps

| Gap | File | Severity |
|-----|------|----------|
| openai-proxy: no JWT auth check | `supabase/functions/openai-proxy/index.ts` | CRITICAL |
| refresh-streak-freeze: no CRON_SECRET check | `supabase/functions/refresh-streak-freeze/index.ts` | HIGH |
| users table: RLS exposes fcm_token + is_admin to all authenticated users | `supabase/migrations/007_activity_feed.sql` | HIGH |
| user_progress table: RLS exposes all columns to all authenticated users | `supabase/migrations/007_activity_feed.sql` | HIGH |
| web-admin server actions: no independent is_admin check | `web-admin/src/lib/actions/*.ts` | HIGH |
| update_streak: client can supply future date for streak preloading | `supabase/migrations/003_streak_function.sql` | MEDIUM |

---

## Performance Concerns

### Leaderboard Full-Table Scan on ORDER BY Queries
- Queries `ORDER BY elo_score DESC LIMIT 50` and `ORDER BY weekly_elo DESC LIMIT 50` run without supporting indexes
- **File:** `supabase/migrations/001_initial_schema.sql`, `app/src/main/java/com/example/vietforces/data/repository/LeaderboardRepository.kt`
- **Fix:** Add `DESC` indexes on both score columns (see item 17)

### Sequential FCM Notification Loop
- `send-streak-reminder` iterates users serially with one HTTP call per user
- **File:** `supabase/functions/send-streak-reminder/index.ts`
- **Fix:** Batch with `Promise.allSettled` concurrency (see item 14)

### Admin ILIKE User Search Without Trigram Index
- Full-table sequential scan on `users.username` for every admin user search
- **File:** `web-admin/src/lib/actions/users.ts`
- **Fix:** `pg_trgm` GIN index (see item 16)

### words_learned JSONB Array Grows Unboundedly
- `public.user_progress.words_learned` is a JSONB array with no size cap
- **File:** `supabase/migrations/001_initial_schema.sql:49`
- Currently bounded by the finite vocabulary set (~154 items); becomes a concern if vocabulary grows to thousands of entries

---

## Technical Debt

### Vocabulary Data Duplicated Across Three Artifacts
- The vocabulary word list is duplicated in: (a) `VocabularyRepository.kt` (1,328 lines, inline Kotlin), (b) `generate-daily-challenge/index.ts` (hardcoded `VOCABULARY_POOL` array), and (c) `public.words` DB table. Any modification requires updating all three.
- **Refactor path:** DB as single source of truth. `VocabularyRepository` reads from `public.words` (with local caching). `generate-daily-challenge` queries the DB dynamically.

### AiManager as Global Kotlin object with Compose State
- **File:** `app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`
- Holds `var aiFeedbackEnabled by mutableStateOf(true)` at process level, coupling data layer to Compose runtime. AI toggles bypass proper lifecycle management.
- **Refactor path:** Convert `AiManager` to a Hilt `@Singleton` class injected into ViewModels; expose AI toggles as `StateFlow<Boolean>`.

### Redundant UPDATE Policy on public.users
- Migration 001 adds `users_update_own` and migration 010 adds `users_update_notif_prefs` — both use identical `USING (id = auth.uid()) WITH CHECK (id = auth.uid())`. The second policy is entirely redundant.
- **File:** `supabase/migrations/001_initial_schema.sql`, `supabase/migrations/010_notif_preferences.sql`

---

## Positive Observations

These patterns are well-implemented and should be preserved:

1. **SECURITY DEFINER + auth.uid() identity guard** — `calculate_elo`, `update_streak`, and `award_daily_bonus` all validate `p_user_id <> auth.uid()` as their first statement. Correct defense-in-depth for multi-user score systems.

2. **Dedicated cleanupScope in LeaderboardViewModel** — Correctly uses a separate `CoroutineScope(SupervisorJob() + Dispatchers.IO)` for `onCleared()` because `viewModelScope` is already cancelled before `onCleared()` runs. Prevents Realtime channel leaks. (`app/src/main/java/com/example/vietforces/ui/viewmodel/LeaderboardViewModel.kt`)

3. **@Volatile on companion object instance vars** — `StreakRepository.instance` and `ProgressRepository.instance` are correctly annotated with `@Volatile` for cross-thread visibility.

4. **Server-side date for streak credit (WA-06)** — `award_daily_bonus` uses `CURRENT_DATE` (not client-supplied `p_challenge_date`) when crediting the streak, preventing backdated completion from manipulating streaks.

5. **CRON_SECRET auth on scheduled functions** — Both `generate-daily-challenge` and `send-streak-reminder` verify a shared secret before executing.

6. **Middleware uses getUser() not getSession()** — `web-admin/middleware.ts` uses `supabase.auth.getUser()` (server-side JWT validation) rather than `getSession()` (client-side cookie only), following Supabase SSR security guidance.

7. **SET search_path = public on all SECURITY DEFINER functions** — Prevents search_path injection attacks consistently across all PL/pgSQL functions.

8. **OkHttpClient shared singleton in OpenAiClient** — `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt` uses `by lazy { OkHttpClient.Builder()... }` correctly sharing the connection pool across all AI requests.

9. **PreferencesManager initialized with Application context** — `app/src/main/java/com/example/vietforces/VietForcesApplication.kt:20` calls `PreferencesManager.init(this)` with the Application context, preventing any Activity context leak.

---

*Concerns audit: 2026-07-24*
