---
phase: 01-supabase-foundation
plan: "04"
subsystem: ci
tags: [github-actions, supabase, keepalive, cron, free-tier]
dependency_graph:
  requires: []
  provides: [supabase-keepalive-workflow]
  affects: [supabase-project-uptime]
tech_stack:
  added: [GitHub Actions cron workflow]
  patterns: [scheduled workflow, secret injection via ${{ secrets.* }}]
key_files:
  created:
    - .github/workflows/supabase-keepalive.yml
  modified: []
decisions:
  - "Used --fail-with-body instead of inspecting HTTP status code manually — workflow step fails visibly on Supabase errors without extra shell logic"
  - "Accepted SUPABASE_URL as non-sensitive (same value baked into APK); only SUPABASE_ANON_KEY masked via GitHub secrets"
  - "Single curl step + log step kept workflow under 40 lines as specified"
metrics:
  duration: "< 5 minutes"
  completed: "2026-07-22T17:00:09Z"
  tasks_completed: 1
  tasks_total: 1
  files_created: 1
  files_modified: 0
requirements_fulfilled:
  - FOUND-05
---

# Phase 01 Plan 04: Supabase Keepalive Workflow Summary

**One-liner:** GitHub Actions cron pinging Supabase REST `/rest/v1/` daily at noon UTC via `--fail-with-body` curl to prevent free-tier 7-day pause.

## What Was Built

A single GitHub Actions workflow file (`.github/workflows/supabase-keepalive.yml`) that:

- Runs automatically every day at **noon UTC** (`cron: '0 12 * * *'`) — well within the 7-day Supabase inactivity window
- Makes an authenticated `curl` GET to `${{ secrets.SUPABASE_URL }}/rest/v1/` with `apikey` and `Authorization` headers
- Uses `--fail-with-body` so the workflow step fails visibly (non-zero exit) on HTTP 4xx/5xx
- Uses `--max-time 30` to prevent hanging
- Supports `workflow_dispatch` for manual trigger / testing
- Logs a timestamped confirmation on success

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create supabase-keepalive GitHub Actions workflow | 8e7ffc7 | `.github/workflows/supabase-keepalive.yml` (created) |

## Verification Results

All success criteria passed:

- ✅ `.github/workflows/supabase-keepalive.yml` exists
- ✅ Cron schedule `0 12 * * *` present
- ✅ No hardcoded secrets — uses `${{ secrets.SUPABASE_URL }}` and `${{ secrets.SUPABASE_ANON_KEY }}`
- ✅ `workflow_dispatch` trigger present for manual testing
- ✅ `--fail-with-body` flag ensures workflow fails visibly on Supabase errors
- ✅ File is 31 lines (under 40-line limit)
- ✅ Valid YAML

## User Setup Required

Before the workflow can run successfully, add these two secrets in **GitHub repo → Settings → Secrets and variables → Actions**:

| Secret Name | Value |
|-------------|-------|
| `SUPABASE_URL` | Your Supabase project URL, e.g. `https://xxxx.supabase.co` |
| `SUPABASE_ANON_KEY` | Your Supabase anon/public key |

## Decisions Made

1. **`--fail-with-body` over manual status-code check:** Cleaner than capturing `%{http_code}` and branching in shell — GitHub Actions will mark the step red automatically, surfacing failures in the UI without extra logic.
2. **`SUPABASE_URL` as secret (not inline):** Although the URL is not sensitive, storing it as a secret keeps the workflow portable across forks and avoids littering the YAML with environment-specific values.
3. **No third-party actions:** `ubuntu-latest` + system `curl` only — zero supply-chain surface beyond GitHub's own infrastructure (T-04-SC accepted).

## Deviations from Plan

None — plan executed exactly as written. The workflow YAML matches the task specification. The `--fail-with-body` flag and two-step structure (ping + log) were specified in the plan and implemented as described.

## Threat Surface Scan

No new security surface beyond what the plan's threat model covers:

- `SUPABASE_ANON_KEY` is injected via `${{ secrets.* }}` — GitHub masks it in logs automatically (T-04-01 mitigated)
- Only system `curl` used — no third-party actions (T-04-SC accepted)

## Known Stubs

None — this plan creates infrastructure only (a GitHub Actions YAML). No UI components or data bindings.

## Self-Check: PASSED

- `.github/workflows/supabase-keepalive.yml` — FOUND ✅
- Commit `8e7ffc7` — FOUND ✅
