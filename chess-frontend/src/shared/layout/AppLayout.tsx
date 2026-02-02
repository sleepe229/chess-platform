import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import clsx from 'clsx'
import { useAuthStore } from '../auth/authStore'
import { logout } from '../../features/auth/authApi'
import { Button } from '../ui/Button'

export function AppLayout() {
  const tokens = useAuthStore((s) => s.tokens)
  const me = useAuthStore((s) => s.me)
  const nav = useNavigate()

  async function onLogout() {
    await logout()
    nav('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-50">
      <header className="h-14 border-b border-slate-800 bg-slate-950/70 backdrop-blur">
        <div className="mx-auto max-w-6xl h-full px-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="font-semibold tracking-tight">Chess Platform</div>

            {tokens?.accessToken ? (
              <nav className="hidden sm:flex items-center gap-2 text-sm text-slate-300">
                <TopLink to="/lobby">Lobby</TopLink>
                <TopLink to="/profile">Profile</TopLink>
                <TopLink to="/games">Games</TopLink>
              </nav>
            ) : null}
          </div>

          {tokens?.accessToken ? (
            <div className="flex items-center gap-3">
              <div className="hidden sm:block text-xs text-slate-300">
                <div className="font-medium text-slate-200">{me?.username || '—'}</div>
                <div className="text-slate-400">{me?.email || ''}</div>
              </div>
              <Button variant="secondary" onClick={onLogout}>
                Logout
              </Button>
            </div>
          ) : null}
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}

function TopLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        clsx('px-2 py-1 rounded hover:text-slate-50', isActive ? 'text-slate-50 bg-slate-900' : 'text-slate-300')
      }
    >
      {children}
    </NavLink>
  )
}

