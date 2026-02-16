import { api } from '../../shared/api/apiClient'

/** Spring Page response. */
export type Page<T> = {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export type GameFact = {
  gameId: string
  whitePlayerId: string
  blackPlayerId: string
  result: string
  finishReason: string
  winnerId: string | null
  finishedAt: string
  pgn: string | null
  rated: boolean
  timeControlType: string
  moveCount: number | null
}

export type AnalysisJob = {
  analysisJobId: string
  gameId: string
  requestedBy: string
  status: string
  totalMoves: number | null
  accuracyWhite: number | null
  accuracyBlack: number | null
  errorMessage: string | null
  completedAt: string | null
  createdAt: string
}

export type PlayerStats = {
  totalGames: number
  wins: number
  draws: number
  losses: number
  ratingsByTimeControl: Record<string, number>
}

/** List finished games (optionally for one player). */
export async function getGames(params: {
  playerId?: string
  from?: string
  to?: string
  page?: number
  size?: number
}): Promise<Page<GameFact>> {
  const sp = new URLSearchParams()
  if (params.playerId) sp.set('playerId', params.playerId)
  if (params.from) sp.set('from', params.from)
  if (params.to) sp.set('to', params.to)
  if (params.page != null) sp.set('page', String(params.page))
  if (params.size != null) sp.set('size', String(params.size))
  const q = sp.toString()
  return api.get<Page<GameFact>>(`/v1/analysis/games${q ? `?${q}` : ''}`)
}

/** Get one game fact by id. */
export async function getGame(gameId: string): Promise<GameFact> {
  return api.get<GameFact>(`/v1/analysis/games/${encodeURIComponent(gameId)}`)
}

/** List analysis jobs (optional filters). */
export async function getAnalysisJobs(params: {
  gameId?: string
  requestedBy?: string
  status?: string
  page?: number
  size?: number
}): Promise<Page<AnalysisJob>> {
  const sp = new URLSearchParams()
  if (params.gameId) sp.set('gameId', params.gameId)
  if (params.requestedBy) sp.set('requestedBy', params.requestedBy)
  if (params.status) sp.set('status', params.status)
  if (params.page != null) sp.set('page', String(params.page))
  if (params.size != null) sp.set('size', String(params.size))
  const q = sp.toString()
  return api.get<Page<AnalysisJob>>(`/v1/analysis/jobs${q ? `?${q}` : ''}`)
}

/** Get one analysis job by id. */
export async function getAnalysisJob(analysisJobId: string): Promise<AnalysisJob> {
  return api.get<AnalysisJob>(`/v1/analysis/jobs/${encodeURIComponent(analysisJobId)}`)
}

/** Request analysis for a game. Returns job with status PENDING. */
export async function requestAnalysis(gameId: string): Promise<AnalysisJob> {
  return api.post<AnalysisJob>(`/v1/analysis/games/${encodeURIComponent(gameId)}/analyze`, {})
}

/** Get player stats (games count, wins/draws/losses, ratings by time control). */
export async function getPlayerStats(playerId: string): Promise<PlayerStats> {
  return api.get<PlayerStats>(`/v1/analysis/players/${encodeURIComponent(playerId)}/stats`)
}
