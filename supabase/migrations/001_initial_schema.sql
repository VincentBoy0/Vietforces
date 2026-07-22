-- ============================================================
-- Migration  : 001_initial_schema
-- Date       : 2026-07-22
-- Description: Initial schema — 6 tables with RLS for VietForces v1
-- Idempotent : yes — CREATE TABLE IF NOT EXISTS throughout
-- ============================================================

-- ---------------------------------------------------------------------------
-- TABLE: public.users
-- Extends auth.users; one row per registered account.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.users (
  id          UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username    TEXT        UNIQUE NOT NULL,
  timezone    TEXT        NOT NULL DEFAULT 'UTC',
  avatar_url  TEXT,
  is_banned   BOOLEAN     NOT NULL DEFAULT FALSE,
  fcm_token   TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY IF NOT EXISTS "users_select_own"
  ON public.users FOR SELECT
  USING (id = auth.uid());

CREATE POLICY IF NOT EXISTS "users_update_own"
  ON public.users FOR UPDATE
  USING (id = auth.uid())
  WITH CHECK (id = auth.uid());

CREATE POLICY IF NOT EXISTS "users_insert_own"
  ON public.users FOR INSERT
  WITH CHECK (id = auth.uid());

-- ---------------------------------------------------------------------------
-- TABLE: public.user_progress
-- Cloud-synced per-user progress; one row per user.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_progress (
  id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID        NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
  elo_score           INTEGER     NOT NULL DEFAULT 0,
  streak_count        INTEGER     NOT NULL DEFAULT 0,
  streak_freeze_count INTEGER     NOT NULL DEFAULT 0,
  last_practice_date  DATE,
  total_games         INTEGER     NOT NULL DEFAULT 0,
  words_learned       JSONB       NOT NULL DEFAULT '[]'::JSONB,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY IF NOT EXISTS "progress_select_own"
  ON public.user_progress FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY IF NOT EXISTS "progress_insert_own"
  ON public.user_progress FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY IF NOT EXISTS "progress_update_own"
  ON public.user_progress FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- TABLE: public.leaderboard
-- Denormalized for Realtime performance.
-- Writes are restricted to service_role (Edge Functions / pg_cron — Phase 3+).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.leaderboard (
  user_id    UUID        PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  username   TEXT        NOT NULL,
  elo_score  INTEGER     NOT NULL DEFAULT 0,
  weekly_elo INTEGER     NOT NULL DEFAULT 0,
  rank_tier  TEXT        NOT NULL DEFAULT 'Bronze',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;

-- Public read — anyone can see the leaderboard
CREATE POLICY IF NOT EXISTS "leaderboard_select_all"
  ON public.leaderboard FOR SELECT
  USING (TRUE);

-- Only service_role may write
CREATE POLICY IF NOT EXISTS "leaderboard_service_insert"
  ON public.leaderboard FOR INSERT
  TO service_role
  WITH CHECK (TRUE);

CREATE POLICY IF NOT EXISTS "leaderboard_service_update"
  ON public.leaderboard FOR UPDATE
  TO service_role
  USING (TRUE);

-- ---------------------------------------------------------------------------
-- TABLE: public.daily_challenges
-- One row per calendar day; created by server functions.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.daily_challenges (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  challenge_date DATE        UNIQUE NOT NULL,
  game_mode      TEXT        NOT NULL,
  vocabulary_ids JSONB       NOT NULL DEFAULT '[]'::JSONB,
  bonus_elo      INTEGER     NOT NULL DEFAULT 50,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.daily_challenges ENABLE ROW LEVEL SECURITY;

-- Public read — all authenticated and anonymous users may read today's challenge
CREATE POLICY IF NOT EXISTS "challenges_select_all"
  ON public.daily_challenges FOR SELECT
  USING (TRUE);

-- Only service_role may insert new daily challenges
CREATE POLICY IF NOT EXISTS "challenges_service_insert"
  ON public.daily_challenges FOR INSERT
  TO service_role
  WITH CHECK (TRUE);

-- ---------------------------------------------------------------------------
-- TABLE: public.friendships
-- Asymmetric follow model: follower → following.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.friendships (
  follower_id  UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  following_id UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (follower_id, following_id),
  CHECK (follower_id <> following_id)
);

ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;

CREATE POLICY IF NOT EXISTS "friendships_select_own"
  ON public.friendships FOR SELECT
  USING (follower_id = auth.uid() OR following_id = auth.uid());

CREATE POLICY IF NOT EXISTS "friendships_insert_own"
  ON public.friendships FOR INSERT
  WITH CHECK (follower_id = auth.uid());

CREATE POLICY IF NOT EXISTS "friendships_delete_own"
  ON public.friendships FOR DELETE
  USING (follower_id = auth.uid());

-- ---------------------------------------------------------------------------
-- TABLE: public.fcm_tokens
-- Firebase Cloud Messaging push token per user; upserted on app start.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.fcm_tokens (
  user_id    UUID        PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  token      TEXT        NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

CREATE POLICY IF NOT EXISTS "fcm_select_own"
  ON public.fcm_tokens FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY IF NOT EXISTS "fcm_insert_own"
  ON public.fcm_tokens FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY IF NOT EXISTS "fcm_update_own"
  ON public.fcm_tokens FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Realtime: subscribe to leaderboard changes
-- ---------------------------------------------------------------------------
ALTER PUBLICATION supabase_realtime ADD TABLE public.leaderboard;
