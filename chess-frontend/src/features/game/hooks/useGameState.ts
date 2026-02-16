import { useEffect, useState } from 'react'
import { getGameState, type GameState } from '../gameApi'
import { getErrorMessage } from '../../../shared/utils/getErrorMessage'

export function useGameState(gameId: string | undefined) {
  const [state, setState] = useState<GameState | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!gameId) return
    let cancelled = false
    ;(async () => {
      try {
        const st = await getGameState(gameId)
        if (cancelled) return
        setState(st)
        setError(null)
      } catch (e: unknown) {
        if (!cancelled) setError(getErrorMessage(e) || 'Failed to load game state')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [gameId])

  function applyFullState(st: GameState) {
    setState(st)
    setError(null)
  }

  return { state, setState, error, setError, applyFullState }
}
