import { getUpcomingChallenges, upsertChallenge, deleteChallenge } from '@/lib/actions/daily-challenges'
import { createAdminClient } from '@/lib/supabase/admin'

async function upsertChallengeAction(formData: FormData): Promise<void> {
  'use server'
  await upsertChallenge(formData)
}

interface Word {
  id: number
  word: string
}

async function getWords(): Promise<Word[]> {
  const admin = createAdminClient()
  const { data, error } = await admin
    .from('words')
    .select('id, word')
    .order('word', { ascending: true })
    .limit(200)

  if (error) {
    console.error('[daily-challenges] getWords error:', error.message)
    return []
  }
  return (data ?? []) as Word[]
}

export default async function DailyChallengesPage() {
  const [challenges, words] = await Promise.all([getUpcomingChallenges(), getWords()])

  const today = new Date().toISOString().slice(0, 10)

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Daily Challenges</h1>

      {/* Challenges table */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm mb-8 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold">Lịch thách đấu (−7 → +7 ngày)</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Ngày</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Số từ</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Bonus ELO</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Lượt hoàn thành</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {challenges.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    Chưa có thách đấu nào trong khoảng thời gian này.
                  </td>
                </tr>
              ) : (
                challenges.map((c) => (
                  <tr key={c.id} className={c.challengeDate < today ? 'bg-gray-50/50' : ''}>
                    <td className="px-4 py-3 font-mono">
                      {c.challengeDate}
                      {c.challengeDate === today && (
                        <span className="ml-2 text-xs bg-red-100 text-red-700 rounded px-1.5 py-0.5">
                          Hôm nay
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">{c.wordIds.length}</td>
                    <td className="px-4 py-3">+{c.bonusElo}</td>
                    <td className="px-4 py-3">{c.completionCount}</td>
                    <td className="px-4 py-3">
                      <form
                        action={async () => {
                          'use server'
                          await deleteChallenge(c.id)
                        }}
                        className="inline"
                      >
                        <button
                          type="submit"
                          className="text-red-600 hover:text-red-800 text-xs underline"
                          onClick={undefined}
                        >
                          Xóa
                        </button>
                      </form>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create / Update form */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <h2 className="text-lg font-semibold mb-4">Tạo / Cập nhật thách đấu</h2>
        <form action={upsertChallengeAction} className="space-y-4 max-w-lg">
          {/* Date */}
          <div>
            <label htmlFor="date" className="block text-sm font-medium text-gray-700 mb-1">
              Ngày <span className="text-red-500">*</span>
            </label>
            <input
              id="date"
              name="date"
              type="date"
              defaultValue={today}
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          {/* Word IDs */}
          <div>
            <label htmlFor="wordIds" className="block text-sm font-medium text-gray-700 mb-1">
              Word IDs (phân tách bằng dấu phẩy) <span className="text-red-500">*</span>
            </label>
            <textarea
              id="wordIds"
              name="wordIds"
              rows={3}
              placeholder="Ví dụ: 1, 5, 12, 34"
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-red-500"
            />
            {words.length > 0 && (
              <p className="mt-1 text-xs text-gray-500">
                Từ điển có {words.length} từ. Một số từ:{' '}
                {words
                  .slice(0, 5)
                  .map((w) => `${w.id}:${w.word}`)
                  .join(', ')}
                …
              </p>
            )}
          </div>

          {/* Bonus ELO */}
          <div>
            <label htmlFor="bonusElo" className="block text-sm font-medium text-gray-700 mb-1">
              Bonus ELO
            </label>
            <input
              id="bonusElo"
              name="bonusElo"
              type="number"
              defaultValue={50}
              min={0}
              max={500}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <button
            type="submit"
            className="bg-[#DA251D] hover:bg-red-700 text-white text-sm font-medium py-2 px-6 rounded-md transition-colors"
          >
            Lưu thách đấu
          </button>
        </form>
      </div>
    </div>
  )
}
