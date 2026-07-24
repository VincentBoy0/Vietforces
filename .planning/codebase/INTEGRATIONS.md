# Integrations

**Analysis Date:** 2026-07-24

## External Services

### Supabase
- **Purpose:** Primary backend — PostgreSQL database, authentication, realtime subscriptions, file storage, and Edge Functions hosting
- **Android:** `io.github.jan-tennert.supabase` SDK 3.7.0; configured in `app/src/main/java/com/example/vietforces/di/SupabaseModule.kt`
  - Modules installed: `Auth`, `Postgrest`, `Realtime`, `Storage`
  - Credentials: `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `local.properties` → injected into `BuildConfig`
- **web-admin:** `@supabase/supabase-js` ^2.49.0 + `@supabase/ssr` 0.12.3
  - Server components use `NEXT_PUBLIC_SUPABASE_URL` + `NEXT_PUBLIC_SUPABASE_ANON_KEY`
  - Admin operations use `SUPABASE_SERVICE_ROLE_KEY` (server-only, never exposed to browser)
- **Edge Functions:** Auto-injected `SUPABASE_URL` + `SUPABASE_SERVICE_ROLE_KEY` for privileged DB access

### OpenAI API
- **Purpose:** AI-powered features — writing feedback, roleplay conversation, mascot reactions, learning path generation, exercise grading
- **Model:** `gpt-4.1-mini` (default); overridable via `OPENAI_MODEL` in `local.properties`
- **Endpoint:** `https://api.openai.com/v1/chat/completions`
- **Integration path:** Android `AiManager` (`app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`) → `OpenAiClient` → Supabase Edge Function `openai-proxy` → OpenAI API
- **Key security:** `OPENAI_API_KEY` is stored only as a Supabase secret; the Android app never holds or sends the key directly
- **Proxy function:** `supabase/functions/openai-proxy/index.ts` — forwards request body verbatim, attaches server-side key

### Firebase Cloud Messaging (FCM)
- **Purpose:** Push notifications to Android devices (streak reminders with deep-link to daily challenge screen)
- **Android SDK:** `com.google.firebase:firebase-messaging-ktx` 24.1.1
- **Service class:** `app/src/main/java/com/example/vietforces/VietForcesFirebaseMessagingService.kt`
- **Token storage:** FCM token stored in `public.fcm_tokens` table in Supabase; managed by `app/src/main/java/com/example/vietforces/data/manager/FCMTokenManager.kt`
- **Server-side sending:** `supabase/functions/send-streak-reminder/index.ts` authenticates with Firebase using a service account JWT (RSA-256 signed, built using Web Crypto API), then calls FCM HTTP v1 API at `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`
- **Required secrets:** `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` (Supabase secrets)
- **Deep linking:** FCM `data.screen = "daily_challenge"` navigates to daily challenge on tap

### Google OAuth
- **Purpose:** Social sign-in for Android users
- **Integration:** Supabase Auth Google provider via `supabase-auth-kt` — called as `signInWithGoogle()` in `app/src/main/java/com/example/vietforces/data/repository/AuthRepository.kt`
- **No separate Google SDK** — handled entirely through Supabase Auth's OAuth flow

### Google OAuth2 Token Exchange (FCM auth only)
- **Purpose:** Exchange Firebase service account JWT for a short-lived access token
- **Endpoint:** `https://oauth2.googleapis.com/token`
- **Used by:** `supabase/functions/send-streak-reminder/index.ts` only — not a user-facing integration

---

## Internal Service Boundaries

```
┌─────────────────────────────────────────────────────────────────────┐
│  Android App (com.vietforces.app)                                    │
│  - Supabase Kotlin SDK (Auth, Postgrest, Realtime, Storage)         │
│  - Calls openai-proxy Edge Function via HTTPS for AI features       │
│  - Receives FCM push from Firebase servers                          │
└────────────────┬───────────────────────────────────────────────────┘
                 │ HTTPS (Supabase REST + WebSocket Realtime)
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Supabase Platform                                                   │
│  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │  PostgreSQL DB  │  │  Supabase Auth   │  │  Edge Functions  │   │
│  │  (RLS enabled) │  │  (email + Google)│  │  (Deno runtime)  │   │
│  └────────────────┘  └──────────────────┘  └────────┬─────────┘   │
│  ┌────────────────┐  ┌──────────────────┐           │              │
│  │  Realtime      │  │  Storage         │           │              │
│  │  (leaderboard) │  │  (avatars/media) │           │              │
│  └────────────────┘  └──────────────────┘           │              │
└────────────────────────────────────────┬────────────┘──────────────┘
                                         │ HTTPS (openai-proxy → OpenAI)
                 ┌───────────────────────┤ HTTPS (send-streak-reminder → FCM)
                 │                       │ HTTPS (generate-daily-challenge, refresh-streak-freeze)
                 ▼                       ▼
┌────────────────────────┐  ┌────────────────────────────────────────┐
│  web-admin (Next.js)   │  │  External APIs                         │
│  - @supabase/ssr       │  │  - OpenAI API (gpt-4.1-mini)           │
│  - Service role client │  │  - Firebase FCM HTTP v1                │
│  - Recharts analytics  │  │  - Google OAuth2 token endpoint        │
└────────────────────────┘  └────────────────────────────────────────┘
┌────────────────────────┐
│  web-landing (Next.js) │
│  - Static/presentational│
│  - No Supabase client  │
└────────────────────────┘
```

