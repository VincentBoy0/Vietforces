---
phase: "07"
plan: "01"
subsystem: "web-landing"
tags: ["next.js", "tailwind", "landing-page", "static-export", "marketing"]
dependency_graph:
  requires: []
  provides: ["web-landing/"]
  affects: []
tech_stack:
  added:
    - "Next.js 15.3.9 (App Router, static export)"
    - "Tailwind CSS v4 (@tailwindcss/postcss)"
    - "TypeScript 5 (strict mode)"
  patterns:
    - "Single-file page component with all sections"
    - "Pure CSS phone mockup frames (no external libs)"
    - "CSS checkbox hamburger menu (no JS)"
key_files:
  created:
    - "web-landing/package.json"
    - "web-landing/tsconfig.json"
    - "web-landing/next.config.ts"
    - "web-landing/postcss.config.mjs"
    - "web-landing/.gitignore"
    - "web-landing/src/app/globals.css"
    - "web-landing/src/app/layout.tsx"
    - "web-landing/src/app/page.tsx"
  modified: []
decisions:
  - "Static export (output: 'export') for zero-cost Vercel deployment"
  - "Single page.tsx with all sections for simplicity — no route splitting needed"
  - "Pure CSS phone mockups to avoid dependencies and improve performance"
  - "CSS checkbox hamburger avoids any JS for mobile nav"
  - "Tailwind v4 @theme tokens for brand colors matching web-admin"
metrics:
  duration: "~15 minutes"
  completed: "2026-07-23"
  tasks_completed: 7
  files_created: 8
---

# Phase 7 Plan 01: VietForces Landing Page Summary

**One-liner:** Mobile-first Next.js 15 static landing page with 5 sections (hero, features, screenshots, download CTA, footer), CSS phone mockups, and full Vietnamese content.

## What Was Built

A standalone marketing site at `web-landing/` — a Next.js 15 App Router project with static export configured for Vercel. All content is in a single `page.tsx` with sections for:

1. **Navbar** — fixed/blurred, VietForces logo, desktop nav + pure CSS mobile hamburger
2. **Hero (LAND-02)** — full-viewport gradient (viet-dark → viet-red), left column with CTA buttons, right column CSS phone mockup with realistic app UI preview
3. **Features (LAND-03)** — 6-card responsive grid covering all major app features
4. **Screenshots (LAND-05)** — horizontal scroll row of 4 CSS phone frames (snap scroll on mobile)
5. **Download CTA (LAND-04)** — dark section with Google Play badge (inline SVG icon) + APK link
6. **Footer** — brand + links + graduation project attribution

## Requirements Satisfied

| Req | Description | Status |
|-----|-------------|--------|
| LAND-01 | Standalone Next.js 15 project scaffold | ✅ |
| LAND-02 | Hero section with phone mockup and CTA | ✅ |
| LAND-03 | Features grid with 6 cards | ✅ |
| LAND-04 | Download section with Play Store + APK | ✅ |
| LAND-05 | Screenshots section with CSS phone frames | ✅ |

## Commits

| Hash | Message |
|------|---------|
| `716967e` | feat(landing): add VietForces landing page with hero, features, screenshots, download CTA (LAND-01 to LAND-05) |

## Deviations from Plan

None — plan executed exactly as written. Added `.gitignore` to exclude `node_modules/` and `.next/` (Rule 2: missing critical file that would pollute the repo).

## Known Stubs

| File | Location | Reason |
|------|----------|--------|
| `page.tsx` | Google Play `href="#"` | APK/Play Store URL not yet published — placeholder |
| `page.tsx` | APK link `href="#"` | APK not yet distributed — placeholder |

These stubs are intentional — the app is pre-launch. The links will be updated when the Play Store listing goes live.

## Threat Flags

None — fully static public marketing page with no auth, no network requests, no user data collection.

## Self-Check: PASSED

- `web-landing/src/app/page.tsx` ✅ exists
- `web-landing/package.json` ✅ exists
- `web-landing/tsconfig.json` ✅ exists
- `web-landing/next.config.ts` ✅ exists
- `git log` commit `716967e` ✅ exists
- `tsc --noEmit` ✅ 0 errors
