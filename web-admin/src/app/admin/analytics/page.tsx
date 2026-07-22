import { getDauLast30Days, getTopGameModes, getSummaryStats } from '@/lib/actions/analytics'
import { DauChart, GameModesChart } from './charts'

export default async function AnalyticsPage() {
  const [dauData, gameModes, summary] = await Promise.all([
    getDauLast30Days(),
    getTopGameModes(),
    getSummaryStats(),
  ])

  const statCards = [
    { label: 'Total Users', value: summary.totalUsers.toLocaleString() },
    { label: 'Active Today', value: summary.activeToday.toLocaleString() },
    { label: 'Challenges Completed', value: summary.totalChallengesCompleted.toLocaleString() },
    { label: 'Avg Streak Days', value: summary.avgStreakDays.toLocaleString() },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Analytics</h1>

      {/* Summary stat cards */}
      <div className="grid grid-cols-2 gap-4 mb-8 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
            <p className="text-sm text-gray-500 mb-1">{card.label}</p>
            <p className="text-3xl font-bold text-gray-900">{card.value}</p>
          </div>
        ))}
      </div>

      {/* DAU chart */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm mb-8">
        <h2 className="text-lg font-semibold mb-4">Daily Active Users (Last 30 Days)</h2>
        <DauChart data={dauData} />
      </div>

      {/* Game modes chart */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
        <h2 className="text-lg font-semibold mb-4">Top Game Modes</h2>
        <GameModesChart data={gameModes} />
      </div>
    </div>
  )
}
