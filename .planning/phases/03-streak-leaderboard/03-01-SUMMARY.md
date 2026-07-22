---
phase: "03-streak-leaderboard"
plan: "01"
subsystem: "backend/supabase"
tags: ["sql", "elo", "streak", "leaderboard", "security-definer", "pg-cron", "edge-function"]
dependency_graph:
  requires: ["001_initial_schema.sql"]
  provides:
    - "calculate_elo() SECURITY DEFINER RPC"
    - "update_streak() SECURITY DEFINER RPC"
    - "streak_history table with RLS"
    - "reset_weekly_elo() + grant_streak_freeze() service_role functions"
    - "refresh-streak-freeze Deno Edge Function"
  affects:
    - "user_progress.elo_score"
    - "user_progress.streak_count"
    - "user_progress.streak_freeze_count"
    - "user_progress.last_practice_date"
    - "leaderboard.elo_score"
    - "leaderboard.weekly_elo"
    - "leaderboard.rank_tier"
tech_stack:
  added:
    - "plpgsql SECURITY DEFINER functions"
    - "pg_cron scheduled job"
    - "Deno Edge Function (Supabase)"
  patterns:
    - "SECURITY DEFINER for client-untrusted mutations"
    - "Composite PK on streak_history (user_id, practiced_date) — idempotent upserts"
    - "Dynamic K-factor ELO (40/32/24 by tier)"
    - "Freeze-first gap logic in update_streak"
key_files:
  created:
    - "supabase/migrations/002_elo_function.sql"
    - "supabase/migrations/003_streak_function.sql"
    - "supabase/migrations/004_leaderboard_week.sql"
    - "supabase/functions/refresh-streak-freeze/index.ts"
  modified: []
decisions:
  - "Dynamic K-factor (40 for <1200, 32 for <2100, 24 for 2100+) matches competitive ELO conventions"
  - "update_streak() returns early on same-day call without mutating state (idempotent per-day)"
  - "No direct INSERT RLS on streak_history — only SECURITY DEFINER function may write (Threat T-03-03)"
  - "grant_streak_freeze() exposed as RPC (not inline SQL) so Edge Function can call it cleanly"
  - "weekly_elo already existed in 001 schema — migration 004 adds only the cron schedule + functions"
metrics:
  duration: "2 minutes"
  completed_date: "2026-07-23"
  tasks_completed: 3
  tasks_total: 3
  files_created: 4
  files_modified: 0
---

# Phase 03 Plan 01: SQL Migrations — ELO, Streak, and Weekly Leaderboard Reset Summary

**One-liner:** Server-side SECURITY DEFINER functions for ELO computation (`calculate_elo`), streak tracking with freeze logic (`update_streak`), `streak_history` table with RLS, pg_cron weekly reset, and a Deno Edge Function to grant weekly streak freezes.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | 002_elo_function.sql — calculate_elo() | 998b3e2 | supabase/migrations/002_elo_function.sql |
| 2 | 003_streak_function.sql — streak_history + update_streak() | 998b3e2 | supabase/migrations/003_streak_function.sql |
| 3 | 004_leaderboard_week.sql + refresh-streak-freeze Edge Function | 998b3e2 | supabase/migrations/004_leaderboard_week.sql, supabase/functions/refresh-streak-freeze/index.ts |

---

## What Was Built

### `002_elo_function.sql`
- **`public.get_rank_tier(INT) → TEXT`** — private helper mapping ELO to Codeforces-style tier name (matches `EloRank.kt` exactly: Newbie → Legendary Grandmaster)
- **`public.calculate_elo(UUID, INT, INT, BIGINT) → JSON`** — SECURITY DEFINER; reads current ELO from `user_progress`, computes delta using dynamic K-factor (40/32/24), applies time bonus (+0.1 if avg answer < 5 s), clamps to [0, 3000], updates `user_progress.elo_score`, upserts `leaderboard` (weekly_elo tracks weekly delta), returns `{ new_elo, rank_tier, elo_delta }`
- GRANT EXECUTE to `authenticated`

