-- ============================================================
-- Migration  : 008_admin_schema
-- Date       : 2026-07-23
-- Description: Admin schema additions — is_admin flag on users +
--              vocabulary words table with RLS policies.
-- Idempotent : yes — ALTER TABLE ADD COLUMN IF NOT EXISTS +
--              CREATE TABLE IF NOT EXISTS + CREATE POLICY
-- ============================================================

-- ---------------------------------------------------------------------------
-- TABLE: public.users — add is_admin column
-- NOTE: is_banned was added in 001_initial_schema.sql. Do NOT add it again.
-- ---------------------------------------------------------------------------
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS
  is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------------------
-- TABLE: public.words
-- Vocabulary entries managed by admins via the web dashboard.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.words (
  id          BIGSERIAL   PRIMARY KEY,
  word        TEXT        NOT NULL,
  classifier  TEXT        NOT NULL DEFAULT '',
  category    TEXT        NOT NULL DEFAULT 'general',
  image_url   TEXT,
  distractors JSONB       NOT NULL DEFAULT '[]',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.words ENABLE ROW LEVEL SECURITY;

-- Public read — all users (including anonymous) can read vocabulary
CREATE POLICY "words_select_public"
  ON public.words FOR SELECT
  USING (TRUE);

-- Admin write — only authenticated admins can insert
CREATE POLICY "words_admin_write"
  ON public.words FOR INSERT
  TO authenticated
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.users
      WHERE id = auth.uid() AND is_admin = TRUE
    )
  );

-- Admin update — only authenticated admins can update
CREATE POLICY "words_admin_update"
  ON public.words FOR UPDATE
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.users
      WHERE id = auth.uid() AND is_admin = TRUE
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.users
      WHERE id = auth.uid() AND is_admin = TRUE
    )
  );

-- Admin delete — only authenticated admins can delete
CREATE POLICY "words_admin_delete"
  ON public.words FOR DELETE
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.users
      WHERE id = auth.uid() AND is_admin = TRUE
    )
  );

-- Index for category filtering (used by vocabulary list page)
CREATE INDEX IF NOT EXISTS idx_words_category ON public.words (category);
