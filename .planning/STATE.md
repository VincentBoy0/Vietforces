# STATE — VietForces

**Project:** VietForces
**Milestone:** v1
**Mode:** mvp
**Last updated:** 2026-07-22

---

## Project Reference

**Core Value:** Người dùng học tiếng Việt hiệu quả qua gameplay thú vị — mỗi phiên chơi phải cảm thấy có tiến bộ rõ ràng và muốn quay lại ngày hôm sau.

**What we're building:** Android gamified Vietnamese vocabulary app → full product with Supabase backend, real-time leaderboard, daily challenges, push notifications, social features, Next.js admin dashboard, and landing page.

**Current focus:** Phase 0 — Pre-work Fixes (locale bug + applicationId rename before any Supabase integration)

---

## Current Position

```
Phase:   0 — Pre-work Fixes
Plan:    TBD (not yet planned)
Status:  Not started
```

**Progress bar:**

```
[Phase 0] [Phase 1] [Phase 2] [Phase 3] [Phase 4] [Phase 5] [Phase 6] [Phase 7]
    ○          ○          ○          ○          ○          ○          ○          ○
```

**Phase summary:**

| # | Phase | Requirements | Status |
|---|-------|--------------|--------|
| 0 | Pre-work Fixes | PRE-01, PRE-02 | ○ Not started |
| 1 | Supabase Foundation | FOUND-01–05 | ○ Not started |
| 2 | Auth + Onboarding + Sync + UX | AUTH-01–05, ONBOARD-01–03, SYNC-01–02, UX-01–05 | ○ Not started |
| 3 | Streak + Leaderboard | STREAK-01–04, ELO-01–02, LEAD-01–04 | ○ Not started |
| 4 | Daily Challenge + Notifications | DAILY-01–04, NOTIF-01–05 | ○ Not started |
| 5 | Social / Friends | SOCIAL-01–04 | ○ Not started |
| 6 | Web Admin Dashboard | ADMIN-01–08 | ○ Not started |
| 7 | Landing Page | LAND-01–05 | ○ Not started |

---

## Performance Metrics

*(Populated after phases complete)*

| Phase | Plans | Completed | Rework Cycles | Notes |
|-------|-------|-----------|---------------|-------|
| — | — | — | — | — |

---

## Accumulated Context

### Key Decisions Logged

| Decision | Rationale |
|----------|-----------|
| Phase 0 before Phase 1 | `Locale.getDefault()` in `SimpleDateFormat` corrupts streak dates post-migration; `com.example` applicationId blocks FCM. Fix first, write no Supabase code before these are done. |
| UX-01–05 bundled into Phase 2 | Empty/loading/error/dark-mode states are pre-requisites for Phase 2 screens going live; delivering them separately would create a broken interim state. |
| Phase 6 (Admin) depends only on Phase 1 | Admin dashboard is an independent Next.js project; it only needs the Supabase schema. Can be parallelised with Android Phases 3-5. |
| Phase 7 (Landing) depends only on Phase 1 | Purely presentational. No Android dependency. Can be built after Phase 6 or in parallel. |
| ELO server-authoritative from Phase 3 | Prevents leaderboard cheating. Client sends game metrics only; `SECURITY DEFINER` PostgreSQL function calculates new ELO. |
| OpenAI key proxied in Phase 1 | Live security risk — key currently in APK. Must close before any deployment. |
| Supabase free-tier keepalive in Phase 1 | Project pauses after 7 days inactivity; GitHub Actions cron ping prevents demo-day failure. |
| last-write-wins for ELO offline sync | ELO is server-authoritative; local value is display-only. Streak uses MAX(local, server). |
| Guest mode before registration | Reduces signup friction (Duolingo pattern). User plays first, registers later; progress migrated via `ONBOARD-03`. |

### Pitfalls to Watch

| ID | Risk | Phase | Mitigation |
|----|------|-------|------------|
| C-1 | OpenAI API key in APK | Phase 1 | Edge Function proxy — close before any deployment |
| C-3 | RLS disabled or misconfigured | Phase 1 | Enable + test policies immediately after each CREATE TABLE |
| C-4 | Client-side ELO | Phase 3 | SECURITY DEFINER function; RLS blocks direct ELO writes |
| M-1 | Auth token refresh failure (silent 401 loops) | Phase 2 | Handle `RefreshFailure` in auth state collector → redirect to login |
| M-2 | Realtime channel leaks | Phase 3+ | Always subscribe in `init{}`, unsubscribe in `onCleared()` |
| M-6 | Android 13+ notification permission | Phase 4 | Runtime `POST_NOTIFICATIONS` request; ask after first streak achievement |
| m-2 | `Locale.getDefault()` streak bug | Phase 0 | PRE-01 — fix before writing migration code |
| m-3 | Vocabulary hardcoded | Phase 6 | Admin CRUD migrates vocabulary to Supabase `vocabulary` table |
| m-5 | Realtime race condition (fetch before subscribe) | Phase 3 | Subscribe THEN fetch — never fetch then subscribe |

### Technology Notes

- **Android:** supabase-bom 3.7.0, ktor-client-okhttp 3.5.1, hilt-android 2.60.1, KSP (not kapt)
- **Web:** Next.js 15.3.9, @supabase/ssr 0.12.3 (NOT deprecated auth-helpers-nextjs), Tailwind v4
- **Supabase Realtime:** lifecycle-aware in ViewModel — subscribe in `init{}`, unsubscribe in `onCleared()`
- **Admin API routes:** Middleware alone is insufficient — every `/api/admin/*` handler must re-verify JWT

### TODOs (for next planning session)

- [ ] Confirm `applicationId` target value (e.g. `vn.edu.hcmus.vietforces`) with user before Phase 0
- [ ] Confirm Supabase project region and organisation name before Phase 1
- [ ] Check if Google OAuth client ID needs to be generated before Phase 2 planning
- [ ] Confirm FCM project name / Firebase project setup (needed for NOTIF-01)

---

## Session Continuity

**How to resume:** Run `/gsd-plan-phase 0` to generate the execution plan for Phase 0 (Pre-work Fixes).

**Last agent action:** ROADMAP.md and STATE.md created by roadmapper agent. REQUIREMENTS.md traceability section updated.

**Files on disk:**
- `.planning/PROJECT.md` — project brief and constraints
- `.planning/REQUIREMENTS.md` — all 58 v1 requirements with phase assignments
- `.planning/ROADMAP.md` — 8-phase roadmap with success criteria
- `.planning/STATE.md` — this file
- `.planning/research/SUMMARY.md` — architecture + pitfalls research

---

*State initialized: 2026-07-22*
*Last updated: 2026-07-22 — roadmap created*
