---
phase: 06-web-admin
plan: "01"
subsystem: web-admin
tags: [next.js, supabase, tailwind, auth, sql-migration, admin-dashboard]
dependency_graph:
  requires: []
  provides:
    - supabase/migrations/008_admin_schema.sql
    - web-admin/package.json
    - web-admin/middleware.ts
    - web-admin/src/lib/supabase/server.ts
    - web-admin/src/lib/supabase/admin.ts
    - web-admin/src/app/admin/layout.tsx
  affects:
    - supabase schema (users.is_admin column, words table)
    - web-admin project scaffold
tech_stack:
  added:
    - next@15.3.9
    - react@^19.0.0
    - "@supabase/ssr@0.12.3"
    - "@supabase/supabase-js@^2.49.0"
    - tailwindcss@^4
    - "@tailwindcss/postcss@^4"
    - recharts@^2.15.0
  patterns:
    - Next.js 15 App Router with Server Components
    - Tailwind v4 CSS-first config (no tailwind.config.js)
    - "@supabase/ssr createServerClient for SSR session management"
    - Server Actions for form handling
    - Middleware-based route protection
key_files:
  created:
    - supabase/migrations/008_admin_schema.sql
    - web-admin/package.json
    - web-admin/tsconfig.json
    - web-admin/next.config.ts
    - web-admin/postcss.config.mjs
    - web-admin/.gitignore
    - web-admin/.env.local.example
    - web-admin/src/app/globals.css
    - web-admin/src/app/layout.tsx
    - web-admin/src/app/page.tsx
    - web-admin/middleware.ts
    - web-admin/src/lib/supabase/server.ts
    - web-admin/src/lib/supabase/admin.ts
    - web-admin/src/lib/supabase/client.ts
    - web-admin/src/app/login/page.tsx
    - web-admin/src/app/login/actions.ts
    - web-admin/src/app/unauthorized/page.tsx
    - web-admin/src/app/admin/layout.tsx
    - web-admin/src/app/admin/page.tsx
  modified: []
decisions:
  - "Used @supabase/ssr@0.12.3 (not deprecated @supabase/auth-helpers)"
  - "getUser() over getSession() in middleware for server-side JWT validation (T-06-01)"
  - "SUPABASE_SERVICE_ROLE_KEY in admin.ts only — never NEXT_PUBLIC_ prefix (T-06-03)"
  - "Tailwind v4 CSS-first — @theme block in globals.css, no tailwind.config.js"
  - "middleware.ts at project root (not in src/) per Next.js App Router convention"
  - "Added client.ts (createBrowserClient) alongside server.ts for future client component use"
metrics:
  duration: "~15 minutes"
  completed: "2026-07-23"
  tasks_completed: 3
  files_created: 19
  commits: 2
---

# Phase 6 Plan 01: SQL Migration 008 + Next.js 15 Scaffold + Supabase SSR Auth + Admin Layout Summary

**One-liner:** SQL migration adds is_admin + words table with RLS; Next.js 15 admin project scaffolded with Tailwind v4 CSS-first config, @supabase/ssr 0.12.3 middleware route protection, Server Action login, and sidebar admin layout.

## What Was Built

### Task 1: SQL Migration 008
Created `supabase/migrations/008_admin_schema.sql` — idempotent DDL:
- `ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE` (does NOT re-add is_banned — that's from 001)
- `CREATE TABLE IF NOT EXISTS public.words` with id (bigserial PK), word text, classifier text, category text, image_url text, distractors jsonb, created_at timestamptz
- RLS ENABLED on words
- 4 RLS policies: `words_select_public` (SELECT USING TRUE), `words_admin_write`, `words_admin_update`, `words_admin_delete` (authenticated + is_admin=true check via EXISTS subquery)
- `CREATE INDEX IF NOT EXISTS idx_words_category ON public.words (category)`
- **Commit:** `e47fa96`

### Task 2: Next.js 15 Project Scaffold
Manually created (no interactive create-next-app) at `web-admin/`:
- `package.json` — next@15.3.9, @supabase/ssr@0.12.3, tailwindcss@^4
- `tsconfig.json` — strict: true, moduleResolution: bundler, paths @/* → ./src/*
- `next.config.ts` — TypeScript config with images.remotePatterns for *.supabase.co
- `postcss.config.mjs` — @tailwindcss/postcss integration (Tailwind v4 CSS-first)
- `globals.css` — `@import "tailwindcss"` + `@theme {}` block with --color-viet-red, --color-viet-yellow, and full design token set
- `src/app/layout.tsx` — root layout, metadata title "VietForces Admin"
- `npm install` completed successfully

### Task 3: Auth + Middleware + Login + Admin Layout
- `src/lib/supabase/server.ts` — async createClient() with await cookies() (Next.js 15 pattern)
- `src/lib/supabase/admin.ts` — createAdminClient() with SUPABASE_SERVICE_ROLE_KEY, autoRefreshToken: false, persistSession: false
- `src/lib/supabase/client.ts` — createClient() with createBrowserClient for future client component use
- `middleware.ts` — protects /admin/:path* with getUser() JWT validation + is_admin check; redirects authed admin from /login to /admin/vocabulary
- `src/app/login/actions.ts` — 'use server' loginAction + signOutAction Server Actions
- `src/app/login/page.tsx` — centered card form with error display, Tailwind CSS styled
- `src/app/unauthorized/page.tsx` — "Không có quyền truy cập" message
- `src/app/admin/layout.tsx` — dark sidebar (w-64) with Vocabulary/Users/Analytics/Daily Challenges links + user email + Sign Out button
- `src/app/admin/page.tsx` — redirects to /admin/vocabulary
- `npx tsc --noEmit` — **zero errors**
- **Commit:** `6ee2281`

## Deviations from Plan

None — plan executed exactly as written.

Note: The prompt's critical context mentioned `vietforces-admin` as the package name, but the plan task spec said `web-admin`. Used `web-admin` to match the plan spec. The client.ts file was added (not in the original plan file list but required for completeness — browser client for future client components).

## Known Stubs

None — this plan is a scaffold with routing/auth, not data-rendering pages. Subsequent plans (06-02, 06-03) will add vocabulary CRUD pages.

## Threat Flags

No new surface beyond what is documented in the plan's threat model. All T-06-01 through T-06-05 mitigations applied:
- T-06-01: getUser() used in middleware (not getSession())
- T-06-02: is_admin checked from DB on every /admin/* request
- T-06-03: SUPABASE_SERVICE_ROLE_KEY only in src/lib/supabase/admin.ts
- T-06-04: loginAction error messages encoded before redirect
- T-06-05: All packages are established ecosystem libraries

## Self-Check: PASSED

Files exist:
- supabase/migrations/008_admin_schema.sql ✓
- web-admin/package.json ✓
- web-admin/middleware.ts ✓
- web-admin/src/lib/supabase/server.ts ✓
- web-admin/src/lib/supabase/admin.ts ✓
- web-admin/src/app/admin/layout.tsx ✓

Commits exist:
- e47fa96 (migration 008) ✓
- 6ee2281 (scaffold + auth) ✓

TypeScript: `npx tsc --noEmit` exits 0 ✓
