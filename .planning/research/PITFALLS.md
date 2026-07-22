# Domain Pitfalls — VietForces

**Domain:** Android gamified language-learning app with Supabase backend + social features
**Researched:** 2026-07-22
**Confidence:** HIGH (all findings cross-referenced with project codebase, official SDK docs, and established Android/Supabase community patterns)

---

## 🔴 Critical Pitfalls

Mistakes that cause rewrites, security incidents, or data loss.

---

### Pitfall C-1: OpenAI API Key Baked Into APK

**What goes wrong:** `BuildConfig.OPENAI_API_KEY` is compiled as a plain string into the DEX. Any APK (debug or release) is trivially reversed with `apktool d app.apk` or `strings classes.dex | grep sk-`. Even with R8/ProGuard enabled (`isMinifyEnabled = true`), string _values_ are not obfuscated — only class/method names are. The key will be found and abused.

**Why it happens:** The current build injects the key via `buildConfigField` in `build.gradle.kts`. `isMinifyEnabled = false` in the release block makes extraction even easier, but enabling minify does not actually protect the key.

**Consequences:** Stolen OpenAI key → unexpected billing charges with no way to attribute usage to the attacker; requires immediate key rotation which breaks all running app instances.

**Codebase evidence:** `app/build.gradle.kts` lines 44-45, `OpenAiClient.kt` — already flagged in CONCERNS.md.

