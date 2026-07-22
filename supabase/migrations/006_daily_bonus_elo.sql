-- ============================================================
-- Migration  : 006_daily_bonus_elo
-- Date       : 2026-07-23
-- Description: SECURITY DEFINER function that awards +50 bonus ELO
--              for completing today's daily challenge.
--              Guards against double-completion via RAISE EXCEPTION.
--              Updates user_progress + leaderboard, inserts into
--              daily_completions, and calls update_streak().
-- Idempotent : yes — CREATE OR REPLACE FUNCTION
-- ============================================================

-- ---------------------------------------------------------------------------
-- PUBLIC RPC: award_daily_bonus(p_user_id, p_challenge_date)
--
-- Called by authenticated clients after completing a daily challenge.
-- Returns the ELO amount earned (always 50 on first call per day).
-- Raises an exception with code 'already_completed' if the user has
-- already completed this date's challenge (Threat T-04-03 mitigation).
--
-- Security notes:
--   • SECURITY DEFINER — runs as the migration owner, bypassing RLS.
--     Client cannot forge another user_id because the Supabase SDK
--     validates the JWT before reaching this function; the Android
--     caller passes auth.uid() which is bound to the verified JWT.
--   • SET search_path = public — prevents search_path injection.
--   • REVOKE ALL / GRANT EXECUTE restricts callers to authenticated role.
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
  -- ----------------------------------------------------------------
  -- Guard: reject double-completion at the DB level
  -- ----------------------------------------------------------------
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

  -- ----------------------------------------------------------------
  -- Award ELO to user_progress
  -- ----------------------------------------------------------------
  UPDATE public.user_progress
     SET elo_score  = elo_score + v_elo_earned,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  -- ----------------------------------------------------------------
  -- Reflect ELO gain in leaderboard (both all-time and weekly)
  -- ----------------------------------------------------------------
  UPDATE public.leaderboard
     SET elo_score  = elo_score  + v_elo_earned,
         weekly_elo = weekly_elo + v_elo_earned,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  -- ----------------------------------------------------------------
  -- Record completion (PK constraint prevents concurrent duplicates)
  -- ----------------------------------------------------------------
  INSERT INTO public.daily_completions (user_id, challenge_date, elo_earned)
  VALUES (p_user_id, p_challenge_date, v_elo_earned);

  -- ----------------------------------------------------------------
  -- Credit streak — calls existing update_streak() (003_streak_function)
  -- ----------------------------------------------------------------
  PERFORM public.update_streak(p_user_id, p_challenge_date);

  RETURN v_elo_earned;
END;
$$;

-- Restrict to authenticated users only (Threat T-04-03 mitigation)
REVOKE ALL ON FUNCTION public.award_daily_bonus(UUID, DATE) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.award_daily_bonus(UUID, DATE) TO authenticated;
