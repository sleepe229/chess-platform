import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import clsx from 'clsx'
import { useAuthStore } from '../auth/authStore'
import { logout } from '../../features/auth/authApi'
import { Button } from '../ui/Button'

const TITLES: Record<string, string> = {
  '/': 'Chess Platform',
  '/lobby': 'Lobby · Chess Platform',
  '/profile': 'Profile · Chess Platform',
  '/games': 'History · Chess Platform',
  '/login': 'Sign in · Chess Platform',
  '/register': 'Create account · Chess Platform',
}

export function AppLayout() {
  const tokens = useAuthStore((s) => s.tokens)
  const me = useAuthStore((s) => s.me)
  const nav = useNavigate()
  const location = useLocation()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  useEffect(() => {
    const path = location.pathname
    let title = TITLES[path]
    if (!title) {
      if (/^\/game\/[^/]+$/.test(path)) title = 'Game · Chess Platform'
      else if (/^\/games\/[^/]+$/.test(path)) title = 'Game · Chess Platform'
      else title = 'Chess Platform'
    }
    document.title = title
  }, [location.pathname])

  async function onLogout() {
    await logout()
    nav('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-50">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-2 focus:py-1 focus:rounded focus:ring-2 focus:ring-sky-500 bg-slate-900 text-slate-50">
        Skip to main content
      </a>
      <header className="h-14 border-b border-slate-800 bg-slate-950/70 backdrop-blur">
        <div className="mx-auto max-w-6xl h-full px-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to={tokens?.accessToken ? '/lobby' : '/'} className="font-semibold tracking-tight hover:text-slate-200">
              Chess Platform
            </Link>

            {tokens?.accessToken ? (
              <>
                <nav className="hidden sm:flex items-center gap-2 text-sm text-slate-300">
                  <TopLink to="/lobby">Lobby</TopLink>
                  <TopLink to="/profile">Profile</TopLink>
                  <TopLink to="/games">History</TopLink>
                </nav>
                <button
                  type="button"
                  className="sm:hidden p-2 rounded text-slate-300 hover:text-slate-50 hover:bg-slate-800 aria-expanded={mobileMenuOpen}"
                  onClick={() => setMobileMenuOpen((o) => !o)}
                  aria-label="Toggle menu"
                >
                  <span className="sr-only">{mobileMenuOpen ? 'Close' : 'Open'} menu</span>
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden>
                    {mobileMenuOpen ? (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    ) : (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                    )}
                  </svg>
                </button>
              </>
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
        {tokens?.accessToken && mobileMenuOpen && (
          <div className="sm:hidden border-t border-slate-800 bg-slate-950/95 px-4 py-3 flex flex-col gap-1">
            <TopLink to="/lobby" onClick={() => setMobileMenuOpen(false)}>Lobby</TopLink>
            <TopLink to="/profile" onClick={() => setMobileMenuOpen(false)}>Profile</TopLink>
            <TopLink to="/games" onClick={() => setMobileMenuOpen(false)}>History</TopLink>
          </div>
        )}
      </header>

      <main id="main" className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}

function TopLink({ to, children, onClick }: { to: string; children: React.ReactNode; onClick?: () => void }) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) =>
        clsx('block px-2 py-1 rounded hover:text-slate-50', isActive ? 'text-slate-50 bg-slate-900' : 'text-slate-300')
      }
    >
      {children}
    </NavLink>
  )
}

