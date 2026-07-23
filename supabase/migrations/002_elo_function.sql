-- ============================================================
-- Migration  : 002_elo_function
-- Date       : 2026-07-23
-- Description: ELO calculation SECURITY DEFINER function
-- Idempotent : yes — CREATE OR REPLACE throughout
-- ============================================================

-- ---------------------------------------------------------------------------
-- PRIVATE HELPER: get_rank_tier(elo INT) → TEXT
-- Codeforces-style tiers matching EloRank.kt
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_rank_tier(p_elo INT)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN CASE
    WHEN p_elo < 1200 THEN 'Newbie'
    WHEN p_elo < 1400 THEN 'Pupil'
    WHEN p_elo < 1600 THEN 'Specialist'
    WHEN p_elo < 1900 THEN 'Expert'
    WHEN p_elo < 2100 THEN 'Candidate Master'
    WHEN p_elo < 2300 THEN 'Master'
    WHEN p_elo < 2400 THEN 'International Master'
    WHEN p_elo < 2600 THEN 'Grandmaster'
    WHEN p_elo < 2900 THEN 'International Grandmaster'
    ELSE 'Legendary Grandmaster'
  END;
END;
$$;

-- ---------------------------------------------------------------------------
-- PUBLIC RPC: calculate_elo(p_user_id, p_correct, p_total, p_time_ms)
--
-- Called by authenticated clients after each game session.
-- The client sends only raw game metrics — never an ELO delta.
-- This function reads the current ELO from user_progress, computes the new
-- ELO server-side, updates both user_progress and leaderboard, and returns
-- the result as JSON.
--
-- Threat T-03-01 mitigation: SECURITY DEFINER + RLS ensures the client
-- cannot write elo_score directly; only this function may mutate it.
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

  -- Performance ratio: correct / total (0 if total is 0)
  v_performance := COALESCE(p_correct::FLOAT / NULLIF(p_total, 0), 0.0);

  -- Time bonus: +0.1 if average answer time < 5 seconds
  v_time_factor := CASE
    WHEN p_total > 0 AND (p_time_ms / p_total) < 5000 THEN 0.1
    ELSE 0.0
  END;

  -- Dynamic K-factor (higher K for lower-rated players)
  v_k_factor := CASE
    WHEN v_current_elo < 1200 THEN 40
    WHEN v_current_elo < 2100 THEN 32
    ELSE 24
  END;

  -- ELO delta: k * (actual_performance - expected_performance 0.5)
  v_delta := ROUND(v_k_factor * (v_performance + v_time_factor - 0.5))::INT;

  -- Clamp new ELO to [0, 3000]
  v_new_elo := GREATEST(0, LEAST(3000, v_current_elo + v_delta));

  v_rank_tier := public.get_rank_tier(v_new_elo);

  -- Update user_progress
  UPDATE public.user_progress
     SET elo_score  = v_new_elo,
         updated_at = NOW()
   WHERE user_id = p_user_id;

  -- Upsert leaderboard (weekly_elo tracks this week's delta only)
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

-- Grant EXECUTE to authenticated clients (RPC callable via Supabase client)
GRANT EXECUTE ON FUNCTION public.get_rank_tier(INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.calculate_elo(UUID, INT, INT, BIGINT) TO authenticated;
