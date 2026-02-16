import { useEffect, useState } from 'react'
import { useAuthStore } from '../../shared/auth/authStore'
import { apiRequest } from '../../shared/api/apiClient'
import { loadMe } from './authApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

export function AuthGate({ children }: { children: React.ReactNode }) {
  const bootstrapped = useAuthStore((s) => s.bootstrapped)
  const markBootstrapped = useAuthStore((s) => s.markBootstrapped)
  const tokens = useAuthStore((s) => s.tokens)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      try {
        // If we have tokens, validate them by loading /users/me (apiRequest will refresh on 401 once).
        if (tokens?.accessToken) {
          await loadMe()
        } else if (tokens?.refreshToken) {
          // No access token (shouldn't happen with our storage), but attempt a refresh-driven request
          await apiRequest('/v1/users/me')
          await loadMe()
        }
      } catch (e: unknown) {
        if (!cancelled) {
          // best-effort: clear auth if bootstrapping failed
          useAuthStore.getState().logout()
          setError(getErrorMessage(e) || 'Auth bootstrap failed')
        }
      } finally {
        if (!cancelled) markBootstrapped()
      }
    })()

    return () => {
      cancelled = true
    }
  }, [markBootstrapped, tokens?.accessToken, tokens?.refreshToken])

  if (!bootstrapped) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-50 flex items-center justify-center">
        <div className="rounded-lg border border-slate-800 bg-slate-900 px-6 py-5">
          <div className="text-sm text-slate-300">Loading…</div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-50 flex items-center justify-center px-4">
        <div className="max-w-md rounded-lg border border-slate-800 bg-slate-900 px-6 py-5">
          <div className="text-base font-semibold">Could not bootstrap session</div>
          <div className="mt-2 text-sm text-slate-300">{error}</div>
          <a
            href="/login"
            className="mt-4 inline-block text-sm text-sky-400 hover:text-sky-300"
          >
            Sign in again
          </a>
        </div>
      </div>
    )
  }

  return <>{children}</>
}

