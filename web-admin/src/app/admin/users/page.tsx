import { listUsers, banUser, unbanUser } from '@/lib/actions/users'

interface PageProps {
  searchParams: Promise<{ search?: string; page?: string }>
}

export default async function UsersPage({ searchParams }: PageProps) {
  const params = await searchParams
  const search = params.search ?? ''
  const page = Math.max(1, parseInt(params.page ?? '1', 10))
  const pageSize = 50

  const { users, total } = await listUsers(search, page, pageSize)
  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Users</h1>
        <span className="text-sm text-gray-500">{total} total users</span>
      </div>

      {/* Search */}
      <form method="GET" action="/admin/users" className="flex gap-2 mb-6">
        <input
          type="text"
          name="search"
          defaultValue={search}
          placeholder="Search by username..."
          className="flex-1 px-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <input type="hidden" name="page" value="1" />
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 transition-colors"
        >
          Search
        </button>
        {search && (
          <a
            href="/admin/users"
            className="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded hover:bg-gray-200 transition-colors"
          >
            Clear
          </a>
        )}
      </form>

      {/* Table or empty state */}
      {users.length === 0 ? (
        <div className="text-center py-16 text-gray-500">No users found.</div>
      ) : (
        <div className="overflow-x-auto rounded border border-gray-200">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left">
              <tr>
                <th className="px-4 py-3 font-semibold text-gray-700">Username</th>
                <th className="px-4 py-3 font-semibold text-gray-700">ELO ⚡</th>
                <th className="px-4 py-3 font-semibold text-gray-700">Streak 🔥</th>
                <th className="px-4 py-3 font-semibold text-gray-700">Games 🎮</th>
                <th className="px-4 py-3 font-semibold text-gray-700">Last Active</th>
                <th className="px-4 py-3 font-semibold text-gray-700">Status</th>
                <th className="px-4 py-3 font-semibold text-gray-700">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.map((user) => {
                const lastActive = user.last_practice_date
                  ? new Date(user.last_practice_date).toLocaleDateString('en-US', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })
                  : '—'

                return (
                  <tr
                    key={user.id}
                    className={user.is_banned ? 'bg-red-50' : 'bg-white'}
                  >
                    {/* Username */}
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {user.username}
                    </td>

                    {/* ELO */}
                    <td className="px-4 py-3 text-gray-700">
                      {user.elo_score ?? '—'}
                    </td>

                    {/* Streak */}
                    <td className="px-4 py-3 text-gray-700">
                      {user.streak_count != null && user.streak_count > 0
                        ? `🔥 ${user.streak_count}`
                        : user.streak_count ?? '—'}
                    </td>

                    {/* Games */}
                    <td className="px-4 py-3 text-gray-700">
                      {user.total_games ?? '—'}
                    </td>

                    {/* Last Active */}
                    <td className="px-4 py-3 text-gray-500">{lastActive}</td>

                    {/* Status badge */}
                    <td className="px-4 py-3">
                      {user.is_banned ? (
                        <span className="inline-block bg-red-100 text-red-700 px-2 py-0.5 rounded-full text-xs font-medium">
                          Banned
                        </span>
                      ) : (
                        <span className="inline-block bg-green-100 text-green-700 px-2 py-0.5 rounded-full text-xs font-medium">
                          Active
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="px-4 py-3">
                      {user.is_admin ? (
                        <span className="text-xs text-gray-400 italic">(admin)</span>
                      ) : user.is_banned ? (
                        <form action={unbanUser.bind(null, user.id)}>
                          <button
                            type="submit"
                            className="bg-green-600 text-white text-xs px-3 py-1 rounded hover:bg-green-700 transition-colors"
                          >
                            Unban
                          </button>
                        </form>
                      ) : (
                        <form action={banUser.bind(null, user.id)}>
                          <button
                            type="submit"
                            className="bg-red-600 text-white text-xs px-3 py-1 rounded hover:bg-red-700 transition-colors"
                          >
                            Ban
                          </button>
                        </form>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 text-sm text-gray-600">
          <span>
            Page {page} of {totalPages} — {total} users
          </span>
          <div className="flex gap-2">
            {page > 1 ? (
              <a
                href={`/admin/users?search=${encodeURIComponent(search)}&page=${page - 1}`}
                className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
              >
                ← Prev
              </a>
            ) : (
              <span className="px-3 py-1 border border-gray-200 rounded text-gray-300 cursor-not-allowed">
                ← Prev
              </span>
            )}
            {page < totalPages ? (
              <a
                href={`/admin/users?search=${encodeURIComponent(search)}&page=${page + 1}`}
                className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
              >
                Next →
              </a>
            ) : (
              <span className="px-3 py-1 border border-gray-200 rounded text-gray-300 cursor-not-allowed">
                Next →
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
