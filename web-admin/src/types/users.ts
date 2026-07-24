/**
 * AdminUser — combines public.users + public.user_progress columns
 * Used by admin Server Actions and the /admin/users page.
 */
export interface AdminUser {
  id: string
  username: string
  /** Not available via public schema join — always null */
  email: string | null
  avatar_url: string | null
  is_banned: boolean
  is_admin: boolean
  created_at: string
  /** From user_progress.elo_score — null if no progress row yet */
  elo_score: number | null
  /** From user_progress.streak_count — null if no progress row yet */
  streak_count: number | null
  /** From user_progress.total_games — null if no progress row yet */
  total_games: number | null
  /** From user_progress.last_practice_date — null if never practiced */
  last_practice_date: string | null
}

export type BanStatus = 'active' | 'banned'
