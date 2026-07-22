import Link from 'next/link'
import { createClient } from '@/lib/supabase/server'
import { signOutAction } from '@/app/login/actions'

export default async function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const supabase = await createClient()
  const {
    data: { user },
  } = await supabase.auth.getUser()

  return (
    <div className="flex h-screen">
      {/* Sidebar */}
      <aside className="w-64 bg-foreground text-white flex flex-col flex-shrink-0">
        <div className="p-6 border-b border-white/10">
          <Link
            href="/admin/vocabulary"
            className="text-xl font-bold hover:opacity-80 transition-opacity"
          >
            VietForces Admin
          </Link>
        </div>

        <nav className="flex-1 p-4 space-y-1">
          <Link
            href="/admin/vocabulary"
            className="block px-4 py-2 rounded hover:bg-white/10 transition-colors text-sm"
          >
            Vocabulary
          </Link>
          <Link
            href="/admin/users"
            className="block px-4 py-2 rounded hover:bg-white/10 transition-colors text-sm"
          >
            Users
          </Link>
          <Link
            href="/admin/analytics"
            className="block px-4 py-2 rounded hover:bg-white/10 transition-colors text-sm"
          >
            Analytics
          </Link>
          <Link
            href="/admin/daily-challenges"
            className="block px-4 py-2 rounded hover:bg-white/10 transition-colors text-sm"
          >
            Daily Challenges
          </Link>
        </nav>

        <div className="mt-auto p-4 border-t border-white/10">
          <p className="text-xs text-white/60 mb-3 truncate">{user?.email}</p>
          <form action={signOutAction}>
            <button
              type="submit"
              className="w-full bg-danger hover:bg-danger-hover text-white text-sm py-2 px-4 rounded transition-colors"
            >
              Sign Out
            </button>
          </form>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-8">{children}</main>
    </div>
  )
}