**Communication protocols:**
- Android ↔ Supabase: HTTPS REST (PostgREST) + WebSocket (Realtime)
- web-admin ↔ Supabase: HTTPS REST via Next.js Server Actions and Server Components
- Edge Functions → OpenAI: HTTPS `POST /v1/chat/completions`
- Edge Functions → FCM: HTTPS `POST /v1/projects/{id}/messages:send`
- pg_cron → Edge Functions: `net.http_post()` with `Authorization: Bearer {CRON_SECRET}`

---

## Authentication Flow

### Android App
1. User enters email/password (or taps Google Sign-In) on `LoginScreen`
2. `AuthRepository.signIn()` / `signInWithGoogle()` calls Supabase Auth via `supabase-auth-kt`
3. Supabase Auth returns a JWT session stored in-memory by the SDK
4. `AuthViewModel` observes `authRepository.authState` (backed by `supabase.auth.sessionStatus` Flow)
5. All subsequent Supabase requests automatically include the JWT via SDK interceptors
6. On `signOut()`, session is cleared and the anon key reverts to unauthenticated mode

### web-admin
1. Admin navigates to `/login`, submits credentials via server action in `web-admin/src/app/login/actions.ts`
2. `createClient()` (server) from `web-admin/src/lib/supabase/server.ts` uses `@supabase/ssr` to authenticate
3. Session stored in HTTP-only cookies; `@supabase/ssr` handles cookie get/set in server components
4. Privileged operations (ban user, etc.) use `createAdminClient()` from `web-admin/src/lib/supabase/admin.ts` with `SUPABASE_SERVICE_ROLE_KEY` — server-only, never sent to browser
5. Unauthenticated or unauthorized users are redirected to `/unauthorized`

### Edge Functions
- Cron-triggered functions (`generate-daily-challenge`, `send-streak-reminder`, `refresh-streak-freeze`) verify `Authorization: Bearer {CRON_SECRET}` header; return 401 if missing/wrong
- `openai-proxy` verifies that the caller holds a valid Supabase anon key (standard Supabase function auth)

---

## Data Sync & Realtime

- **Leaderboard realtime:** Android app subscribes to the `leaderboard` table via `supabase-realtime-kt`; changes are pushed over WebSocket when `LeaderboardRepository` is active
- **User progress:** Synced to Supabase on game completion via `ProgressRepository` / `UserProgressManager`; no offline-first caching — network required for sync
- **Daily challenge:** Read from `daily_challenges` table; generated server-side daily by the `generate-daily-challenge` Edge Function at 00:00 UTC
- **Activity feed:** Social events written to the `activity_feed` table (via `007_activity_feed.sql` schema); read by `ActivityFeedViewModel` / `SocialRepository`
- **Local preferences:** Non-critical settings (AI toggles, encountered items) stored locally via `PreferencesManager` (`app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt`)

---

## Push Notifications

### Flow
1. Android app registers with Firebase on first launch / token refresh via `VietForcesFirebaseMessagingService.onNewToken()`
2. `FCMTokenManager.registerToken()` stores the token in `public.fcm_tokens` table in Supabase via the Supabase client (authenticated with user's JWT)
3. At 19:00 UTC daily, pg_cron triggers the `send-streak-reminder` Edge Function
4. The function queries all users in `fcm_tokens` who: (a) have `is_banned = false`, (b) have `notif_streak_enabled = true`, (c) have not practiced today (`last_practice_date < today` or NULL)
5. The function obtains a Firebase OAuth2 access token by signing a JWT with the `FIREBASE_SERVICE_ACCOUNT_JSON` service account key (RSA-256 via Web Crypto API)
6. For each eligible user, the function POSTs to `https://fcm.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/messages:send`
7. Notification payload: title "🔥 Streak sắp mất!", body reminder text, `data.screen = "daily_challenge"`
8. On the device, `VietForcesFirebaseMessagingService.onMessageReceived()` builds a system notification with a `PendingIntent` that deep-links to `MainActivity` with `navigate_to = "daily_challenge"`

### User Opt-Out
- Column `notif_streak_enabled` (added by `supabase/migrations/010_notif_preferences.sql`) allows per-user opt-out; default is `true`
- Users can toggle this in the Settings screen

### Required Secrets (Supabase)
- `CRON_SECRET` — shared secret for pg_cron → Edge Function auth
- `FIREBASE_PROJECT_ID` — Firebase project identifier
- `FIREBASE_SERVICE_ACCOUNT_JSON` — Full service account JSON with `firebase.messaging` scope

---

*Integration audit: 2026-07-24*
