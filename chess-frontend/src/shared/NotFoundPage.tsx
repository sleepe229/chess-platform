import { Link } from 'react-router-dom'
import { Button } from './ui/Button'
import { useAuthStore } from './auth/authStore'

export function NotFoundPage() {
  const hasAuth = Boolean(useAuthStore((s) => s.tokens?.accessToken))

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center px-4">
      <h1 className="text-2xl font-semibold text-slate-50">Page not found</h1>
      <p className="mt-2 text-slate-400">The page you're looking for doesn't exist or has been moved.</p>
      <div className="mt-6 flex flex-wrap gap-3 justify-center">
        {hasAuth ? (
          <Link to="/lobby">
            <Button type="button">Go to Lobby</Button>
          </Link>
        ) : (
          <Link to="/login">
            <Button type="button">Sign in</Button>
          </Link>
        )}
        <Link to="/">
          <Button type="button" variant="secondary">Home</Button>
        </Link>
      </div>
    </div>
  )
}
