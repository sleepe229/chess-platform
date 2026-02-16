import { useAuthStore } from '../auth/authStore'
import { ApiError } from './errors'

type Json = Record<string, unknown> | unknown[] | string | number | boolean | null

type ErrorResponse = {
  error?: string
  message?: string
  traceId?: string
  details?: unknown
}

function apiBaseUrl(): string {
  const v = import.meta.env.VITE_API_BASE_URL
  return v && v.trim().length > 0 ? v.trim().replace(/\/$/, '') : ''
}

/** Base URL for API (e.g. for OAuth2 redirect). */
export function getApiBaseUrl(): string {
  return apiBaseUrl() || (typeof window !== 'undefined' ? window.location.origin : '')
}

function newRequestId(): string {
  // modern browsers
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  // fallback
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

let refreshInFlight: Promise<void> | null = null

async function refreshTokens(): Promise<void> {
  if (refreshInFlight) return refreshInFlight

  refreshInFlight = (async () => {
    const { tokens } = useAuthStore.getState()
    if (!tokens?.refreshToken) {
      throw new ApiError({ status: 401, message: 'Not authenticated' })
    }

    const res = await fetch(`${apiBaseUrl()}/v1/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': newRequestId(),
      },
      body: JSON.stringify({ refreshToken: tokens.refreshToken }),
    })

    if (!res.ok) {
      useAuthStore.getState().logout()
      throw new ApiError({ status: res.status, message: 'Session expired' })
    }

    const json = (await res.json()) as { accessToken: string; refreshToken: string; expiresInSeconds: number }
    const expiresAtMs = Date.now() + (json.expiresInSeconds ?? 0) * 1000
    useAuthStore.getState().setTokens({
      accessToken: json.accessToken,
      refreshToken: json.refreshToken,
      expiresAtMs,
    })
  })().finally(() => {
    refreshInFlight = null
  })

  return refreshInFlight
}

async function readError(res: Response): Promise<ErrorResponse | null> {
  const ct = res.headers.get('content-type') || ''
  if (!ct.includes('application/json')) return null
  try {
    return (await res.json()) as ErrorResponse
  } catch {
    return null
  }
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit & { json?: Json } = {},
  opts: { retryOn401?: boolean } = {},
): Promise<T> {
  const retryOn401 = opts.retryOn401 ?? true
  const { tokens } = useAuthStore.getState()

  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  headers.set('X-Request-Id', headers.get('X-Request-Id') || newRequestId())

  if (tokens?.accessToken) {
    headers.set('Authorization', `Bearer ${tokens.accessToken}`)
  }

  let body = init.body
  if (init.json !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(init.json)
  }

  const res = await fetch(`${apiBaseUrl()}${path}`, {
    ...init,
    headers,
    body,
  })

  if (res.status === 401 && retryOn401) {
    await refreshTokens()
    return apiRequest<T>(path, init, { retryOn401: false })
  }

  if (!res.ok) {
    const err = await readError(res)
    throw new ApiError({
      status: res.status,
      code: err?.error,
      message: err?.message || `HTTP ${res.status}`,
      traceId: err?.traceId,
      details: err?.details,
    })
  }

  if (res.status === 204) return undefined as T

  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) {
    return (await res.json()) as T
  }

  return (await res.text()) as unknown as T
}

export const api = {
  get: <T>(path: string, init?: RequestInit) => apiRequest<T>(path, { ...(init || {}), method: 'GET' }),
  post: <T>(path: string, json?: Json, init?: RequestInit) =>
    apiRequest<T>(path, { ...(init || {}), method: 'POST', json }),
  put: <T>(path: string, json?: Json, init?: RequestInit) => apiRequest<T>(path, { ...(init || {}), method: 'PUT', json }),
}

