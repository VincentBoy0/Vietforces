-- Migration 011: Auto-create public.users row on auth.users insert
-- Description: handle_new_user trigger + backfill + fix RLS for admin reads

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. TRIGGER FUNCTION: auto-create public.users row when auth user registers
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_username TEXT;
BEGIN
  -- Prefer username from metadata, fallback to email prefix, then UUID prefix
  v_username := COALESCE(
    NULLIF(TRIM(NEW.raw_user_meta_data->>'username'), ''),
    NULLIF(SPLIT_PART(NEW.email, '@', 1), ''),
    'user_' || SUBSTR(NEW.id::TEXT, 1, 8)
  );

  -- Ensure uniqueness by appending short suffix if collision
  IF EXISTS (SELECT 1 FROM public.users WHERE username = v_username) THEN
    v_username := v_username || '_' || SUBSTR(NEW.id::TEXT, 1, 4);
  END IF;

  INSERT INTO public.users (id, username, created_at)
  VALUES (NEW.id, v_username, NOW())
  ON CONFLICT (id) DO NOTHING;

  RETURN NEW;
END;
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. TRIGGER: fire after every new auth.users insert
-- ─────────────────────────────────────────────────────────────────────────────
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. BACKFILL: create public.users rows for existing auth.users that lack them
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO public.users (id, username, created_at)
SELECT
  au.id,
  COALESCE(
    NULLIF(TRIM(au.raw_user_meta_data->>'username'), ''),
    NULLIF(SPLIT_PART(au.email, '@', 1), ''),
    'user_' || SUBSTR(au.id::TEXT, 1, 8)
  ) AS username,
  au.created_at
FROM auth.users au
WHERE NOT EXISTS (SELECT 1 FROM public.users pu WHERE pu.id = au.id)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. RLS: allow admins to SELECT all users (web admin dashboard)
-- ─────────────────────────────────────────────────────────────────────────────
DROP POLICY IF EXISTS "users_admin_select_all" ON public.users;
CREATE POLICY "users_admin_select_all"
  ON public.users FOR SELECT
  USING (
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND is_admin = TRUE)
  );
