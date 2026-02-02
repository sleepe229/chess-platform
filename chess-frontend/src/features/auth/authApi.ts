import { api, apiRequest } from '../../shared/api/apiClient'
import { useAuthStore } from '../../shared/auth/authStore'
import type { UserProfile } from '../../shared/auth/types'

type AuthResponse = {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

type MeResponse = {
  id: string
  username: string
  email?: string | null
  avatarUrl?: string | null
  country?: string | null
  createdAt?: string | null
}

export async function login(email: string, password: string): Promise<void> {
  const res = await api.post<AuthResponse>('/v1/auth/login', { email, password })
  useAuthStore.getState().setTokens({
    accessToken: res.accessToken,
    refreshToken: res.refreshToken,
    expiresAtMs: Date.now() + res.expiresInSeconds * 1000,
  })
  await loadMe()
}

export async function register(email: string, password: string): Promise<void> {
  await api.post('/v1/auth/register', { email, password })
  // auto-login after register
  await login(email, password)
}

export async function logout(): Promise<void> {
  try {
    await api.post('/v1/auth/logout', null)
  } finally {
    useAuthStore.getState().logout()
  }
}

export async function loadMe(): Promise<UserProfile> {
  const me = await apiRequest<MeResponse>('/v1/users/me')
  const mapped: UserProfile = {
    userId: String(me.id),
    username: String(me.username ?? ''),
    email: me.email ?? null,
    avatarUrl: me.avatarUrl ?? null,
    country: me.country ?? null,
    createdAt: me.createdAt ?? null,
  }
  useAuthStore.getState().setMe(mapped)
  return mapped
}

