import { api } from '../../shared/api/apiClient'

/** Standard starting position FEN. */
export const STARTING_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

export type GameMove = { ply: number; uci: string; san?: string | null; fenAfter?: string | null; byUserId?: string | null }
export type GameState = {
  gameId: string
  whiteId: string
  blackId: string
  fen: string
  moves: GameMove[]
  clocks?: { whiteMs: number; blackMs: number } | null
  status?: string | null
  sideToMove?: string | null
  result?: string | null
  finishReason?: string | null
}

export async function getGameState(gameId: string): Promise<GameState> {
  return api.get<GameState>(`/v1/games/${encodeURIComponent(gameId)}/state`)
}

export async function resign(gameId: string): Promise<GameState> {
  return api.post<GameState>(`/v1/games/${encodeURIComponent(gameId)}/resign`)
}

export async function offerDraw(gameId: string): Promise<GameState> {
  return api.post<GameState>(`/v1/games/${encodeURIComponent(gameId)}/offer-draw`)
}

export async function acceptDraw(gameId: string): Promise<GameState> {
  return api.post<GameState>(`/v1/games/${encodeURIComponent(gameId)}/accept-draw`)
}

