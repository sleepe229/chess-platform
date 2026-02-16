import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../shared/auth/authStore'
import { loadMe } from './authApi'

type OAuthCallbackPayload = {
  error: string | null
  accessToken: string | null
  refreshToken: string | null
  expiresInSeconds: number
}

function parseOAuthCallbackHash(hash: string): OAuthCallbackPayload {
  const normalizedHash = hash.startsWith('#') ? hash.slice(1) : hash
  const params = new URLSearchParams(normalizedHash)
  const error = params.get('error')

  if (error) {
    return {
      error,
      accessToken: null,
      refreshToken: null,
      expiresInSeconds: 0,
    }
  }

  const accessToken = params.get('access_token')
  const refreshToken = params.get('refresh_token')
  if (!accessToken || !refreshToken) {
    return {
      error: 'missing_tokens',
      accessToken: null,
      refreshToken: null,
      expiresInSeconds: 0,
    }
  }

  const expiresIn = parseInt(params.get('expires_in') || '0', 10) || 900
  return {
    error: null,
    accessToken,
    refreshToken,
    expiresInSeconds: expiresIn,
  }
}

/**
 * OAuth2 callback: backend redirects here with tokens in hash (#access_token=...&refresh_token=...&expires_in=...).
 * We save tokens, load profile, then redirect to lobby.
 */
export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const callback = useMemo(() => parseOAuthCallbackHash(window.location.hash || ''), [])
  const [loadMeError, setLoadMeError] = useState<string | null>(null)
  const error = callback.error ?? loadMeError

  useEffect(() => {
    if (callback.error || !callback.accessToken || !callback.refreshToken) {
      return
    }

    useAuthStore.getState().setTokens({
      accessToken: callback.accessToken,
      refreshToken: callback.refreshToken,
      expiresAtMs: Date.now() + callback.expiresInSeconds * 1000,
    })

    loadMe()
      .then(() => navigate('/lobby', { replace: true }))
      .catch(() => setLoadMeError('load_me_failed'))
  }, [callback, navigate])

  if (error) {
    return (
      <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4">
        <div className="text-center">
          <p className="text-red-400">Sign-in failed: {error}</p>
          <a href="/login" className="mt-4 inline-block text-slate-300 underline">
            Back to login
          </a>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4">
      <p className="text-slate-300">Signing you in…</p>
    </div>
  )
}
