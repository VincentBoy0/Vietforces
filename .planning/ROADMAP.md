# Roadmap — VietForces

**Project:** VietForces — Gamified Vietnamese vocabulary learning app
**Core Value:** Người dùng học tiếng Việt hiệu quả qua gameplay thú vị — mỗi phiên chơi phải cảm thấy có tiến bộ rõ ràng và muốn quay lại ngày hôm sau.
**Created:** 2026-07-22
**Milestone:** v1 — Full product demo for graduation defence
**Granularity:** fine
**Mode:** mvp
**Requirements:** 58 v1 requirements across 8 phases

---

## Phases

- [x] **Phase 0: Pre-work Fixes** — Resolve two blocking bugs before any Supabase code is written ✓
- [ ] **Phase 1: Supabase Foundation** — Database schema, auth client, DI, and OpenAI key security
- [ ] **Phase 2: Auth + Onboarding + Progress Sync + UX Polish** — Complete user accounts, guest-first onboarding, cloud sync, and baseline Android UX quality
- [ ] **Phase 3: Streak + Real-time Leaderboard** — Server-authoritative ELO + Supabase Realtime leaderboard + streak mechanics
- [ ] **Phase 4: Daily Challenge + Push Notifications** — Daily habit loop (server-generated challenge) + FCM re-engagement notifications
- [ ] **Phase 5: Social / Friends** — Follow system, friends leaderboard tab, and activity feed
- [ ] **Phase 6: Web Admin Dashboard** — Next.js 15 admin app for vocabulary CRUD, user management, analytics, and challenge scheduling
- [ ] **Phase 7: Landing Page** — Next.js marketing page deployed to Vercel

---

## Phase Details

### Phase 0: Pre-work Fixes
**Mode:** mvp
**Goal:** Two silent bugs in the existing codebase are neutralised before a single line of Supabase integration is written — preventing date corruption in migrated progress data and unblocking FCM/Play Store setup
**Depends on:** Nothing
**Requirements:** PRE-01, PRE-02
**Success Criteria** (what must be TRUE):
  1. `SimpleDateFormat` and all date/time formatting throughout the codebase use `Locale.ROOT` — verified by grepping for `Locale.getDefault()` returning zero hits in date-related files
  2. `applicationId` in `build.gradle.kts` is no longer `com.example.vietforces`; the new ID matches the intended Play Store / FCM package
  3. App builds, installs, and all existing game modes run without regression after both changes
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓

---

### Phase 1: Supabase Foundation
**Mode:** mvp
**Goal:** The entire backend infrastructure is live and secure — Supabase project running with full schema, RLS on every table, a single injectable `SupabaseClient`, Hilt wired up, OpenAI key removed from the APK, and a keepalive preventing free-tier pause
**Depends on:** Phase 0
**Requirements:** FOUND-01, FOUND-02, FOUND-03, FOUND-04, FOUND-05
**Success Criteria** (what must be TRUE):
  1. Supabase dashboard shows all 6 tables (`users`, `user_progress`, `leaderboard`, `daily_challenges`, `friendships`, `fcm_tokens`) with RLS enabled and at least one SELECT/INSERT policy per table
  2. An AI feature call (roleplay / writing practice) routes through the Supabase Edge Function proxy — the OpenAI key is absent from the built APK (`strings.xml` and `BuildConfig` contain no `OPENAI_API_KEY` value)
  3. `SupabaseClient` can be injected into any Repository via `@Inject` constructor — confirmed by a smoke-test Repository that resolves without a crash in a Hilt component
  4. A keepalive mechanism (GitHub Actions cron or equivalent) is in place and confirmed to prevent the Supabase project from pausing
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓

---

### Phase 2: Auth + Onboarding + Progress Sync + UX Polish
**Mode:** mvp
**Goal:** A new user can discover the app, play a full game session as a guest, register an account, and find their progress safely stored in the cloud — and every screen feels polished with proper loading, error, and empty states
**Depends on:** Phase 1
**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, ONBOARD-01, ONBOARD-02, ONBOARD-03, SYNC-01, SYNC-02, UX-01, UX-02, UX-03, UX-04, UX-05
**Success Criteria** (what must be TRUE):
  1. A first-time user sees the 4-screen onboarding flow (Welcome → Level → Daily Goal → Name/Avatar) and can reach a playable game mode without creating an account
  2. After completing a guest game session, the user can register with email/password **or** Google one-tap sign-in; their pre-registration ELO and streak are preserved after migration
  3. A returning user can log in (email or Google), and their progress (ELO, streak, words seen) is loaded from Supabase within the same session — and persists across app restarts
  4. A logged-in user can log out from Settings, and can reset a forgotten password via an email link
  5. Every screen with network data shows a skeleton/shimmer loader instead of a blank white view; every error state has a visible retry button; every empty state has an illustration and a CTA
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

