# Phase 6 Context: Web Admin Dashboard

## Goal
Build a Next.js 15 App Router admin dashboard deployed on Vercel. Service-role Supabase key stays server-side only. Features: vocabulary CRUD with image upload, user management (ban/unban), analytics, and manual daily challenge scheduling.

## Requirements
ADMIN-01 through ADMIN-08

## Tech Stack (from research)
- Next.js 15.3.9 (App Router, stable LTS)
- Tailwind CSS v4 (CSS-first config via @theme, no tailwind.config.js)
- @supabase/ssr 0.12.3 (NOT deprecated auth-helpers)
- TypeScript strict mode
- Vercel deployment

## Project Location
`/web-admin/` directory at repo root (sibling to `app/`, `supabase/`)

## Architecture

### Auth Protection (ADMIN-01)
- Supabase Auth with email/password
- Admin check: `users.is_admin = true` column (add to schema)
- Server Component middleware checks session; redirect to /login if not admin
- SUPABASE_SERVICE_ROLE_KEY only in server-side env (never in client bundle)
- `middleware.ts` using `@supabase/ssr` createServerClient

### Vocabulary CRUD (ADMIN-02, ADMIN-03, ADMIN-04)
- `/admin/vocabulary` — paginated table with category filter
- `/admin/vocabulary/new` — form with image upload to Supabase Storage bucket `vocabulary-images`
- `/admin/vocabulary/[id]/edit` — edit form, delete button with confirmation
- Server Actions for mutations (no separate API routes needed in Next.js 15)
- `words` table needed: id, word text, classifier text, category text, image_url text, distractors jsonb, created_at

### User Management (ADMIN-05, ADMIN-06)
- `/admin/users` — DataTable: username, ELO, streak_days, last_active, is_banned status
- Server Action: `banUser(userId)` / `unbanUser(userId)` — UPDATE users SET is_banned = true/false
- Android app: checks `is_banned` on login → show "Tài khoản bị khóa" dialog

### Analytics (ADMIN-07)
- `/admin/analytics` — Charts using Recharts (lightweight, React-compatible)
- DAU: COUNT DISTINCT user_id from user_progress WHERE updated_at >= date - 30 days GROUP BY date
- Top game modes: could derive from daily_completions and general activity
- Simple bar/line charts; no real-time needed (static SSR refreshed on demand)

### Daily Challenge Scheduling (ADMIN-08)
- `/admin/daily-challenges` — list of upcoming/past challenges
- Manual form: pick date + select word IDs from vocabulary list
- Server Action calls Supabase service-role to INSERT into daily_challenges
- Override: if challenge exists for date, update word_ids

## New SQL (Migration 008)
- `008_admin_schema.sql`:
  * ADD COLUMN is_admin boolean DEFAULT false to users
  * ADD COLUMN is_banned boolean DEFAULT false to users
  * CREATE TABLE IF NOT EXISTS words (id bigserial PK, word text, classifier text, category text, image_url text, distractors jsonb DEFAULT '[]', created_at timestamptz DEFAULT now())
  * RLS on words: SELECT = public; INSERT/UPDATE/DELETE = is_admin only

## Wave Structure
- Wave 1 (parallel): 06-01 (SQL schema + Next.js project scaffold) + Android ban check
- Wave 2: 06-02 (Vocabulary CRUD pages)
- Wave 3: 06-03 (User management + Analytics + Daily challenge admin)
