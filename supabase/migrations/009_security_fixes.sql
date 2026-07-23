-- ============================================================
-- Migration  : 009_security_fixes
-- Date       : 2026-07-23
-- Description: Security hardening:
--              1. Narrow public views for users and user_progress (WA-04)
--                 The existing USING(TRUE) RLS policies on both tables remain
--                 for backward compatibility; these views provide a minimal
--                 surface for new code to reference instead.
--              2. Patch deployed functions to enforce auth.uid() caller
--                 identity (CR-02 — adds the guard to already-deployed
--                 functions without requiring a full re-deploy).
-- Idempotent : yes — CREATE OR REPLACE VIEW / FUNCTION throughout
-- ============================================================

-- ---------------------------------------------------------------------------
-- WA-04: Narrow views — expose only the columns required for public profiles
-- and leaderboard display.  New application code should SELECT from these
-- views instead of the base tables.
-- ---------------------------------------------------------------------------

-- Public-facing user lookup: only id + username needed for display
CREATE OR REPLACE VIEW public.users_public AS
  SELECT id, username FROM public.users;

GRANT SELECT ON public.users_public TO authenticated;

-- Public-facing progress stats: only the columns shown on public profiles
CREATE OR REPLACE VIEW public.user_progress_public AS
  SELECT user_id, elo_score, streak_count, total_games FROM public.user_progress;

GRANT SELECT ON public.user_progress_public TO authenticated;

