'use client'

import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'

export type DauDataPoint = { date: string; count: number }
export type GameModeStat = { mode: string; count: number }

export function DauChart({ data }: { data: DauDataPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height={400}>
      <LineChart data={data} margin={{ top: 8, right: 24, left: 0, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis
          dataKey="date"
          tick={{ fontSize: 11 }}
          tickFormatter={(v: string) => v.slice(5)} // MM-DD
        />
        <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
        <Tooltip
          formatter={(value: number) => [value, 'Active Users']}
          labelFormatter={(label: string) => `Date: ${label}`}
        />
        <Line
          type="monotone"
          dataKey="count"
          stroke="#DA251D"
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, fill: '#DA251D' }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

export function GameModesChart({ data }: { data: GameModeStat[] }) {
  return (
    <ResponsiveContainer width="100%" height={400}>
      <BarChart data={data} margin={{ top: 8, right: 24, left: 0, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis dataKey="mode" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
        <Tooltip formatter={(value: number) => [value, 'Sessions']} />
        <Bar dataKey="count" fill="#FFCD00" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}
