-- ============================================================
-- Migration  : 007_activity_feed
-- Date       : 2026-07-23
-- Description: activity_events table + daily_completion trigger
--              + public RLS policies for social features
--              (SOCIAL-01, SOCIAL-03, SOCIAL-04)
-- Idempotent : yes — CREATE TABLE IF NOT EXISTS,
--              CREATE POLICY,
--              CREATE OR REPLACE FUNCTION / TRIGGER throughout
-- ============================================================

-- ---------------------------------------------------------------------------
-- TABLE: public.activity_events
-- Feed of user actions visible to followers.
-- event_type values: 'daily_completion', 'elo_milestone' (future)
-- Populated exclusively by the on_daily_completion_insert() SECURITY DEFINER
-- trigger; no direct client INSERT policy (Threat T-05-01-01 mitigation).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.activity_events (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  event_type TEXT        NOT NULL,
  metadata   JSONB       NOT NULL DEFAULT '{}'::JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.activity_events ENABLE ROW LEVEL SECURITY;

-- Index for per-user feed queries
CREATE INDEX IF NOT EXISTS idx_activity_events_user_id
  ON public.activity_events (user_id);

-- Index for chronological feed queries (most-recent first)
CREATE INDEX IF NOT EXISTS idx_activity_events_created_at
  ON public.activity_events (created_at DESC);

-- ---------------------------------------------------------------------------
-- RLS: public.activity_events
-- ---------------------------------------------------------------------------

-- Authenticated user can read their own events AND events of users they follow
-- (asymmetric follow model: follower_id = current user, following_id = event author)
CREATE POLICY "activity_events_select_following"
  ON public.activity_events FOR SELECT
  USING (
    user_id = auth.uid()
    OR EXISTS (
      SELECT 1
      FROM public.friendships
      WHERE friendships.follower_id  = auth.uid()
        AND friendships.following_id = activity_events.user_id
    )
  );

-- Only service_role may INSERT directly (e.g. admin back-fill).
-- The on_daily_completion_insert() SECURITY DEFINER trigger bypasses RLS,
-- so this policy is belt-and-suspenders for direct service_role writes.
CREATE POLICY "activity_events_service_insert"
  ON public.activity_events FOR INSERT
  TO service_role
  WITH CHECK (TRUE);

-- ---------------------------------------------------------------------------
-- TRIGGER FUNCTION: public.on_daily_completion_insert()
-- Fires AFTER INSERT on public.daily_completions.
-- Writes one activity_events row per completion.
-- SECURITY DEFINER so it can bypass the service_role-only INSERT policy.
-- SET search_path = public prevents search_path-hijack attacks (T-05-01-02).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.on_daily_completion_insert()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path = public
AS $$
BEGIN
  INSERT INTO public.activity_events (user_id, event_type, metadata)
  VALUES (
    NEW.user_id,
    'daily_completion',
    jsonb_build_object(
      'challenge_date', NEW.challenge_date::TEXT,
      'elo_earned',     NEW.elo_earned
    )
  );
  RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- TRIGGER: after_daily_completion_insert
-- Bound to daily_completions; fires on every new completion row.
-- CREATE OR REPLACE TRIGGER requires Postgres 14+ (Supabase Cloud is 15+).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER after_daily_completion_insert
  AFTER INSERT ON public.daily_completions
  FOR EACH ROW
  EXECUTE FUNCTION public.on_daily_completion_insert();

-- ---------------------------------------------------------------------------
-- RLS PATCH: public.users — public username search (SOCIAL-01)
-- Allows any authenticated user to read all user rows for ILIKE username search.
-- Multiple SELECT policies are OR'd; existing users_select_own is unchanged.
-- Security note: only username, id, avatar_url are queried by the Android client;
-- is_banned, fcm_token, timezone are NOT projected in search queries (T-05-01-03).
-- ---------------------------------------------------------------------------
CREATE POLICY "users_select_public_username"
  ON public.users FOR SELECT
  USING (TRUE);

-- ---------------------------------------------------------------------------
-- RLS PATCH: public.user_progress — public profile stats (SOCIAL-03)
-- Allows any authenticated user to read any user_progress row.
-- Exposes gamification stats (elo_score, streak_count, total_games) for
-- PublicProfileViewModel — low sensitivity for academic project (T-05-01-04).
-- Existing progress_select_own / progress_update_own policies are unchanged.
-- ---------------------------------------------------------------------------
CREATE POLICY "progress_select_public"
  ON public.user_progress FOR SELECT
  USING (TRUE);
