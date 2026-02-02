import { api } from '../../shared/api/apiClient'

export type JoinMatchmakingResponse = { requestId: string; status: string }
export type MatchmakingStatusResponse = { requestId: string; status: string; gameId?: string | null }

export async function joinMatchmaking(params: { baseSeconds: number; incrementSeconds: number; rated: boolean }): Promise<JoinMatchmakingResponse> {
  return api.post<JoinMatchmakingResponse>('/v1/matchmaking/join', params)
}

export async function leaveMatchmaking(params: { requestId: string }): Promise<void> {
  await api.post('/v1/matchmaking/leave', params)
}

export async function getStatus(requestId: string): Promise<MatchmakingStatusResponse> {
  return api.get<MatchmakingStatusResponse>(`/v1/matchmaking/status/${encodeURIComponent(requestId)}`)
}

