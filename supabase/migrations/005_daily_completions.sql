-- ============================================================
-- Migration  : 005_daily_completions
-- Date       : 2026-07-23
-- Description: Tracks which users completed each day's challenge.
--              PK is (user_id, challenge_date) to enforce one
--              completion per user per day at the DB level.
-- Idempotent : yes — CREATE TABLE IF NOT EXISTS, CREATE POLICY
-- ============================================================

-- ---------------------------------------------------------------------------
-- TABLE: public.daily_completions
-- One row per (user, calendar_day) recording the ELO earned for the daily
-- challenge.  Written only by the SECURITY DEFINER award_daily_bonus()
-- function defined in migration 006_daily_bonus_elo.sql — no direct client
-- INSERT policy (Threat T-04-03 mitigation).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.daily_completions (
  user_id        UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  challenge_date DATE        NOT NULL,
  elo_earned     INTEGER     NOT NULL DEFAULT 50,
  completed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, challenge_date)
);

ALTER TABLE public.daily_completions ENABLE ROW LEVEL SECURITY;

-- Users may read their own completion history (calendar display, history screen)
CREATE POLICY "completions_select_own"
  ON public.daily_completions FOR SELECT
  USING (user_id = auth.uid());

-- Only service_role may INSERT directly (e.g. admin tooling).
-- The award_daily_bonus() SECURITY DEFINER function bypasses RLS entirely
-- when writing, so this policy is a belt-and-suspenders safeguard.
CREATE POLICY "completions_service_insert"
  ON public.daily_completions FOR INSERT
  TO service_role
  WITH CHECK (TRUE);
