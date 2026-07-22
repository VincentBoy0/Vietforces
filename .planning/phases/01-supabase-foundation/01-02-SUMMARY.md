---
phase: 01-supabase-foundation
plan: "02"
subsystem: database
tags: [supabase, postgresql, rls, schema, migration]
dependency_graph:
  requires: []
  provides: [supabase-schema, rls-policies, leaderboard-realtime]
  affects: [all-phases-using-supabase]
tech_stack:
  added: [supabase-cli, postgresql-15]
  patterns: [row-level-security, service-role-write-guard, idempotent-migration]
key_files:
  created:
    - supabase/migrations/001_initial_schema.sql
    - supabase/README.md
    - supabase/config.toml
  modified: []
decisions:
  - "Used CREATE TABLE IF NOT EXISTS for idempotent migrations — safe to re-apply"
  - "leaderboard and daily_challenges writes restricted to service_role per threat model T-02-02"
  - "Used gen_random_uuid() (PG 13+ built-in) instead of uuid-ossp extension for UUIDs"
  - "fcm_token moved to dedicated fcm_tokens table (not inlined on users) for cleaner upsert semantics"
metrics:
  duration: "~5 minutes"
  completed: "2026-07-22T17:01:00Z"
  tasks_completed: 2
  files_created: 3
---

# Phase 01 Plan 02: Supabase Schema Migration Summary

**One-liner:** PostgreSQL 15 schema with 6 tables, 17 RLS policies, and service_role write guard on leaderboard/daily_challenges via idempotent SQL migration.

## What Was Built

### Task 1: SQL Migration — 6 Tables + RLS + Policies

**File:** `supabase/migrations/001_initial_schema.sql`

Created a complete, idempotent DDL migration:

| Table | Primary Key | RLS Policies |
|---|---|---|
| `public.users` | `id` (FK → `auth.users`) | select_own, update_own, insert_own |
| `public.user_progress` | `uuid` | select_own, insert_own, update_own |
| `public.leaderboard` | `user_id` | select_all, service_insert, service_update |
| `public.daily_challenges` | `uuid` | select_all, service_insert |
| `public.friendships` | `(follower_id, following_id)` | select_own, insert_own, delete_own |
| `public.fcm_tokens` | `user_id` | select_own, insert_own, update_own |

- 6 × `ENABLE ROW LEVEL SECURITY` (one per table)
- 17 × `CREATE POLICY` statements
- 15 × `auth.uid()` usages across user-scoped policies
- Realtime publication: `ALTER PUBLICATION supabase_realtime ADD TABLE public.leaderboard`

### Task 2: supabase/README.md

Documents:
- Prerequisites (Supabase CLI install, project creation, `supabase link`)
- Option A: `supabase db push` (preferred)
- Option B: Dashboard SQL editor (manual fallback)
- Verification steps (Table Editor, Policies, Replication)
- Edge Function secrets setup
- Free tier keepalive guidance
- Important notes on service_role write guard

---

## Verification Results

```
ENABLE ROW LEVEL SECURITY : 6  ✓ (one per table)
CREATE TABLE IF NOT EXISTS : 6  ✓ (+ 1 in comment header)
CREATE POLICY              : 17 ✓ (≥ 12 required)
auth.uid() usages          : 15 ✓ (≥ 8 required)
supabase/README.md         : FOUND ✓
"supabase db push" in README: 1 ✓
```

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Feature] Added `IF NOT EXISTS` guard to policy creation**
- **Found during:** Task 1
- **Issue:** The plan specified `CREATE POLICY` but the plan also requires idempotency; `CREATE POLICY` errors if a policy already exists
- **Fix:** Used `CREATE POLICY IF NOT EXISTS` (PostgreSQL 15+ syntax) to match the idempotency requirement stated in the plan's action block
- **Files modified:** `supabase/migrations/001_initial_schema.sql`

**2. [Plan Deviation - Prompt vs Plan] config.toml included**
- The prompt requested a minimal `supabase/config.toml`; the PLAN.md did not specify this file. Added as it is required for `supabase db push` to work locally.
- **Files modified:** `supabase/config.toml`

---

## Threat Surface Scan

No new security surface beyond what is described in the plan's `<threat_model>`. All write paths to `leaderboard` and `daily_challenges` are correctly guarded with `TO service_role`. User-scoped tables use `auth.uid()` exclusively.

---

## Known Stubs

None. This plan delivers SQL and documentation only — no application code.

---

## Self-Check: PASSED

- `supabase/migrations/001_initial_schema.sql` — FOUND ✓
- `supabase/README.md` — FOUND ✓
- `supabase/config.toml` — FOUND ✓
- Commit `0412c01` — FOUND ✓
