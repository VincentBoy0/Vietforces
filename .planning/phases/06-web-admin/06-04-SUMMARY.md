---
phase: 06-web-admin
plan: "04"
subsystem: web-admin
tags: [analytics, daily-challenges, recharts, server-actions, next15]
dependency_graph:
  requires: [06-01, 06-02, 06-03]
  provides: [ADMIN-07, ADMIN-08]
  affects: [web-admin]
tech_stack:
  added: []
  patterns: [server-actions, server-components, recharts, upsert-pattern]
key_files:
  created:
    - web-admin/src/lib/actions/analytics.ts
    - web-admin/src/app/admin/analytics/charts.tsx
    - web-admin/src/app/admin/analytics/page.tsx
    - web-admin/src/lib/actions/daily-challenges.ts
    - web-admin/src/app/admin/daily-challenges/page.tsx
  modified: []
decisions:
  - "DAU aggregated in JS (not SQL GROUP BY) to avoid RPC round-trips with supabase-js"
  - "GameModesChart uses derived stats (daily_completions + user_progress) since no explicit mode column exists"
  - "Form action wrapper (upsertChallengeAction) added to satisfy TS strict Promise<void> constraint"
  - "upsert uses onConflict: challenge_date to allow overriding auto-generated challenges"
metrics:
  duration: "~20 minutes"
  completed: "2026-07-23"
  tasks: 5
  files: 5
---

# Phase 6 Plan 04: Analytics Dashboard + Daily Challenge Scheduler Summary

Analytics page at `/admin/analytics` showing 30-day DAU line chart (VietRed) and top game-mode bar chart (VietYellow) with 4 summary stat cards; daily challenge CRUD at `/admin/daily-challenges` with ±7-day table and upsert form.

## What Was Built

### Analytics (ADMIN-07)

**`src/lib/actions/analytics.ts`** — Three server actions:
- `getDauLast30Days()` — Fetches `user_progress.updated_at` records for the last 30 days, aggregates per-day in JS, returns a filled 30-day series (zero-filled for days with no activity).
- `getTopGameModes()` — Derives mode stats from `daily_completions` count and `user_progress.streak_days > 0`. Returns 5 mode rows (last 3 are 0 until more event tracking is added).
- `getSummaryStats()` — Parallel queries for total users, active today, total completions, and average streak days.

**`src/app/admin/analytics/charts.tsx`** — `'use client'` component with:
- `DauChart` — Recharts `LineChart`, 400px, VietRed (`#DA251D`) line, date-formatted X-axis.
- `GameModesChart` — Recharts `BarChart`, 400px, VietYellow (`#FFCD00`) bars with radius.

**`src/app/admin/analytics/page.tsx`** — Server Component: parallel `Promise.all` fetch of all 3 data sources, 4 stat cards grid, renders both charts.

### Daily Challenge Scheduler (ADMIN-08)

**`src/lib/actions/daily-challenges.ts`** — Three server actions:
- `getUpcomingChallenges()` — SELECT challenges in [today−7, today+7] range, enriched with per-date completion counts.
- `upsertChallenge(formData)` — Validates date + wordIds (comma-separated), upserts with `onConflict: challenge_date`, revalidates path.
- `deleteChallenge(id)` — Hard delete by id, revalidates path.

**`src/app/admin/daily-challenges/page.tsx`** — Server Component:
- Sortable table with Date / Word Count / Bonus ELO / Completions / Delete action.
- Inline "Tạo / Cập nhật thách đấu" form: date picker, word IDs textarea with hint showing first 5 words from DB, bonus ELO number input.
- Past challenges highlighted with gray tint; today's date tagged.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] TypeScript strict: form action return type mismatch**
- **Found during:** TypeScript check after Task 4
- **Issue:** `<form action={upsertChallenge}>` failed because `upsertChallenge` returns `Promise<{ error?: string }>` but form `action` prop requires `(fd: FormData) => void | Promise<void>`
- **Fix:** Added `upsertChallengeAction` wrapper in `page.tsx` with `'use server'` that calls `upsertChallenge` and discards the return value
- **Files modified:** `web-admin/src/app/admin/daily-challenges/page.tsx`
- **Commit:** 9c7285d

## Commits

| Task | Description | Commit |
|------|-------------|--------|
| 1–5 | All 5 files + TS fix | 9c7285d |

## Known Stubs

- `getTopGameModes()` returns `count: 0` for Vocabulary Quiz, Flash Cards, and Practice Mode — no event tracking table exists yet. These modes will remain 0 until an `activity_events` table is added in a future phase.

## Threat Flags

None — this plan only reads data via service-role admin client (bypasses RLS by design for admin panel) and writes to `daily_challenges` which is an admin-only table. No new user-facing auth paths introduced.

## Self-Check: PASSED

- [x] `web-admin/src/lib/actions/analytics.ts` — FOUND
- [x] `web-admin/src/app/admin/analytics/charts.tsx` — FOUND
- [x] `web-admin/src/app/admin/analytics/page.tsx` — FOUND
- [x] `web-admin/src/lib/actions/daily-challenges.ts` — FOUND
- [x] `web-admin/src/app/admin/daily-challenges/page.tsx` — FOUND
- [x] Commit 9c7285d — FOUND (`git log --oneline -1`)
- [x] TypeScript: 0 errors
