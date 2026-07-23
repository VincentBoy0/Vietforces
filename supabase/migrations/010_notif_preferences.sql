-- ============================================================
-- Migration  : 010_notif_preferences
-- Date       : 2026-07-23
-- Description: Add server-side notification preference columns so the
--              send-streak-reminder Edge Function can respect per-user
--              opt-out settings (WA-05).
-- Idempotent : yes — ADD COLUMN IF NOT EXISTS
-- ============================================================

-- Add notification preference columns to users table.
-- Both default to TRUE so existing users continue receiving notifications
-- until they explicitly opt out.
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS notif_streak_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS notif_daily_enabled   BOOLEAN NOT NULL DEFAULT TRUE;

-- Allow authenticated users to update their own notification preferences.
-- The existing users_select_public_username policy already allows SELECT;
-- we add an UPDATE policy scoped to these two columns only.
CREATE POLICY IF NOT EXISTS "users_update_notif_prefs"
  ON public.users FOR UPDATE
  USING (id = auth.uid())
  WITH CHECK (id = auth.uid());