**Prevention:**
1. **Phase: Supabase integration** — Create a Supabase Edge Function (`/functions/ai-proxy`) that holds `OPENAI_API_KEY` as a Supabase secret environment variable. The Android app calls the Edge Function (authenticated with the user's Supabase JWT) instead of OpenAI directly. The key never touches the client.
2. Remove `OPENAI_API_KEY` from `local.properties` + `build.gradle.kts` entirely once proxy is live.
3. Enable `isMinifyEnabled = true` for release regardless — it reduces APK size and adds marginal obfuscation.

**Detection (warning sign):** If `BuildConfig.OPENAI_API_KEY` appears in any `grep -r BuildConfig app/src` output, the key is in the binary.

**Phase that must address it:** Phase 1 (Supabase setup) — do NOT ship a phase that adds Supabase auth without also moving AI calls behind the proxy. Each deployment widens exposure.

---

### Pitfall C-2: Supabase Anon Key Exposed (But That's Fine) vs. Service Role Key Leaked (That's Fatal)

**What goes wrong:** Developers copy the service role key from the Supabase dashboard into the Android app, thinking "it's just like the anon key." The service role key **bypasses all RLS policies entirely** — a user holding it can read, write, and delete any row in any table.

**Why it happens:** The Supabase dashboard shows both keys side-by-side. The service role key looks like a longer JWT and feels more "powerful" — some developers choose it thinking it gives better access.

**Consequences:** Complete database compromise. All user data, all ELO scores, all auth tokens readable/writable by anyone who decompiles the app.

**Prevention:**
- Android app uses **only** the `anon` (public) key.
- Service role key lives **only** in Next.js admin dashboard server-side environment variables (`SUPABASE_SERVICE_ROLE_KEY` in `.env.local`, never in client code) and in Supabase Edge Function secrets.
- Add a `grep -r "service_role" app/src` check to CI or pre-commit hook.

**Detection:** Supabase dashboard → Project Settings → API → "service_role" key. If it appears anywhere in your Android source or `local.properties`, it's wrong.

**Phase:** Phase 1 (Supabase setup). Put this rule in the README on day one.

---

### Pitfall C-3: RLS Disabled or Misconfigured — Ghost Authorization

**What goes wrong:** You enable RLS on a table and add one SELECT policy, then create other tables without policies. Any `INSERT` or `UPDATE` on a policy-less table silently returns 0 rows affected with no error, or worse, returns an empty result set that looks like success. Users can read other users' private data if policies are wrong.

**Why it happens:** RLS is opt-in per table. New tables default to RLS disabled. The Supabase client returns `[]` (empty array) for RLS-blocked reads, not an error — easy to confuse with "user has no records yet."

**Consequences for VietForces:**
- `progress` table without RLS → any authenticated user can read/overwrite any other user's ELO and streak.
- `friendships` table with only SELECT policy → INSERT blocked silently; friend requests appear to send but nothing is saved.
- `daily_challenges` table without proper INSERT policy → admin can't add challenges from the dashboard.

**Prevention:**
```sql
-- Always verify RLS is ON for every table:
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_challenges ENABLE ROW LEVEL SECURITY;

-- Test every operation (SELECT/INSERT/UPDATE/DELETE) explicitly:
-- Use the Supabase SQL editor, log in as a specific user, and verify
-- that cross-user access is blocked.
```

**Detection:** In the Supabase dashboard → Table Editor → any table → check the shield icon. Run `SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public';` — every row must show `rowsecurity = true`.

**Phase:** Phase 1 (DB schema creation). Write RLS policies immediately after each `CREATE TABLE` — never defer.

---

### Pitfall C-4: Client-Side ELO — Trivially Cheatable

**What goes wrong:** The existing local ELO system calculates score changes in `UserProgressManager.kt` on-device. If this calculation is carried over to the online leaderboard (client sends "I gained +32 ELO"), any user can replay or modify the request to inject arbitrary ELO values.

**Why it happens:** It's the natural extension of the existing local system. The Supabase `upsert` on the `progress` table seems straightforward — just send the new total.

**Consequences:** Leaderboard filled with users at ELO 9999 within hours of launch. The academic demo becomes unusable for demonstrating social features.

**Prevention:**
1. **Server-authoritative ELO via PostgreSQL function:**
```sql
CREATE OR REPLACE FUNCTION submit_game_result(
  p_user_id UUID,
  p_game_mode TEXT,
  p_score INT,        -- raw game score (0-100)
  p_opponent_elo INT  -- ELO of matched "virtual opponent"
)
RETURNS INT           -- returns new ELO
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
  current_elo INT;
  elo_change INT;
BEGIN
  -- Validate score is within plausible range
  IF p_score < 0 OR p_score > 100 THEN
    RAISE EXCEPTION 'Invalid score';
  END IF;
  SELECT elo INTO current_elo FROM progress WHERE user_id = p_user_id;
  -- Standard ELO formula
  elo_change := ROUND(32 * (p_score/100.0 - 1/(1+POWER(10, (p_opponent_elo - current_elo)/400.0))));
  -- Cap change per game to prevent runaway
  elo_change := GREATEST(-50, LEAST(50, elo_change));
  UPDATE progress SET elo = elo + elo_change WHERE user_id = p_user_id;
  RETURN current_elo + elo_change;
END;
$$;
```
2. RLS policy: users can only **read** their own `progress.elo`; they cannot **write** it directly. Only `SECURITY DEFINER` functions can update ELO.
3. Add rate limiting: reject ELO updates more frequent than 1 per 60 seconds per user (prevents replay attacks via rapid submission).

**Detection:** If the Android client is calling `supabase.from("progress").update(mapOf("elo" to newElo))` directly, ELO is client-controlled.

**Phase:** Phase 2 (leaderboard/social). Establish the PostgreSQL function before building the leaderboard UI.

---

### Pitfall C-5: Daily Challenge Timezone Mismatch — Silent Streak Breaks

**What goes wrong:** The server stores challenges with a UTC date key (`2026-07-22`). The app displays "today's challenge" based on the device's local time. A user in Vietnam (UTC+7) who completes the challenge at 11pm local time has their completion stored against UTC date `2026-07-21`, not `2026-07-22`. The next morning they see "streak broken" even though they played last night.

**Why it happens:** The existing `SimpleDateFormat` bug in `UserProgressManager.kt` already shows this pattern — the codebase uses device locale for date keys. Carrying this approach to server-side dates creates a systematic mismatch for all Vietnamese users (UTC+7).

**Consequences:** False streak breaks erode trust in the core engagement loop — streak loss is the most emotionally impactful negative event in gamified learning apps (see Duolingo incident discussions).

**Prevention:**
1. **Server side:** Store challenge date as a date column with the challenge's "effective timezone offset" metadata, or use a `date` column in UTC+7 explicitly: `CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh'`.
2. **Completion timestamp:** Store `completed_at TIMESTAMPTZ` (absolute UTC timestamp). Determine whether it counts for "today" using the user's local date at time of completion, not server UTC.
3. **Streak calculation:** Run streak checks in a PostgreSQL function that converts timestamps to `Asia/Ho_Chi_Minh` timezone before comparing dates:
```sql
DATE(completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh') = CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh'
```
4. **Grace period:** Add a 2-hour grace period after midnight for streak continuation (Duolingo does 4 hours) — reduces false breaks from late-night play.
5. **Fix existing bug:** Replace `Locale.getDefault()` with `Locale.ROOT` in `UserProgressManager.kt` date formatters before migration.

**Detection (warning sign):** If any date comparison uses `SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())` or compares against `LocalDate.now()` without explicit timezone, timezone bugs are present.

**Phase:** Phase 2 (daily challenges). Design the schema with timezone from the start — retrofitting timezone handling after data is written is expensive.

---

## 🟡 Moderate Pitfalls

Mistakes that cause regressions, bad UX, or significant rework — but not catastrophic failures.

---

### Pitfall M-1: Supabase Auth Token Refresh Failure — Silent 401 Loops

**What goes wrong:** Supabase JWTs expire after 1 hour by default. The `supabase-kt` SDK auto-refreshes tokens, but only if the session was properly persisted and the app has network access at refresh time. If the app is backgrounded for >1 hour, the token expires. On foreground, the SDK attempts refresh — but if the device has brief network loss during that window, the refresh fails and the SDK may leave the user in a broken "authenticated but with expired token" state. Subsequent API calls return 401 errors that look like permission errors, not auth errors.

**Consequences:** Users appear logged out randomly mid-session; ELO updates silently fail; support reports of "my score didn't save."

**Prevention:**
```kotlin
// In your Supabase client setup:
val supabase = createSupabaseClient(url, anonKey) {
    install(Auth) {
        // Persist session to EncryptedSharedPreferences, not plain SharedPreferences
        sessionManager = SettingsSessionManager(context) // uses DataStore
        // Auto-refresh enabled by default, but verify:
        autoRefreshToken = true
    }
}

// In your network layer, add an interceptor that catches 401 and triggers re-auth:
// If refresh fails → emit AuthState.SignedOut → navigate to login screen
supabase.auth.sessionStatus.collect { status ->
    when (status) {
        is SessionStatus.Authenticated -> { /* normal */ }
        is SessionStatus.NotAuthenticated -> navigateToLogin()
        is SessionStatus.RefreshFailure -> {
            // Show "Session expired, please log in again" — don't silently fail
            navigateToLogin()
        }
        else -> {}
    }
}
```

**Detection:** If the app makes Supabase calls without collecting `sessionStatus`, auth failures are invisible.

**Phase:** Phase 1 (auth implementation).

---

### Pitfall M-2: Realtime Channel Leaks in Jetpack Compose

**What goes wrong:** Leaderboard and social screens subscribe to Supabase Realtime channels inside composables. If the channel is created in a `LaunchedEffect` but not unsubscribed in the `DisposableEffect` cleanup or `onCleared()` in a ViewModel, each recomposition or navigation creates a new WebSocket subscription. After navigating away and back 5-10 times, the device holds multiple duplicate subscriptions consuming memory and battery.

**Why it happens:** The existing codebase already has `MascotFeedbackManager` with an unscoped CoroutineScope leak (CONCERNS.md). The same pattern applied to Realtime subscriptions is worse because WebSocket connections are OS-level resources.

**Consequences:** Memory growth over time; battery drain; app may hit Supabase's free tier concurrent connection limit (200 on free plan) unexpectedly.

**Prevention:**
```kotlin
// WRONG — creates new subscription on every recomposition:
@Composable
fun LeaderboardScreen(supabase: SupabaseClient) {
    LaunchedEffect(Unit) {
        val channel = supabase.realtime.createChannel("leaderboard")
        channel.postgresChangeFlow<LeaderboardEntry>(schema = "public") { ... }
            .collect { updateUI(it) }
        channel.subscribe()
        // ❌ Never unsubscribed
    }
}

// CORRECT — manage in ViewModel, clean up in onCleared():
class LeaderboardViewModel(private val supabase: SupabaseClient) : ViewModel() {
    private var channel: RealtimeChannel? = null

    init {
        channel = supabase.realtime.createChannel("leaderboard-${System.currentTimeMillis()}")
        viewModelScope.launch {
            channel!!.postgresChangeFlow<LeaderboardEntry>(schema = "public") { ... }
                .collect { _leaderboard.value = it }
            channel!!.subscribe()
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            channel?.unsubscribe()
            supabase.realtime.removeChannel(channel!!)
        }
    }
}
```

**Detection:** Count Supabase channel subscriptions in the Supabase dashboard → Realtime → Connections. If count grows on repeated navigation, channels are leaking.

**Phase:** Phase 2 (leaderboard). Establish the ViewModel pattern before implementing any Realtime feature.

---

### Pitfall M-3: SharedPreferences → Supabase Migration — Double-Write Data Loss

**What goes wrong:** On first launch after Supabase integration, the app reads local SharedPreferences data and uploads it to Supabase. If the migration runs again (app restarted before the "migration complete" flag is written, or the flag lives in SharedPreferences that gets cleared), data is uploaded twice, potentially with stale values overwriting the correct synced data from another device.

**Consequences for VietForces:** ELO reset, streak reset, learned words wiped — the exact user data the migration was meant to preserve is corrupted.

**Prevention:**
```kotlin
// Migration must be idempotent and atomic:
suspend fun migrateLocalDataToSupabase(userId: String, context: Context) {
    val prefs = context.getSharedPreferences("vietforces_prefs", Context.MODE_PRIVATE)
    
    // Check migration flag FIRST — stored server-side, not locally
    val alreadyMigrated = supabase.from("users")
        .select { filter { eq("id", userId) } }
        .decodeSingle<User>().localDataMigrated  // boolean column
    
    if (alreadyMigrated) return
    
    val localElo = prefs.getInt("elo", 1000)
    val localStreak = prefs.getInt("streak", 0)
    
    // Only migrate if local data is > default (has meaningful data)
    if (localElo > 1000 || localStreak > 0) {
        supabase.from("progress").upsert(
            mapOf(
                "user_id" to userId,
                "elo" to localElo,
                "streak" to localStreak,
                "local_data_migrated" to true,
                "migrated_at" to Instant.now().toString()
            )
        )
    } else {
        // No local data worth migrating — just mark done
        supabase.from("progress").update(
            mapOf("local_data_migrated" to true)
        ) { filter { eq("user_id", userId) } }
    }
}
```

**Detection:** If migration logic uses a SharedPreferences flag instead of a server-side flag, double-migration is possible any time SharedPreferences is cleared.

**Phase:** Phase 1 (auth + Supabase integration). Design migration before writing any auth code.

---

### Pitfall M-4: Offline Behavior — ELO and Streaks Diverge from Server

**What goes wrong:** User plays a game while offline. Local ELO updates. User comes online, app syncs — but server has a different ELO (maybe from another device, or a corrected value). The sync uses last-write-wins, overwriting the more recent local value with a stale server value.

**The project already declared** "last-write-wins" for offline sync, which is correct for the academic scope. The pitfall is implementing it incorrectly.

**Consequences:** ELO loss ("my score was 1250 this morning, now it's 1100") is the single most complained-about bug in gamified apps. Streak resets are second.

**Prevention:**
1. **ELO:** Since ELO should be server-authoritative (Pitfall C-4), the "offline ELO" is a local display value only. When online, always show server ELO. Show a `[offline]` badge if displaying locally-calculated pending score.
2. **Streaks:** Local streak increment is fine for immediate feedback, but the authoritative streak value is always server-side. On sync, use `MAX(local, server)` for streak (never let sync decrease streak unless it's a real break).
3. **Daily challenge completion:** Store completion events in a local queue (`Room` table or `DataStore`) when offline. Flush queue to Supabase when network is restored. Use an idempotent `upsert` with `(user_id, challenge_date)` as the unique key.

**Detection:** If `supabase.from("progress").update(...)` is called directly without checking `NetworkManager.isOnline()`, offline updates will queue/fail silently.

**Phase:** Phase 2 (sync/challenges). The project already accepted last-write-wins — just implement it consistently.

---

### Pitfall M-5: Next.js Admin — Route Protection Only in Middleware

**What goes wrong:** Admin routes are protected with Next.js Middleware that checks for an admin JWT cookie before serving the page. But the API routes that the admin dashboard calls (e.g., `/api/admin/ban-user`, `/api/admin/delete-content`) don't independently verify the admin JWT. A user who discovers the API endpoint can call it directly with their regular user token, bypassing the middleware entirely.

**Why it happens:** Next.js middleware is the first thing developers reach for. It works for page routes but is bypassed by direct API calls.

**Consequences for VietForces:** Any authenticated user could call `/api/admin/reset-elo?userId=...` and reset others' ELO scores.

**Prevention:**
```typescript
// middleware.ts — protects page navigation only
export function middleware(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith('/admin')) {
    const token = request.cookies.get('sb-token')
    if (!isAdminToken(token)) return NextResponse.redirect('/login')
  }
}

