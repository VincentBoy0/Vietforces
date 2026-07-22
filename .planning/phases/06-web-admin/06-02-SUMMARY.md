---
phase: 06-web-admin
plan: "02"
subsystem: web-admin
tags: [vocabulary, crud, server-actions, supabase-storage, next-js, typescript]
dependency_graph:
  requires:
    - 06-01  # web-admin scaffold with auth and admin layout
  provides:
    - vocabulary-list-page
    - vocabulary-create-page
    - vocabulary-edit-page
    - vocabulary-server-actions
    - vocabulary-types
  affects:
    - web-admin/src/types/vocabulary.ts
    - web-admin/src/lib/actions/vocabulary.ts
    - web-admin/src/app/admin/vocabulary/page.tsx
    - web-admin/src/app/admin/vocabulary/new/page.tsx
    - web-admin/src/app/admin/vocabulary/[id]/edit/page.tsx
tech_stack:
  added:
    - Next.js Server Actions for all CRUD mutations
    - Supabase Storage via service-role client (vocabulary-images bucket)
    - next/image for vocabulary image rendering
  patterns:
    - Server Components for all read operations
    - Server Actions (bind pattern) for mutations with route redirect
    - FormData extraction for multipart/form-data uploads
    - createAdminClient() for all DB and Storage mutations
key_files:
  created:
    - web-admin/src/types/vocabulary.ts
    - web-admin/src/lib/actions/vocabulary.ts
    - web-admin/src/app/admin/vocabulary/page.tsx
    - web-admin/src/app/admin/vocabulary/new/page.tsx
    - web-admin/src/app/admin/vocabulary/[id]/edit/page.tsx
  modified: []
decisions:
  - "Used createAdminClient() (service role) for all Supabase operations — vocab images are public assets, no row-level security needed in admin context"
  - "updateWord uses .bind(null, wordId) pattern to pass numeric ID as first arg to Server Action"
  - "Delete confirmation kept as simple button (no JS confirm dialog) to avoid client component complexity on the edit page"
  - "Category input is free text (not constrained select) to allow admins to define new categories without code changes"
metrics:
  duration: "~2 minutes"
  completed: "2026-07-23T03:15:37+07:00"
  tasks_completed: 3
  files_created: 5
  files_modified: 0
---

# Phase 6 Plan 02: Vocabulary CRUD Pages Summary

## One-liner

Full vocabulary CRUD admin interface with Supabase Storage image upload via Next.js Server Actions, covering list/filter/paginate, create, edit, and delete.

## What Was Built

### Task 1: Vocabulary Types + Server Actions

**`web-admin/src/types/vocabulary.ts`**
- `Word` interface matching the `words` table schema (`id`, `word`, `classifier`, `category`, `image_url`, `distractors`, `created_at`)
- `WordFormData` interface for form submission data
- `CategoryFilter` type alias

**`web-admin/src/lib/actions/vocabulary.ts`**
- `'use server'` directive; uses `createAdminClient()` exclusively
- `listWords(category, page, pageSize)` — paginated SELECT with optional category filter
- `listCategories()` — distinct categories from DB
- `createWord(formData)` — extracts FormData fields, uploads image to `vocabulary-images` bucket if provided, inserts row
- `updateWord(id, formData)` — updates row, optionally replaces image; reads `existing_image_url` hidden field to preserve current image when no new file provided
- `deleteWord(id)` — removes storage file (path extracted from URL), then deletes DB row
- All mutations call `revalidatePath('/admin/vocabulary')`

### Task 2: Vocabulary List Page

**`web-admin/src/app/admin/vocabulary/page.tsx`**
- Server Component; `searchParams` typed as `Promise<{...}>` (Next.js 15)
- Parallel data fetch: `listWords` + `listCategories` via `Promise.all`
- Category filter via GET form (preserves browser navigation/bookmarking)
- Table with columns: Image (60×60 next/image), Word, Classifier, Category badge, Distractors (truncated), Edit link
- Pagination links with `pointer-events-none opacity-50` at bounds
- Empty state message when no results

### Task 3: New + Edit Form Pages

**`web-admin/src/app/admin/vocabulary/new/page.tsx`**
- Server Component; inline `createAndRedirect` server action wraps `createWord` + `redirect`
- Form fields: word (required), classifier, category (required), image file, distractors textarea
- `encType="multipart/form-data"` for file upload

**`web-admin/src/app/admin/vocabulary/[id]/edit/page.tsx`**
- Server Component; `params` typed as `Promise<{ id: string }>` (Next.js 15)
- Fetches word by ID using `createAdminClient()` directly; calls `notFound()` for missing/invalid IDs
- Edit form pre-populated with `defaultValue` on all fields
- Hidden `existing_image_url` field preserves current image when no new file selected
- Shows current image preview (next/image) when `word.image_url` exists
- Danger Zone section with delete form using `deleteAndRedirect` bound action

## Deviations from Plan

None — plan executed exactly as written.

## Threat Mitigations Applied

| Threat | Mitigation |
|--------|-----------|
| T-06-02-01 (Tampering via FormData) | Server Actions run server-side; Supabase JS uses parameterized queries (no SQL injection) |
| T-06-02-02 (Image URL disclosure) | Filenames sanitized (`replace(/[^a-zA-Z0-9._-]/g, '_')`); timestamp prefix prevents guessing |
| T-06-02-04 (Malformed URL in deleteWord) | Path extraction guards with `.split('/vocabulary-images/')[1]`; skips storage delete if path is falsy |

## Self-Check

- [x] `web-admin/src/types/vocabulary.ts` — created
- [x] `web-admin/src/lib/actions/vocabulary.ts` — created, `'use server'` on line 1
- [x] `web-admin/src/app/admin/vocabulary/page.tsx` — created
- [x] `web-admin/src/app/admin/vocabulary/new/page.tsx` — created
- [x] `web-admin/src/app/admin/vocabulary/[id]/edit/page.tsx` — created
- [x] `npx tsc --noEmit` — exit code 0 (no errors)
- [x] Commit `24aa5a6` — confirmed in git log
