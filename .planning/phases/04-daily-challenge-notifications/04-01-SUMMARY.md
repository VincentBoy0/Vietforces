---
phase: "04-daily-challenge-notifications"
plan: "01"
subsystem: "backend/supabase"
tags: ["sql", "migrations", "edge-functions", "daily-challenge", "elo", "fcm", "push-notifications"]
dependency_graph:
  requires:
    - "003_streak_function.sql (update_streak function)"
    - "001_initial_schema.sql (users, user_progress, leaderboard, daily_challenges, fcm_tokens tables)"
  provides:
    - "daily_completions table with composite PK + RLS"
    - "award_daily_bonus() SECURITY DEFINER SQL function"
    - "generate-daily-challenge Deno Edge Function"
    - "send-streak-reminder Deno Edge Function"
  affects:
    - "public.user_progress (elo_score updated by award_daily_bonus)"
    - "public.leaderboard (elo_score + weekly_elo updated by award_daily_bonus)"
    - "public.streak_history (via update_streak call inside award_daily_bonus)"
tech_stack:
  added:
    - "Deno Edge Functions (TypeScript)"
    - "FCM HTTP v1 API (Firebase Cloud Messaging)"
    - "Web Crypto API (RSASSA-PKCS1-v1_5 RS256 JWT signing)"
    - "esm.sh/@supabase/supabase-js@2 (Edge Function Supabase client)"
  patterns:
    - "SECURITY DEFINER SQL function with REVOKE/GRANT discipline"
    - "Fisher-Yates shuffle for random vocabulary selection"
    - "PEM→DER conversion via atob + Uint8Array for Web Crypto key import"
    - "CRON_SECRET bearer token auth for all cron-invoked Edge Functions"
key_files:
  created:
    - "supabase/migrations/005_daily_completions.sql"
    - "supabase/migrations/006_daily_bonus_elo.sql"
    - "supabase/functions/generate-daily-challenge/index.ts"
    - "supabase/functions/send-streak-reminder/index.ts"
  modified: []
decisions:
  - "award_daily_bonus() returns INTEGER (ELO earned) and RAISES EXCEPTION on double-completion rather than returning a status JSON — simpler error path for Android caller using Supabase RPC"
  - "Vocabulary pool is a static array (154 IDs mirroring VocabularyRepository.kt) inside generate-daily-challenge — avoids a DB round-trip for random word selection, consistent with offline-capable design"
  - "send-streak-reminder uses Supabase join query (users + fcm_tokens + user_progress) rather than an RPC — service_role bypasses RLS, keeping the query in the Edge Function layer"
  - "Firebase access token obtained via self-signed JWT + token exchange (Web Crypto API) rather than an external library — zero npm/Deno imports beyond @supabase/supabase-js"
metrics:
  duration: "~10 minutes"
  completed_date: "2026-07-23"
  tasks_completed: 2
  tasks_total: 2
  files_created: 4
  files_modified: 0
---

# Phase 04 Plan 01: SQL Migrations + Edge Functions for Daily Challenge — Summary

**One-liner:** SQL foundation for daily challenge habit loop — `daily_completions` table + `award_daily_bonus()` SECURITY DEFINER function + two pg_cron-scheduled Deno Edge Functions (challenge generation at 00:00 UTC and FCM streak reminder at 19:00 UTC).

---

## What Was Built

### Task 1: SQL Migrations

**`supabase/migrations/005_daily_completions.sql`**
- Creates `public.daily_completions` with composite PK `(user_id, challenge_date)` — enforces one completion per user per day at the DB level.
- Columns: `user_id` (FK → users), `challenge_date` (DATE), `elo_earned` (INTEGER DEFAULT 50), `completed_at` (TIMESTAMPTZ DEFAULT NOW()).
- RLS enabled; `completions_select_own` policy for history display; `completions_service_insert` for service_role tooling.

**`supabase/migrations/006_daily_bonus_elo.sql`**
- `award_daily_bonus(p_user_id UUID, p_challenge_date DATE) RETURNS INTEGER`
- SECURITY DEFINER, SET search_path = public — prevents search_path injection (T-04-03).
- Double-completion guard: `RAISE EXCEPTION 'already_completed'` if row already exists in `daily_completions`.
- Awards +50 ELO: `UPDATE user_progress SET elo_score = elo_score + 50` and `UPDATE leaderboard SET elo_score += 50, weekly_elo += 50`.
- Records completion via `INSERT INTO daily_completions`.
- Calls `update_streak(p_user_id, p_challenge_date)` to credit streak.
- `REVOKE ALL … FROM PUBLIC; GRANT EXECUTE … TO authenticated` (T-04-03 mitigation).

