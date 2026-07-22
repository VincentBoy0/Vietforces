# Supabase — VietForces Backend

This directory contains the Supabase configuration and database migrations for VietForces.

---

## Prerequisites

| Tool | Install |
|------|---------|
| Supabase CLI | `brew install supabase/tap/supabase` **or** `npm install -g supabase` |
| A Supabase project | Create at [supabase.com/dashboard](https://supabase.com/dashboard) → **New project** |

> **Free tier note:** Supabase pauses inactive projects after 7 days. Set up a keepalive ping
> (e.g. a GitHub Actions cron job hitting your project's health endpoint every 3 days) to
> avoid pauses in development: `GET https://<project-ref>.supabase.co/rest/v1/` with the
> `apikey` header.

---

## Apply the Migration

### Option A — Supabase CLI (recommended)

```bash
# 1. Link to your project (one-time setup)
supabase link --project-ref YOUR_PROJECT_REF

# 2. Push all migrations in filename order
supabase db push
```

The CLI applies every file under `supabase/migrations/` in alphanumeric order.
The migration is **idempotent** (`CREATE TABLE IF NOT EXISTS`) — re-running is safe.

### Option B — Dashboard SQL editor (manual fallback)

1. Open [supabase.com/dashboard](https://supabase.com/dashboard) → select your project.
2. Navigate to **SQL Editor** → **New query**.
3. Paste the full contents of `supabase/migrations/001_initial_schema.sql`.
4. Click **Run**.

---

## Verify

After applying, confirm in the Supabase Dashboard:

- **Table Editor** → you should see all 6 tables:
  `users`, `user_progress`, `leaderboard`, `daily_challenges`, `friendships`, `fcm_tokens`
- **Authentication → Policies** → each table lists its RLS policies.
- **Database → Replication** → `leaderboard` appears under `supabase_realtime`.

---

## Environment Variables for Edge Functions

When deploying Supabase Edge Functions (Phase 3+), set these secrets via the CLI:

```bash
supabase secrets set OPENAI_API_KEY=sk-...
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=eyJ...
```

For local development, add them to `supabase/.env` (gitignored).

---

## Important Notes

- **leaderboard** and **daily_challenges** are **write-protected for clients** — INSERT/UPDATE
  policies are scoped to `service_role`. Client-side writes will be rejected by RLS. Use
  Edge Functions or `pg_cron` for server-side writes (Phase 3–4).
- **Never apply migrations to production** without reviewing RLS policies first.
- The `auth.uid()` function in policies is populated by Supabase from the verified JWT;
  clients cannot forge it.