### `003_streak_function.sql`
- **`public.streak_history`** — composite PK `(user_id, practiced_date)`, FK → users.id ON DELETE CASCADE, RLS enabled with SELECT-own policy; no direct INSERT policy (only SECURITY DEFINER may write)
- **`public.update_streak(UUID, DATE) → JSON`** — SECURITY DEFINER; handles four cases:
  - `gap = 0` → no-op, returns current state
  - `gap = 1` → `streak_count + 1`
  - `gap > 1 AND freeze > 0` → consume freeze, streak unchanged
  - `gap > 1 AND freeze = 0` → reset streak to 1
  - Always upserts `streak_history` (ON CONFLICT DO NOTHING)
  - Returns `{ streak_count, streak_freeze_available, was_freeze_used }`
- GRANT EXECUTE to `authenticated`

### `004_leaderboard_week.sql`
- **`public.reset_weekly_elo()`** — sets `weekly_elo = 0` for all leaderboard rows; SECURITY DEFINER; GRANT to `service_role`
- **`public.grant_streak_freeze()`** — sets `streak_freeze_count = 1` for all users with `< 1`; SECURITY DEFINER; GRANT to `service_role`
- pg_cron schedule `reset-weekly-elo` at `0 0 * * 1` (Monday 00:00 UTC) inside a DO block that skips gracefully if pg_cron extension is not yet enabled

### `supabase/functions/refresh-streak-freeze/index.ts`
- Deno Edge Function; reads `SUPABASE_URL` + `SUPABASE_SERVICE_ROLE_KEY` from env
- Calls `supabase.rpc('grant_streak_freeze')` via service_role client
- Returns `{ refreshed: true }` on success, 500 + error message on failure
- Scheduled Monday 01:00 UTC (after leaderboard reset at 00:00)

---

## Threat Mitigations Applied

| Threat | Mitigation |
|--------|-----------|
| T-03-01 Tampering (ELO) | SECURITY DEFINER — client sends only game metrics; server reads and writes elo_score |
| T-03-02 Tampering (streak date) | Gap computed server-side; single-day increments only per call |
| T-03-03 EoP (streak_history) | No direct INSERT RLS; only SECURITY DEFINER function writes to streak_history |
| T-03-04 Spoofing (Edge Function) | service_role key from Supabase env vars — never exposed to client |

---

## Decisions Made

1. **Dynamic K-factor** (40/32/24 by ELO band) — higher volatility for new players improves early onboarding experience; same bands used by Codeforces.
2. **Same-day idempotency** — `update_streak` returns early without mutation when `gap = 0`; safe to call multiple times per day.
3. **Freeze-before-reset priority** — when gap > 1 and freeze available, freeze is consumed automatically; client does not choose.
4. **streak_history INSERT security** — no RLS INSERT policy; only the SECURITY DEFINER function can record practice days, preventing clients from forging dates.
5. **grant_streak_freeze as RPC** — exposed as a named function so the Edge Function calls one RPC rather than raw SQL via service_role.
6. **weekly_elo not re-added** — column already existed in `001_initial_schema.sql`; migration 004 adds only the reset infrastructure.

---

## Deviations from Plan

None — plan executed exactly as written. Schema column names matched the actual `001_initial_schema.sql` (`elo_score`, `streak_count`, `last_practice_date`) rather than the slightly different names in the CRITICAL CONTEXT block of the prompt.

---

## Requirements Addressed

| Requirement | Status |
|-------------|--------|
| ELO-01 | ✅ `calculate_elo()` RPC; client sends only game metrics |
| STREAK-01 | ✅ `update_streak()` handles UTC date gap logic server-side |
| STREAK-03 | ✅ Freeze auto-applied by `update_streak()` when streak would break |

---

## Self-Check: PASSED

- `supabase/migrations/002_elo_function.sql` — FOUND ✓
- `supabase/migrations/003_streak_function.sql` — FOUND ✓
- `supabase/migrations/004_leaderboard_week.sql` — FOUND ✓
- `supabase/functions/refresh-streak-freeze/index.ts` — FOUND ✓
- Commit `998b3e2` — FOUND ✓
