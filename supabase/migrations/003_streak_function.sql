-- ============================================================
-- Migration  : 003_streak_function
-- Date       : 2026-07-23
-- Description: Streak tracking function + history table
-- Idempotent : yes — CREATE TABLE IF NOT EXISTS, CREATE OR REPLACE
-- ============================================================

-- ---------------------------------------------------------------------------
-- TABLE: public.streak_history
-- One row per (user, date) representing a day the user practiced.
-- Written only by the SECURITY DEFINER update_streak() function —
-- no direct client INSERT policy (Threat T-03-03 mitigation).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.streak_history (
  user_id        UUID  NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  practiced_date DATE  NOT NULL,
  PRIMARY KEY (user_id, practiced_date)
);

ALTER TABLE public.streak_history ENABLE ROW LEVEL SECURITY;

-- Users may read their own history (for streak heatmap, STREAK-04)
CREATE POLICY IF NOT EXISTS "streak_history_select_own"
  ON public.streak_history FOR SELECT
  USING (user_id = auth.uid());

-- No direct INSERT policy — only the SECURITY DEFINER function below may write.

-- ---------------------------------------------------------------------------
-- PUBLIC RPC: update_streak(p_user_id, p_today_date)
--
-- Called by authenticated clients after each game session.
-- The client sends today's UTC date — the server validates the gap and applies
-- freeze logic; the client never sends the streak delta directly.
--
-- Threat T-03-02 mitigation: server computes gap; single-day increments only.
-- Threat T-03-03 mitigation: SECURITY DEFINER writes streak_history — client
--   cannot forge historical dates via direct INSERT.
--
-- Returns JSON: { streak_count, streak_freeze_available, was_freeze_used }
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
  -- Fetch current streak state; use defaults for brand-new users
  SELECT last_practice_date, streak_count, streak_freeze_count
    INTO v_last_date, v_streak, v_freeze
    FROM public.user_progress
   WHERE user_id = p_user_id;

  IF NOT FOUND THEN
    v_last_date := NULL;
    v_streak    := 0;
    v_freeze    := 0;
  END IF;

  -- NULL-safe gap: treat no prior practice as a large gap (forces streak = 1)
  v_days_diff := CASE
    WHEN v_last_date IS NULL THEN 999
    ELSE (p_today_date - v_last_date)
  END;

  -- Apply streak logic
  IF v_days_diff = 0 THEN
    -- Already practiced today — no change needed
    -- Return current state without mutating anything
    RETURN json_build_object(
      'streak_count',           v_streak,
      'streak_freeze_available', v_freeze > 0,
      'was_freeze_used',         FALSE
    );

  ELSIF v_days_diff = 1 THEN
    -- Consecutive day — extend streak
    v_streak := v_streak + 1;

  ELSIF v_days_diff > 1 AND v_freeze > 0 THEN
    -- Gap of 2+ days but freeze available — consume it, streak unchanged
    v_freeze     := v_freeze - 1;
    v_was_freeze := TRUE;

  ELSE
    -- Gap with no freeze — reset streak to 1
    v_streak := 1;
  END IF;

  -- Persist streak state
  UPDATE public.user_progress
     SET streak_count        = v_streak,
         streak_freeze_count = v_freeze,
         last_practice_date  = p_today_date,
         updated_at          = NOW()
   WHERE user_id = p_user_id;

  -- Record practice day in history (idempotent)
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

-- Grant EXECUTE to authenticated clients (RPC callable via Supabase client)
GRANT EXECUTE ON FUNCTION public.update_streak(UUID, DATE) TO authenticated;
