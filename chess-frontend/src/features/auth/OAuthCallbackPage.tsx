import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../shared/auth/authStore'
import { loadMe } from './authApi'

/**
 * OAuth2 callback: backend redirects here with tokens in hash (#access_token=...&refresh_token=...&expires_in=...).
 * We save tokens, load profile, then redirect to lobby.
 */
export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const hash = window.location.hash?.slice(1) || ''
    const params = new URLSearchParams(hash)
    const err = params.get('error')
    if (err) {
      setError(err)
      return
    }
    const accessToken = params.get('access_token')
    const refreshToken = params.get('refresh_token')
    const expiresIn = params.get('expires_in')
    if (!accessToken || !refreshToken) {
      setError('missing_tokens')
      return
    }
    const expiresInSeconds = parseInt(expiresIn || '0', 10) || 900
    useAuthStore.getState().setTokens({
      accessToken,
      refreshToken,
      expiresAtMs: Date.now() + expiresInSeconds * 1000,
    })
    loadMe()
      .then(() => navigate('/lobby', { replace: true }))
      .catch(() => setError('load_me_failed'))
  }, [navigate])

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