### Task 2: Edge Functions

**`supabase/functions/generate-daily-challenge/index.ts`**
- CRON_SECRET bearer token auth on every request (T-04-01 mitigation).
- Idempotent: SELECT-before-INSERT returns `already_exists: true` if today's challenge exists (T-04-06 mitigation).
- Static `VOCABULARY_POOL` array of 154 IDs from `VocabularyRepository.kt` (all categories: animal/body/clothing/food/household/kitchen/place/school/sentence/vehicle).
- Fisher-Yates shuffle → picks first 10 IDs.
- Random `game_mode` from: `['image_to_word', 'word_to_image', 'fill_blank', 'sentence_order', 'syllable_match']`.
- Inserts `{ challenge_date, game_mode, vocabulary_ids, bonus_elo: 50 }` into `daily_challenges`.
- pg_cron schedule documented as JSDoc comment.

**`supabase/functions/send-streak-reminder/index.ts`**
- CRON_SECRET bearer token auth (T-04-02 mitigation).
- Queries users with FCM tokens who haven't practiced today via Supabase join (`users + fcm_tokens!inner + user_progress`), filtered for `is_banned = false`.
- Firebase access token obtained via self-signed RS256 JWT using Web Crypto API (`crypto.subtle.importKey pkcs8 + crypto.subtle.sign RSASSA-PKCS1-v1_5`), exchanged at `https://oauth2.googleapis.com/token`.
- `pemToDer()` helper converts PEM private key to DER for Web Crypto import.
- Sends FCM HTTP v1 notification: title "🔥 Streak sắp mất!", body "Bạn chưa luyện tập hôm nay! Đừng để mất streak nhé.", `data.screen = "daily_challenge"`.
- Returns `{ notified: N, failed: M }`.
- `FIREBASE_SERVICE_ACCOUNT_JSON` stays in Supabase secrets only — never committed (T-04-05 mitigation).
- pg_cron schedule documented as JSDoc comment.

---

## Deviations from Plan

None — plan executed exactly as written.

---

## Required Supabase Secrets (post-deploy setup)

```bash
# For generate-daily-challenge and send-streak-reminder
supabase secrets set CRON_SECRET=$(openssl rand -hex 32)

# For send-streak-reminder FCM
supabase secrets set FIREBASE_PROJECT_ID=your-firebase-project-id
supabase secrets set FIREBASE_SERVICE_ACCOUNT_JSON="$(cat path/to/serviceAccount.json)"
```

## pg_cron Schedule Setup (run in Supabase SQL Editor after deploy)

```sql
-- Generate daily challenge at midnight UTC
SELECT cron.schedule(
  'generate-daily-challenge',
  '0 0 * * *',
  $$ SELECT net.http_post(
       url     := 'https://<project-ref>.supabase.co/functions/v1/generate-daily-challenge',
       headers := '{"Authorization":"Bearer <CRON_SECRET>"}',
       body    := '{}'
     ) $$
);

-- Send streak reminder at 19:00 UTC
SELECT cron.schedule(
  'send-streak-reminder',
  '0 19 * * *',
  $$ SELECT net.http_post(
       url     := 'https://<project-ref>.supabase.co/functions/v1/send-streak-reminder',
       headers := '{"Authorization":"Bearer <CRON_SECRET>"}',
       body    := '{}'
     ) $$
);
```

---

## Known Stubs

None — all files are production-ready server artifacts with no UI data stubs.

---

## Threat Flags

No new threat surface beyond the plan's threat model. All T-04-xx mitigations applied as documented.

---

## Self-Check: PASSED

- `supabase/migrations/005_daily_completions.sql` — FOUND ✓
- `supabase/migrations/006_daily_bonus_elo.sql` — FOUND ✓
- `supabase/functions/generate-daily-challenge/index.ts` — FOUND ✓
- `supabase/functions/send-streak-reminder/index.ts` — FOUND ✓
- Commit `4b5cc5a` — FOUND ✓
