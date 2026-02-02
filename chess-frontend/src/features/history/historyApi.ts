import { api } from '../../shared/api/apiClient'

export type RatingHistoryItem = {
  id: string
  timeControl: string
  oldRating: number
  newRating: number
  ratingChange: number
  gameId: string
  opponentId: string
  opponentRating: number
  result: string
  createdAt: string
}

export type Page<T> = {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export async function getRatingHistory(userId: string, page = 0, size = 20): Promise<Page<RatingHistoryItem>> {
  return api.get<Page<RatingHistoryItem>>(`/v1/users/${encodeURIComponent(userId)}/rating-history?page=${page}&size=${size}`)
}

