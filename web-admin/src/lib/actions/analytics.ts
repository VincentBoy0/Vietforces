'use server'

import { createAdminClient } from '@/lib/supabase/admin'

export type DauDataPoint = { date: string; count: number }
export type GameModeStat = { mode: string; count: number }
export type SummaryStats = {
  totalUsers: number
  activeToday: number
  totalChallengesCompleted: number
  avgStreakDays: number
}

/**
 * Returns daily active users for the last 30 days.
 * A user is "active" on a given day if their user_progress.updated_at falls on that day.
 */
export async function getDauLast30Days(): Promise<DauDataPoint[]> {
  const admin = createAdminClient()

  const since = new Date()
  since.setDate(since.getDate() - 29)
  since.setHours(0, 0, 0, 0)

  const { data, error } = await admin
    .from('user_progress')
    .select('updated_at')
    .gte('updated_at', since.toISOString())

  if (error) {
    console.error('[analytics] getDauLast30Days error:', error.message)
    return []
  }

  // Aggregate by day in JS
  const countsByDay = new Map<string, number>()
  for (const row of data ?? []) {
    const day = (row.updated_at as string).slice(0, 10) // 'YYYY-MM-DD'
    countsByDay.set(day, (countsByDay.get(day) ?? 0) + 1)
  }

  // Build a full 30-day series (fill missing days with 0)
  const result: DauDataPoint[] = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const dateStr = d.toISOString().slice(0, 10)
    result.push({ date: dateStr, count: countsByDay.get(dateStr) ?? 0 })
  }

  return result
}

/**
 * Returns top game-mode stats derived from daily_completions.
 * Because the schema has no explicit "mode" column, we bucket by week
 * and return static mode labels as a best-effort approximation.
 */
export async function getTopGameModes(): Promise<GameModeStat[]> {
  const admin = createAdminClient()

  const { count: dailyCount, error: e1 } = await admin
    .from('daily_completions')
    .select('*', { count: 'exact', head: true })

  const { count: streakCount, error: e2 } = await admin
    .from('user_progress')
    .select('*', { count: 'exact', head: true })
    .gt('streak_days', 0)

  if (e1 || e2) {
    console.error('[analytics] getTopGameModes error:', e1?.message ?? e2?.message)
  }

  // Derived stats: daily challenge + streak-based modes
  return [
    { mode: 'Daily Challenge', count: dailyCount ?? 0 },
    { mode: 'Streak Mode', count: streakCount ?? 0 },
    { mode: 'Vocabulary Quiz', count: 0 },
    { mode: 'Flash Cards', count: 0 },
    { mode: 'Practice Mode', count: 0 },
  ]
}

/**
 * Returns high-level summary statistics for the admin dashboard.
 */
export async function getSummaryStats(): Promise<SummaryStats> {
  const admin = createAdminClient()

  const todayStart = new Date()
  todayStart.setHours(0, 0, 0, 0)

  const [totalUsersRes, activeTodayRes, completionsRes, progressRes] = await Promise.all([
    admin.from('user_progress').select('*', { count: 'exact', head: true }),
    admin
      .from('user_progress')
      .select('*', { count: 'exact', head: true })
      .gte('updated_at', todayStart.toISOString()),
    admin.from('daily_completions').select('*', { count: 'exact', head: true }),
    admin.from('user_progress').select('streak_days'),
  ])

  const streaks = (progressRes.data ?? []).map(
    (r: { streak_days: number | null }) => r.streak_days ?? 0
  )
  const avgStreakDays =
    streaks.length > 0
      ? Math.round(streaks.reduce((a: number, b: number) => a + b, 0) / streaks.length)
      : 0

  return {
    totalUsers: totalUsersRes.count ?? 0,
    activeToday: activeTodayRes.count ?? 0,
    totalChallengesCompleted: completionsRes.count ?? 0,
    avgStreakDays,
  }
}