-- ---------------------------------------------------------------------------
-- CR-02 patch: re-deploy calculate_elo with the auth.uid() guard so the
-- fix is effective on already-running Supabase projects (the function body
-- in 002_elo_function.sql is only applied on a fresh deploy).
-- The full function body is reproduced here; the only change vs 002 is the
-- five-line IF block immediately after BEGIN.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.calculate_elo(
  p_user_id  UUID,
  p_correct  INT,
  p_total    INT,
  p_time_ms  BIGINT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_current_elo  INT;
  v_k_factor     INT;
  v_performance  FLOAT;
  v_time_factor  FLOAT;
  v_delta        INT;
  v_new_elo      INT;
  v_rank_tier    TEXT;
BEGIN
  -- Security: reject calls where p_user_id ≠ the authenticated caller (CR-02)
  IF p_user_id <> auth.uid() THEN
    RAISE EXCEPTION 'unauthorized'
      USING HINT = 'p_user_id must equal the calling user''s auth.uid()';
  END IF;

  -- Read current ELO; default to 1000 for new users
  SELECT elo_score INTO v_current_elo
    FROM public.user_progress
   WHERE user_id = p_user_id;

  IF NOT FOUND THEN
    v_current_elo := 1000;
  END IF;

  v_performance := COALESCE(p_correct::FLOAT / NULLIF(p_total, 0), 0.0);

  v_time_factor := CASE
    WHEN p_total > 0 AND (p_time_ms / p_total) < 5000 THEN 0.1
    ELSE 0.0
  END;

  v_k_factor := CASE
    WHEN v_current_elo < 1200 THEN 40
    WHEN v_current_elo < 2100 THEN 32
    ELSE 24
  END;

  v_delta := ROUND(v_k_factor * (v_performance + v_time_factor - 0.5))::INT;
  v_new_elo := GREATEST(0, LEAST(3000, v_current_elo + v_delta));
  v_rank_tier := public.get_rank_tier(v_new_elo);

  UPDATE public.user_progress
     SET elo_score  = v_new_elo,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  INSERT INTO public.leaderboard (user_id, username, elo_score, weekly_elo, rank_tier, updated_at)
  SELECT
    p_user_id,
    u.username,
    v_new_elo,
    GREATEST(0, COALESCE(l.weekly_elo, 0) + v_delta),
    v_rank_tier,
    NOW()
  FROM public.users u
  LEFT JOIN public.leaderboard l ON l.user_id = p_user_id
  WHERE u.id = p_user_id
  ON CONFLICT (user_id) DO UPDATE
    SET elo_score  = EXCLUDED.elo_score,
        weekly_elo = GREATEST(0, leaderboard.weekly_elo + v_delta),
        rank_tier  = EXCLUDED.rank_tier,
        updated_at = EXCLUDED.updated_at;

  RETURN json_build_object(
    'new_elo',    v_new_elo,
    'rank_tier',  v_rank_tier,
    'elo_delta',  v_delta
  );
END;
$$;

-- ---------------------------------------------------------------------------
-- CR-02 patch: re-deploy update_streak with the auth.uid() guard.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.update_streak(
  p_user_id    UUID,
  p_today_date DATE
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_last_date   DATE;
  v_streak      INT;
  v_freeze      INT;
  v_days_diff   INT;
  v_was_freeze  BOOLEAN := FALSE;
BEGIN
  -- Security: reject calls where p_user_id ≠ the authenticated caller (CR-02)
  IF p_user_id <> auth.uid() THEN
    RAISE EXCEPTION 'unauthorized'
      USING HINT = 'p_user_id must equal the calling user''s auth.uid()';
  END IF;

  SELECT last_practice_date, streak_count, streak_freeze_count
    INTO v_last_date, v_streak, v_freeze
    FROM public.user_progress
   WHERE user_id = p_user_id;

  IF NOT FOUND THEN
    v_last_date := NULL;
    v_streak    := 0;
    v_freeze    := 0;
  END IF;

  v_days_diff := CASE
    WHEN v_last_date IS NULL THEN 999
    ELSE (p_today_date - v_last_date)
  END;

  IF v_days_diff = 0 THEN
    RETURN json_build_object(
      'streak_count',           v_streak,
      'streak_freeze_available', v_freeze > 0,
      'was_freeze_used',         FALSE
    );
  ELSIF v_days_diff = 1 THEN
    v_streak := v_streak + 1;
  ELSIF v_days_diff > 1 AND v_freeze > 0 THEN
    v_freeze     := v_freeze - 1;
    v_was_freeze := TRUE;
  ELSE
    v_streak := 1;
  END IF;

  UPDATE public.user_progress
     SET streak_count        = v_streak,
         streak_freeze_count = v_freeze,
         last_practice_date  = p_today_date,
         updated_at          = NOW()
   WHERE user_id = p_user_id;

  INSERT INTO public.streak_history (user_id, practiced_date)
  VALUES (p_user_id, p_today_date)
  ON CONFLICT DO NOTHING;

  RETURN json_build_object(
    'streak_count',           v_streak,
    'streak_freeze_available', v_freeze > 0,
    'was_freeze_used',         v_was_freeze
  );
END;
$$;

-- ---------------------------------------------------------------------------
-- CR-02 + WA-06 patch: re-deploy award_daily_bonus with:
--   1. auth.uid() caller identity guard (CR-02)
--   2. CURRENT_DATE for streak credit instead of client-supplied date (WA-06)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.award_daily_bonus(
  p_user_id       UUID,
  p_challenge_date DATE
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_already_completed BOOLEAN;
  v_elo_earned        INTEGER := 50;
BEGIN
  -- Security: reject calls where p_user_id ≠ the authenticated caller (CR-02)
  IF p_user_id <> auth.uid() THEN
    RAISE EXCEPTION 'unauthorized'
      USING HINT = 'p_user_id must equal the calling user''s auth.uid()';
  END IF;

  SELECT EXISTS(
    SELECT 1
      FROM public.daily_completions
     WHERE user_id       = p_user_id
       AND challenge_date = p_challenge_date
  ) INTO v_already_completed;

  IF v_already_completed THEN
    RAISE EXCEPTION 'already_completed'
      USING HINT = 'Daily challenge already completed for this date';
  END IF;

  UPDATE public.user_progress
     SET elo_score  = elo_score + v_elo_earned,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  UPDATE public.leaderboard
     SET elo_score  = elo_score  + v_elo_earned,
         weekly_elo = weekly_elo + v_elo_earned,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  INSERT INTO public.daily_completions (user_id, challenge_date, elo_earned)
  VALUES (p_user_id, p_challenge_date, v_elo_earned);

  -- WA-06: use CURRENT_DATE (server date) for streak credit, not the
  -- client-supplied challenge date, to prevent stale-challenge streak fraud.
  PERFORM public.update_streak(p_user_id, CURRENT_DATE);

  RETURN v_elo_earned;
END;
$$;

REVOKE ALL ON FUNCTION public.award_daily_bonus(UUID, DATE) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.award_daily_bonus(UUID, DATE) TO authenticated;