// /api/admin/reset-elo/route.ts — MUST also verify server-side:
export async function POST(request: Request) {
  // Re-verify on every API route — middleware is not enough
  const supabase = createServerClient(url, serviceRoleKey, { cookies })
  const { data: { user } } = await supabase.auth.getUser()
  
  if (!user || !await isAdminUser(user.id)) {
    return Response.json({ error: 'Forbidden' }, { status: 403 })
  }
  
  // Now safe to perform admin action
  // Use service role client (server-side only, never client-side)
  const adminClient = createServerClient(url, serviceRoleKey, { cookies })
  // ...
}

// Admin check via custom claim or dedicated admin_users table:
async function isAdminUser(userId: string): Promise<boolean> {
  const { data } = await adminSupabase
    .from('admin_users')
    .select('id')
    .eq('id', userId)
    .single()
  return !!data
}
```

**Detection:** If any `/api/admin/*` handler doesn't begin with an auth check, it's exploitable.

**Phase:** Phase 3 (admin dashboard). Establish the pattern on the first admin API route.

---

### Pitfall M-6: Android 13 Notification Permission — Silent Drop

**What goes wrong:** On Android 13+ (API 33+), apps must request `POST_NOTIFICATIONS` at runtime like a dangerous permission. FCM silently delivers the push message to the device but the system does not show the notification. No error is thrown. The existing `NotificationManager` singleton in the codebase presumably creates channels without checking permission state.

**Consequences:** Push notifications for streak reminders and daily challenge alerts don't appear for users who never granted permission. Retention impact is significant since notifications are the primary re-engagement mechanism.

**Prevention:**
```kotlin
// In MainActivity or a dedicated permission handler:
val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (!isGranted) {
        // Show in-app "you'll miss streak alerts" message — don't nag again
        settingsManager.notificationsDeclined = true
    }
}

// WHEN to ask — not at app start. Ask contextually:
fun maybeRequestNotificationPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val state = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        )
        if (state != PackageManager.PERMISSION_GRANTED) {
            // Ask only after user completes first day streak (high motivation moment)
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

**Detection:** If `NotificationManager.scheduleReminder()` doesn't check `POST_NOTIFICATIONS` permission on API ≥ 33, reminders silently fail on new Android versions.

**Phase:** Phase 2 (notifications). Add the permission check before implementing FCM integration.

---

### Pitfall M-7: Supabase Free Tier Inactivity Pause

**What goes wrong:** Supabase free tier projects are **paused after 1 week of inactivity** (no API calls). A demo project that isn't accessed for a week becomes inaccessible until manually "unpaused" in the Supabase dashboard. For a thesis defense demo, this is a single-point-of-failure.

**Free tier limits to plan around:**
| Resource | Free Limit | VietForces Risk Level |
|---|---|---|
| Database size | 500 MB | Low (text data only) |
| File storage | 1 GB | Medium (if storing user avatars) |
| Bandwidth | 5 GB/month | Low (demo scale) |
| MAU | 50,000 | Low |
| Realtime connections | 200 concurrent | Low |
| Edge Function invocations | 500K/month | Low |
| DB connections (pooler) | 15 direct / 200 via pgbouncer | Medium (always use pooler) |
| **Project pause** | **After 7 days no activity** | **HIGH for demo** |

**Prevention:**
1. Set up a GitHub Action or cron job that pings the Supabase health endpoint every 3 days: `curl https://YOUR_PROJECT.supabase.co/rest/v1/ -H "apikey: $ANON_KEY"`.
2. Before thesis defense: upgrade to Pro ($25/month) for 1 month — no pausing on Pro.
3. Always use the **connection pooler URL** (port 6543) for database connections, not the direct connection URL (port 5432). Direct connections exhaust the 15-connection free limit instantly with Android's connection pool.

**Detection:** Log in to Supabase dashboard 2 days before demo. If project shows "Paused" badge, click Resume and wait 2-3 minutes.

**Phase:** Phase 1 (initial setup). Configure the keepalive ping on day one.

---

## 🟢 Minor Pitfalls

Annoyances that cause debugging time or minor regressions.

---

### Pitfall m-1: Google OAuth Deep Link Configuration — Auth Redirect Doesn't Return to App

**What goes wrong:** Supabase Google OAuth redirects to a URL like `io.supabase.vietforces://login-callback`. If the Android Manifest doesn't declare the correct `<intent-filter>` with the exact scheme, the OAuth flow completes in the browser but the app never receives the token. User sees the browser with a blank success page.

**Prevention:**
```xml
<!-- AndroidManifest.xml -->
<activity android:name=".MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- Scheme must match Supabase project URL host: -->
        <data android:scheme="io.supabase.vietforces" android:host="login-callback" />
    </intent-filter>
</activity>
```

Also configure the redirect URL in Supabase dashboard → Auth → URL Configuration → Redirect URLs.

**Detection:** Run OAuth flow once in debug build. If browser doesn't return to app, the deep link is misconfigured.

**Phase:** Phase 1 (auth).

---

### Pitfall m-2: `SimpleDateFormat` Locale Bug — Existing Streak Data Corrupted Post-Migration

**What goes wrong:** The existing `UserProgressManager.kt` uses `SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())`. On non-Gregorian locales, stored date strings like `"1447-04-15"` (Persian calendar) differ from expected `"2026-07-22"` (Gregorian). After migration to Supabase, `DATE`-type columns only accept Gregorian ISO 8601. Any persisted streak/heatmap keys from a non-Gregorian locale device would fail to migrate and silently produce a fresh streak.

**Prevention:** Fix the bug *before* migration: replace all `Locale.getDefault()` with `Locale.ROOT` in `UserProgressManager.kt` (lines 84, 189). Run migration only after this fix is in a released update.

**Detection:** `grep -r "Locale.getDefault()" app/src/main/java/` — any date formatting using this is a bug.

**Phase:** Phase 0 (pre-migration cleanup). Fix before writing any Supabase sync code.

---

### Pitfall m-3: `VocabularyRepository` Hardcoded Data Blocks Supabase CRUD Admin Flow

**What goes wrong:** The admin dashboard includes "Vocabulary CRUD" as a feature. But 1,328 lines of vocabulary data is hardcoded in `VocabularyRepository.kt`. Admin changes in the dashboard write to Supabase but the Android app never reads from Supabase — it always uses the hardcoded data. The admin CRUD feature is effectively a no-op.

**Prevention:** Migrate vocabulary data to a Supabase `vocabulary` table. Android fetches from Supabase on startup (or uses a cached local copy). This is already noted as future work in the file comment. Do this migration in the same phase as admin dashboard to avoid the disconnect.

**Detection:** If the admin dashboard writes vocabulary to Supabase but the Android app still uses `VocabularyRepository.allVocabulary`, the features are disconnected.

**Phase:** Phase 3 (admin dashboard) or Phase 1 if vocabulary Supabase table is part of the core schema.

---

### Pitfall m-4: `com.example` Application ID Blocks Play Store + Firebase

**What goes wrong:** `applicationId = "com.example.vietforces"` is rejected by the Google Play Store with error "Package name must not use a reserved namespace." Firebase and FCM also require a real application ID to configure push notifications. If this is changed post-launch, all existing FCM tokens are invalidated (users stop receiving notifications) and any deep links break.

**Consequences for VietForces:** If the thesis requires a Play Store submission or real FCM push notifications, this must be changed before any Firebase/FCM setup. Changing it later requires re-registration of all Firebase services.

**Prevention:** Change `applicationId` to `vn.edu.hcmus.vietforces` in `build.gradle.kts` before adding Firebase or publishing to Play Store. The project constraint says "keep package name" for the Java package structure — that's separate from `applicationId` and does not need to change.

**Detection:** `grep applicationId app/build.gradle.kts` — should not contain `com.example`.

**Phase:** Phase 1 if FCM is needed; Phase 0 if Play Store publication is a requirement.

---

### Pitfall m-5: Supabase Realtime Leaderboard — Race Condition on Initial Load

**What goes wrong:** The leaderboard screen subscribes to Realtime for live updates, then fetches the initial snapshot. If a leaderboard change occurs between the subscription being set up and the initial fetch completing, that change is missed. The displayed leaderboard is inconsistent until the next Realtime event.

**Prevention:**
```kotlin
// Correct ordering: subscribe FIRST, then fetch initial state
viewModelScope.launch {
    // 1. Set up subscription (captures all changes from this point)
    channel.postgresChangeFlow<LeaderboardEntry>(schema = "public") { 
        table = "leaderboard" 
    }.collect { change -> applyChange(change) }
    channel.subscribe()
    
    // 2. THEN fetch current snapshot
    val initial = supabase.from("leaderboard")
        .select { order("elo", Order.DESCENDING); limit(50) }
        .decodeList<LeaderboardEntry>()
    _leaderboard.value = initial
}
```

**Phase:** Phase 2 (leaderboard).

---

### Pitfall m-6: Android Cloud Backup Exposes Supabase Session Tokens

**What goes wrong:** `android:allowBackup="true"` with no exclusion rules (already flagged in CONCERNS.md) means the Supabase session token stored in SharedPreferences/DataStore is backed up to the user's Google account. On a new device restore, the old session token is restored. If the token was revoked (user changed password, account deleted), the restored token causes silent auth failures.

**Prevention:**
```xml
<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <!-- Exclude auth tokens and session data -->
        <exclude domain="sharedpref" path="supabase_session" />
        <exclude domain="database" path="supabase_session.db" />
        <!-- Include gameplay progress (safe to backup) -->
        <include domain="sharedpref" path="vietforces_prefs" />
    </cloud-backup>
</data-extraction-rules>
```

**Phase:** Phase 1 (auth). Configure backup rules immediately after adding Supabase auth.

---

## Phase-Specific Warnings Summary

| Phase | Topic | Likely Pitfall | Must-Do Mitigation |
|---|---|---|---|
| Phase 0 (pre-work) | `SimpleDateFormat` bug | Streak data corrupted in migration | Fix to `Locale.ROOT` before migration |
| Phase 0 (pre-work) | `applicationId` | FCM/Play Store blocked | Rename to `vn.edu.hcmus.vietforces` |
| Phase 1 (Supabase setup) | API key in APK | OpenAI key leaked | Proxy via Edge Function — **do first** |
| Phase 1 (Supabase setup) | Service role key | Database compromise | Only anon key in Android client |
| Phase 1 (Supabase setup) | RLS misconfiguration | Cross-user data leakage | Enable RLS + policies per table immediately |
| Phase 1 (auth) | Token refresh | Silent 401 failures | Collect `sessionStatus`, handle `RefreshFailure` |
| Phase 1 (auth) | Google OAuth deep link | Auth flow broken | Configure intent-filter + Supabase redirect URL |
| Phase 1 (auth) | Cloud backup | Session token backed up | Exclude session from backup rules |
| Phase 1 (migration) | SharedPreferences migration | Double-write data loss | Server-side migration flag, idempotent upsert |
| Phase 2 (leaderboard) | Client-side ELO | Leaderboard cheating | Server-authoritative PostgreSQL function |
| Phase 2 (leaderboard) | Realtime channel leaks | Memory/connection leak | ViewModel `onCleared()` cleanup |
| Phase 2 (daily challenges) | Timezone | False streak breaks | Store/compare dates in `Asia/Ho_Chi_Minh` |
| Phase 2 (notifications) | Android 13 permission | Notifications silently blocked | Runtime `POST_NOTIFICATIONS` check |
| Phase 2 (offline) | Sync conflict | ELO overwritten on sync | Server-authoritative ELO, local = display only |
| Phase 3 (admin) | Middleware-only auth | API routes unprotected | Re-verify JWT in every API route handler |
| Phase 3 (admin) | Vocabulary hardcoded | CRUD is a no-op | Migrate vocabulary to Supabase table |
| All phases | Free tier pause | Demo down before defense | Keepalive cron job; Pro for defense week |

---

## Sources

- Supabase official documentation: Row Level Security, Auth, Realtime (confidence: HIGH — official)
- supabase-kt library: GitHub `supabase-community/supabase-kt` (confidence: HIGH — official)
- Android developer docs: Notification permissions (Android 13+), backup rules, deep links (confidence: HIGH — official)
- VietForces codebase analysis: `/Users/longtdang/Long/HCMUS/vietforces/.planning/codebase/CONCERNS.md` (confidence: HIGH — direct code analysis)
- Supabase free tier pricing page: pricing.supabase.com (confidence: HIGH — verified against known limits)
- Next.js App Router security patterns (confidence: HIGH — official Next.js docs)
- Gamification retention research: streak loss impact, notification timing (confidence: MEDIUM — community/industry papers)