### Phase 3: Streak + Real-time Leaderboard
**Mode:** mvp
**Goal:** The two most demo-visible retention mechanics are live — a streak system that cannot be cheated via device time manipulation, and a real-time leaderboard where ELO is calculated server-side and updates visibly on screen
**Depends on:** Phase 2
**Requirements:** STREAK-01, STREAK-02, STREAK-03, STREAK-04, ELO-01, ELO-02, LEAD-01, LEAD-02, LEAD-03, LEAD-04
**Success Criteria** (what must be TRUE):
  1. Streak counter increments only when the server (Supabase, user's stored timezone) confirms a practice session occurred today — changing device clock does not affect the count
  2. A 7-day heatmap calendar is visible on the Profile screen; days with practice are highlighted; streak freeze icon appears when the weekly freeze is available
  3. A user who has not practised today and is within 2 hours of midnight sees a "streak in danger" in-app warning
  4. The Leaderboard screen shows top 50 global players with ELO scores that update in real time (via Supabase Realtime); the current user's rank is visible even when outside top 50
  5. Leaderboard has "This Week" and "All-time" tabs that filter correctly; the Realtime subscription is cleaned up when the user navigates away (no channel leak)
  6. A user's displayed ELO tier badge (Bronze / Silver / Gold / Platinum / Diamond) reflects their server-side ELO — the client cannot manipulate the score
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

### Phase 4: Daily Challenge + Push Notifications
**Mode:** mvp
**Goal:** Users have a reason to open the app every day — a fresh server-generated challenge with a visible countdown and bonus reward — and the app re-engages them via targeted FCM push notifications if they forget
**Depends on:** Phase 3
**Requirements:** DAILY-01, DAILY-02, DAILY-03, DAILY-04, NOTIF-01, NOTIF-02, NOTIF-03, NOTIF-04, NOTIF-05
**Success Criteria** (what must be TRUE):
  1. The Daily Challenge screen shows a new challenge each calendar day (UTC), with a live countdown to midnight UTC; the challenge refreshes automatically when the timer expires without requiring an app restart
  2. Completing the daily challenge awards +50 bonus ELO and credits one day of streak — confirmed even if the user has not played any other game mode that day
  3. The challenge history panel shows the last 7 days with accurate completed / missed / upcoming states
  4. A logged-in user who has not practised today receives an FCM push notification ("Streak in danger!") at the configured time; tapping the notification opens the Daily Challenge screen directly
  5. Android 13+ users are shown the `POST_NOTIFICATIONS` runtime permission request at an appropriate moment (not on first launch); users who deny it do not see repeated prompts; notification preferences in Settings correctly enable/disable each notification type
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

### Phase 5: Social / Friends
**Mode:** mvp
**Goal:** Users can connect with friends, see how they compare, and feel social accountability that drives return visits
**Depends on:** Phase 3
**Requirements:** SOCIAL-01, SOCIAL-02, SOCIAL-03, SOCIAL-04
**Success Criteria** (what must be TRUE):
  1. A user can search for another user by username and follow them; the follow is asymmetric (Twitter-style) — following does not require mutual approval
  2. The Leaderboard screen has a "Friends" tab that shows only the users the current user follows, ranked by ELO
  3. Tapping a friend's name opens their public profile (read-only) showing streak, ELO tier, and game stats
  4. An activity feed shows recent friend milestones (daily challenge completion, ELO tier change) — feed updates without requiring a manual refresh
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

### Phase 6: Web Admin Dashboard
**Mode:** mvp
**Goal:** An authenticated admin can manage vocabulary content, users, daily challenges, and view usage analytics entirely through the Next.js web app — with every API route protected by server-side JWT verification
**Depends on:** Phase 1
**Requirements:** ADMIN-01, ADMIN-02, ADMIN-03, ADMIN-04, ADMIN-05, ADMIN-06, ADMIN-07, ADMIN-08
**Success Criteria** (what must be TRUE):
  1. The admin dashboard is live on Vercel at a public URL; accessing any `/admin/*` route without a valid Supabase admin session redirects to the login page
  2. An admin can create, edit, and delete vocabulary items — including uploading an image to Supabase Storage — and changes are immediately reflected in the Android app (vocabulary no longer hardcoded)
  3. An admin can view all registered users filtered by ELO / last active, soft-ban a user (they see a "banned" message on next login), and reset a user's ELO
  4. The analytics dashboard shows DAU for the last 30 days, top game modes by play count, and average session length as charts — data sourced from Supabase
  5. An admin can view the daily challenge schedule, create a manual challenge for a specific date, and override the auto-generated one — confirmed by the Android app loading the manually created challenge on that date
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

### Phase 7: Landing Page
**Mode:** mvp
**Goal:** Any visitor arriving at the project URL sees a polished, mobile-first marketing page that communicates the app's value and directs them to download it
**Depends on:** Phase 1
**Requirements:** LAND-01, LAND-02, LAND-03, LAND-04, LAND-05
**Success Criteria** (what must be TRUE):
  1. The landing page is live on Vercel, loads without errors on mobile viewport (375px) and desktop, and scores ≥ 90 on Lighthouse Performance
  2. The hero section clearly states the app name, tagline ("Học tiếng Việt qua trò chơi"), and displays an app screenshot or mockup above the fold on mobile
  3. A features section presents at least 4 key capabilities (game modes, AI practice, leaderboard, daily challenge) with icons or illustrations
  4. A download CTA (Google Play badge or APK link) is visible on the hero and in a dedicated section; clicking it leads to a valid download destination
  5. A screenshots or demo section shows actual in-app screens or a video embed that plays correctly on both mobile and desktop
**Plans**: 00-01-PLAN.md ✓, 00-02-PLAN.md ✓
**UI hint**: yes

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. Pre-work Fixes | 2/2 | **Complete** ✓ | 2026-07-22 |
| 1. Supabase Foundation | 0/? | Not started | - |
| 2. Auth + Onboarding + Sync + UX | 0/? | Not started | - |
| 3. Streak + Leaderboard | 0/? | Not started | - |
| 4. Daily Challenge + Notifications | 0/? | Not started | - |
| 5. Social / Friends | 0/? | Not started | - |
| 6. Web Admin Dashboard | 0/? | Not started | - |
| 7. Landing Page | 0/? | Not started | - |

---

## Coverage Map

| Requirement | Phase | Status |
|-------------|-------|--------|
| PRE-01 | Phase 0 | ✓ Complete |
| PRE-02 | Phase 0 | ✓ Complete |
| FOUND-01 | Phase 1 | Pending |
| FOUND-02 | Phase 1 | Pending |
| FOUND-03 | Phase 1 | Pending |
| FOUND-04 | Phase 1 | Pending |
| FOUND-05 | Phase 1 | Pending |
| AUTH-01 | Phase 2 | Pending |
| AUTH-02 | Phase 2 | Pending |
| AUTH-03 | Phase 2 | Pending |
| AUTH-04 | Phase 2 | Pending |
| AUTH-05 | Phase 2 | Pending |
| ONBOARD-01 | Phase 2 | Pending |
| ONBOARD-02 | Phase 2 | Pending |
| ONBOARD-03 | Phase 2 | Pending |
| SYNC-01 | Phase 2 | Pending |
| SYNC-02 | Phase 2 | Pending |
| UX-01 | Phase 2 | Pending |
| UX-02 | Phase 2 | Pending |
| UX-03 | Phase 2 | Pending |
| UX-04 | Phase 2 | Pending |
| UX-05 | Phase 2 | Pending |
| STREAK-01 | Phase 3 | Pending |
| STREAK-02 | Phase 3 | Pending |
| STREAK-03 | Phase 3 | Pending |
| STREAK-04 | Phase 3 | Pending |
| ELO-01 | Phase 3 | Pending |
| ELO-02 | Phase 3 | Pending |
| LEAD-01 | Phase 3 | Pending |
| LEAD-02 | Phase 3 | Pending |
| LEAD-03 | Phase 3 | Pending |
| LEAD-04 | Phase 3 | Pending |
| DAILY-01 | Phase 4 | Pending |
| DAILY-02 | Phase 4 | Pending |
| DAILY-03 | Phase 4 | Pending |
| DAILY-04 | Phase 4 | Pending |
| NOTIF-01 | Phase 4 | Pending |
| NOTIF-02 | Phase 4 | Pending |
| NOTIF-03 | Phase 4 | Pending |
| NOTIF-04 | Phase 4 | Pending |
| NOTIF-05 | Phase 4 | Pending |
| SOCIAL-01 | Phase 5 | Pending |
| SOCIAL-02 | Phase 5 | Pending |
| SOCIAL-03 | Phase 5 | Pending |
| SOCIAL-04 | Phase 5 | Pending |
| ADMIN-01 | Phase 6 | Pending |
| ADMIN-02 | Phase 6 | Pending |
| ADMIN-03 | Phase 6 | Pending |
| ADMIN-04 | Phase 6 | Pending |
| ADMIN-05 | Phase 6 | Pending |
| ADMIN-06 | Phase 6 | Pending |
| ADMIN-07 | Phase 6 | Pending |
| ADMIN-08 | Phase 6 | Pending |
| LAND-01 | Phase 7 | Pending |
| LAND-02 | Phase 7 | Pending |
| LAND-03 | Phase 7 | Pending |
| LAND-04 | Phase 7 | Pending |
| LAND-05 | Phase 7 | Pending |

**Total mapped: 58 / 58 ✓**
*(Note: REQUIREMENTS.md lists 56 in its header; actual count of enumerated IDs is 58 — all IDs mapped above.)*

---

*Roadmap created: 2026-07-22*
*Last updated: 2026-07-22 after initialization*
