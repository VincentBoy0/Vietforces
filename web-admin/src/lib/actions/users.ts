'use server'

import { createAdminClient } from '@/lib/supabase/admin'
import { revalidatePath } from 'next/cache'
import type { AdminUser } from '@/types/users'

/**
 * Fetch all users joined with user_progress.
 * Uses the service-role admin client so RLS is bypassed.
 */
export async function listUsers(
  search: string = '',
  page: number = 1,
  pageSize: number = 50
): Promise<{ users: AdminUser[]; total: number }> {
  const admin = createAdminClient()

  const { data, error, count } = await admin
    .from('users')
    .select(
      `
      id,
      username,
      avatar_url,
      is_banned,
      is_admin,
      created_at,
      user_progress (
        elo_score,
        streak_count,
        total_games,
        last_practice_date
      )
    `,
      { count: 'exact' }
    )
    .ilike('username', search ? `%${search}%` : '%')
    .order('created_at', { ascending: false })
    .range((page - 1) * pageSize, page * pageSize - 1)

  if (error) throw new Error('Failed to fetch users: ' + error.message)

  const users: AdminUser[] = (data ?? []).map((u) => {
    // Supabase may return a nested one-to-one relation as object or single-element array
    const progress = Array.isArray(u.user_progress)
      ? u.user_progress[0]
      : u.user_progress

    return {
      id: u.id as string,
      username: u.username as string,
      email: null,
      avatar_url: (u.avatar_url as string | null) ?? null,
      is_banned: (u.is_banned as boolean) ?? false,
      is_admin: (u.is_admin as boolean) ?? false,
      created_at: u.created_at as string,
      elo_score: (progress?.elo_score as number | null | undefined) ?? null,
      streak_count: (progress?.streak_count as number | null | undefined) ?? null,
      total_games: (progress?.total_games as number | null | undefined) ?? null,
      last_practice_date: (progress?.last_practice_date as string | null | undefined) ?? null,
    }
  })

  return { users, total: count ?? 0 }
}

/**
 * Set is_banned = true for the given user ID.
 * Revalidates /admin/users so the page reflects the change immediately.
 */
export async function banUser(userId: string): Promise<void> {
  const admin = createAdminClient()
  const { error } = await admin
    .from('users')
    .update({ is_banned: true })
    .eq('id', userId)

  if (error) throw new Error('Failed to ban user: ' + error.message)
  revalidatePath('/admin/users')
}

/**
 * Set is_banned = false for the given user ID.
 * Revalidates /admin/users so the page reflects the change immediately.
 */
export async function unbanUser(userId: string): Promise<void> {
  const admin = createAdminClient()
  const { error } = await admin
    .from('users')
    .update({ is_banned: false })
    .eq('id', userId)

  if (error) throw new Error('Failed to unban user: ' + error.message)
  revalidatePath('/admin/users')
}
