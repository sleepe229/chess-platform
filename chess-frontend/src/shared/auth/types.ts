export type AuthTokens = {
  accessToken: string
  refreshToken: string
  expiresAtMs: number
}

export type UserProfile = {
  userId: string
  username: string
  email?: string | null
  avatarUrl?: string | null
  country?: string | null
  createdAt?: string | null
}

