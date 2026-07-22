-- ============================================================
-- Migration  : 004_leaderboard_week
-- Date       : 2026-07-23
-- Description: Schedule weekly_elo reset via pg_cron
--              (requires pg_cron extension enabled in
--               Supabase Dashboard → Database → Extensions)
-- Idempotent : yes — cron.schedule upserts by job name
-- ============================================================

-- ---------------------------------------------------------------------------
-- NOTE: weekly_elo column already exists on public.leaderboard (001_initial_schema.sql).
-- This migration adds the pg_cron job to reset it every Monday at 00:00 UTC.
-- If pg_cron is not yet enabled, the DO block silently skips scheduling —
-- enable the extension and re-run the migration (or use Supabase Dashboard →
-- Database → Extensions → pg_cron → Enable).
--
-- Call order (both scheduled in UTC, Monday):
--   00:00 — reset_weekly_elo  (this pg_cron job)
--   01:00 — refresh-streak-freeze Edge Function (grants 1 freeze to eligible users)
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- FUNCTION: reset_weekly_elo()
-- Resets weekly_elo to 0 for all leaderboard rows.
-- Called by pg_cron every Monday 00:00 UTC.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.reset_weekly_elo()
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  UPDATE public.leaderboard
     SET weekly_elo = 0,
         updated_at = NOW();
$$;

-- ---------------------------------------------------------------------------
-- FUNCTION: grant_streak_freeze()
-- Grants 1 streak freeze to all users whose current count is < 1.
-- Called weekly (via refresh-streak-freeze Edge Function, Monday 01:00 UTC).
-- Exposed as an RPC so the Edge Function can invoke it with service_role key.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.grant_streak_freeze()
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  UPDATE public.user_progress
     SET streak_freeze_count = 1,
         updated_at          = NOW()
   WHERE streak_freeze_count < 1;
$$;

-- Only service_role needs to call these — do NOT grant to authenticated
GRANT EXECUTE ON FUNCTION public.reset_weekly_elo()    TO service_role;
GRANT EXECUTE ON FUNCTION public.grant_streak_freeze() TO service_role;

-- ---------------------------------------------------------------------------
-- Schedule weekly_elo reset via pg_cron (Monday 00:00 UTC)
-- Requires: pg_cron extension enabled in Supabase Dashboard.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  -- Requires pg_cron extension enabled in Supabase Dashboard → Database → Extensions
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.schedule(
      'reset-weekly-elo',          -- job name (upserted on re-run)
      '0 0 * * 1',                 -- every Monday at 00:00 UTC
      $cron$ SELECT public.reset_weekly_elo(); $cron$
    );
  END IF;
END $$;
