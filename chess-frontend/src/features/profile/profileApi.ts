import { api } from '../../shared/api/apiClient'

export type UserProfileResponse = {
  id: string
  username: string
  email?: string | null
  avatarUrl?: string | null
  bio?: string | null
  country?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

/** Public profile (GET /users/{userId}) — no email/bio */
export type UserProfilePublicResponse = {
  userId: string
  username: string
  avatarUrl?: string | null
  country?: string | null
  totalGames?: number | null
  totalWins?: number | null
  totalLosses?: number | null
  totalDraws?: number | null
  winRate?: number | null
  ratings?: RatingDto[] | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type RatingDto = {
  type: string
  rating: number
  deviation: number
  volatility?: number
  games_played?: number
  peakRating?: number
  updatedAt?: string
}

export type RatingsResponse = { userId: string; ratings: RatingDto[] }

export async function getMe(): Promise<UserProfileResponse> {
  return api.get<UserProfileResponse>('/v1/users/me')
}

export async function updateMe(payload: { username?: string; avatarUrl?: string; bio?: string; country?: string }): Promise<UserProfileResponse> {
  return api.put<UserProfileResponse>('/v1/users/me', payload)
}

export async function getRatings(userId: string): Promise<RatingsResponse> {
  return api.get<RatingsResponse>(`/v1/users/${encodeURIComponent(userId)}/ratings`)
}

export async function getPublicProfile(userId: string): Promise<UserProfilePublicResponse> {
  return api.get<UserProfilePublicResponse>(`/v1/users/${encodeURIComponent(userId)}`)
}

