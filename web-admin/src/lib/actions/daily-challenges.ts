'use server'

import { createAdminClient } from '@/lib/supabase/admin'
import { revalidatePath } from 'next/cache'

export type AdminChallenge = {
  id: number
  challengeDate: string
  wordIds: number[]
  bonusElo: number
  completionCount: number
}

/**
 * Fetch challenges for the window of last 7 days → next 7 days.
 */
export async function getUpcomingChallenges(): Promise<AdminChallenge[]> {
  const admin = createAdminClient()

  const past = new Date()
  past.setDate(past.getDate() - 7)
  const future = new Date()
  future.setDate(future.getDate() + 7)

  const { data, error } = await admin
    .from('daily_challenges')
    .select('id, challenge_date, word_ids, bonus_elo')
    .gte('challenge_date', past.toISOString().slice(0, 10))
    .lte('challenge_date', future.toISOString().slice(0, 10))
    .order('challenge_date', { ascending: true })

  if (error) {
    console.error('[daily-challenges] getUpcomingChallenges error:', error.message)
    return []
  }

  // Fetch completion counts per challenge date
  const dates = (data ?? []).map(
    (r: { challenge_date: string }) => r.challenge_date as string
  )

  let completionMap = new Map<string, number>()
  if (dates.length > 0) {
    const { data: completions, error: ce } = await admin
      .from('daily_completions')
      .select('challenge_date')
      .in('challenge_date', dates)

    if (!ce && completions) {
      for (const row of completions) {
        const d = row.challenge_date as string
        completionMap.set(d, (completionMap.get(d) ?? 0) + 1)
      }
    }
  }

  return (data ?? []).map(
    (r: {
      id: number
      challenge_date: string
      word_ids: number[]
      bonus_elo: number
    }) => ({
      id: r.id,
      challengeDate: r.challenge_date,
      wordIds: Array.isArray(r.word_ids) ? r.word_ids : [],
      bonusElo: r.bonus_elo ?? 50,
      completionCount: completionMap.get(r.challenge_date) ?? 0,
    })
  )
}

/**
 * Create or update a daily challenge (upsert by challenge_date).
 */
export async function upsertChallenge(formData: FormData): Promise<{ error?: string }> {
  const admin = createAdminClient()

  const date = (formData.get('date') as string | null)?.trim()
  if (!date) return { error: 'Date is required' }

  const rawIds = (formData.get('wordIds') as string | null)?.trim() ?? ''
  const wordIds = rawIds
    .split(',')
    .map((s) => parseInt(s.trim(), 10))
    .filter((n) => !isNaN(n))

  if (wordIds.length === 0) return { error: 'At least one word ID is required' }

  const bonusEloRaw = formData.get('bonusElo')
  const bonusElo = bonusEloRaw ? parseInt(bonusEloRaw as string, 10) : 50
  const safeBonusElo = isNaN(bonusElo) ? 50 : bonusElo

  const { error } = await admin.from('daily_challenges').upsert(
    {
      challenge_date: date,
      word_ids: wordIds,
      bonus_elo: safeBonusElo,
    },
    { onConflict: 'challenge_date' }
  )

  if (error) {
    console.error('[daily-challenges] upsertChallenge error:', error.message)
    return { error: error.message }
  }

  revalidatePath('/admin/daily-challenges')
  return {}
}

/**
 * Delete a daily challenge by id.
 */
export async function deleteChallenge(id: number): Promise<{ error?: string }> {
  const admin = createAdminClient()

  const { error } = await admin.from('daily_challenges').delete().eq('id', id)

  if (error) {
    console.error('[daily-challenges] deleteChallenge error:', error.message)
    return { error: error.message }
  }

  revalidatePath('/admin/daily-challenges')
  return {}
}
