---
phase: 05-social-friends
plan: "01"
subsystem: database-migrations
tags:
  - sql
  - supabase
  - rls
  - activity-feed
  - social

dependency_graph:
  requires:
    - 001_initial_schema.sql (users, friendships, user_progress tables + base RLS)
    - 005_daily_completions.sql (daily_completions table)
  provides:
    - public.activity_events table with RLS
    - on_daily_completion_insert() trigger function
    - after_daily_completion_insert trigger on daily_completions
    - users_select_public_username policy (SOCIAL-01 enabler)
    - progress_select_public policy (SOCIAL-03 enabler)
  affects:
    - supabase/migrations/007_activity_feed.sql

tech_stack:
  added: []
  patterns:
    - SECURITY DEFINER trigger function for cross-table writes bypassing RLS
    - OR-based SELECT policy composition (multiple permissive policies)
    - EXISTS subquery RLS for follower-scoped feed visibility

key_files:
  created:
    - supabase/migrations/007_activity_feed.sql
  modified: []

decisions:
  - "Used UUID PK on activity_events (gen_random_uuid()) for consistency with existing schema rather than bigserial"
  - "Trigger function named on_daily_completion_insert() per PLAN.md canonical key_links pattern (not notify_daily_completion from prompt)"
  - "activity_events INSERT restricted to service_role + SECURITY DEFINER trigger; no client INSERT policy (T-05-01-01 mitigation)"
  - "SET search_path = public on trigger function prevents search_path-hijack attacks (T-05-01-02)"
  - "Skipped re-adding friendship policies: friendships_select_own, friendships_insert_own, friendships_delete_own already exist in 001_initial_schema.sql"

metrics:
  duration_seconds: ~120
  completed_date: "2026-07-23"
  tasks_completed: 1
  tasks_total: 1
  files_created: 1
  files_modified: 0
---

# Phase 05 Plan 01: Activity Feed SQL Migration Summary

**One-liner:** PostgreSQL migration adding `activity_events` table with follower-scoped RLS, a SECURITY DEFINER trigger auto-populating the feed from `daily_completions`, and public SELECT policies on `users` and `user_progress` for social features.

## What Was Built

`supabase/migrations/007_activity_feed.sql` — idempotent migration applying:

| Object | Type | Purpose |
|--------|------|---------|
| `public.activity_events` | Table | Stores user activity for social feed |
| `idx_activity_events_user_id` | Index | Per-user feed query performance |
| `idx_activity_events_created_at` | Index | Chronological feed sorting (DESC) |
| `activity_events_select_following` | RLS Policy | Owner + follower read access via EXISTS subquery on friendships |
| `activity_events_service_insert` | RLS Policy | service_role-only direct INSERT (belt-and-suspenders) |
| `on_daily_completion_insert()` | Trigger Function | SECURITY DEFINER; writes one activity_events row per daily_completions INSERT |
| `after_daily_completion_insert` | Trigger | AFTER INSERT on daily_completions, FOR EACH ROW |
| `users_select_public_username` | RLS Policy | USING(TRUE) — enables ILIKE username search (SOCIAL-01) |
| `progress_select_public` | RLS Policy | USING(TRUE) — enables public profile stats (SOCIAL-03) |

## Schema

```sql
CREATE TABLE IF NOT EXISTS public.activity_events (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  event_type TEXT        NOT NULL,       -- 'daily_completion' | 'elo_milestone'
  metadata   JSONB       NOT NULL DEFAULT '{}'::JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## Security Notes (Threat Mitigations)

- **T-05-01-01 (Spoofing):** No client INSERT policy on activity_events; only the SECURITY DEFINER trigger and service_role can write.
- **T-05-01-02 (Tampering):** `SET search_path = public` on trigger function blocks search_path-hijack attacks.
- **T-05-01-03 (Info Disclosure — users):** `USING(TRUE)` SELECT is intentional; Android client only queries `id, username, avatar_url`.
- **T-05-01-04 (Info Disclosure — user_progress):** Gamification stats (elo_score, streak_count, total_games) — low sensitivity for academic project.
- **T-05-01-05 (EoP — friendships subquery):** EXISTS references `auth.uid()` server-side; cannot be spoofed.

## Commits

| Hash | Message |
|------|---------|
| 5ee83aa | feat(sql): add activity_events table, daily_completion trigger, social RLS policies (SOCIAL-01, SOCIAL-04) |

## Deviations from Plan

### Auto-decisions (no plan deviation, clarifications only)

**1. Trigger function name**
- Prompt suggested `notify_daily_completion()`; PLAN.md `key_links` uses `on_daily_completion_insert()`.
- Used PLAN.md name for consistency with documented canonical pattern.

**2. Friendship RLS policies not re-added**
- Prompt's section (e) listed `view_own_friendships`, `insert_own_follow`, `delete_own_follow`.
- These already exist in `001_initial_schema.sql` as `friendships_select_own`, `friendships_insert_own`, `friendships_delete_own` with identical USING clauses.
- Adding duplicate policies with different names would be harmless (Postgres ORs permissive policies) but confusing. PLAN.md does not list them. Skipped.

No bugs, no architectural changes, no blockers.

## Verification Checklist

After applying via `supabase db push` or Dashboard SQL editor:

```sql
-- 1. Table exists
SELECT * FROM public.activity_events LIMIT 1;

-- 2. Trigger bound
SELECT trigger_name FROM information_schema.triggers
WHERE event_object_table = 'daily_completions';
-- Expected: after_daily_completion_insert

-- 3. Public username search works
SELECT id, username FROM public.users LIMIT 5;
-- Expected: rows (not just own row)

-- 4. Policies in place
SELECT policyname, tablename FROM pg_policies
WHERE policyname IN (
  'activity_events_select_following',
  'activity_events_service_insert',
  'users_select_public_username',
  'progress_select_public'
);
-- Expected: 4 rows
```

## Known Stubs

None — this is a pure SQL migration with no UI stubs.

## Threat Flags

None — all surfaces are within the scope of the plan's threat model.

## Self-Check: PASSED

- [x] `supabase/migrations/007_activity_feed.sql` exists and contains all required objects
- [x] Commit 5ee83aa exists in git log
- [x] 4 policies present (activity_events ×2, users ×1, user_progress ×1)
- [x] Trigger function SECURITY DEFINER with SET search_path = public
- [x] AFTER INSERT trigger bound to daily_completions
- [x] No accidental file deletions in commit
