import { useEffect, useRef, useCallback, useState } from 'react'
import { GameWsClient, type WsServerMessage, type WsStatus } from '../../../shared/ws/GameWsClient'
import { getGameState } from '../gameApi'
import { wsMessageToGameState, type GameState } from '../gameApi'

type MsgMoveAccepted = WsServerMessage & {
  type: 'MOVE_ACCEPTED'
  fen: string
  clocks?: { whiteMs: number; blackMs: number }
}
type MsgGameFinished = WsServerMessage & { type: 'GAME_FINISHED'; result: string; reason: string }

type UseGameWsArgs = {
  gameId: string | undefined
  token: string
  applyFullState: (st: GameState) => void
  setError: (e: string | null) => void
  setState: React.Dispatch<React.SetStateAction<GameState | null>>
  chessLoad: (fen: string) => void
}

export function useGameWs({
  gameId,
  token,
  applyFullState,
  setError,
  setState,
  chessLoad,
}: UseGameWsArgs) {
  const wsRef = useRef<GameWsClient | null>(null)
  const pendingClientMoveId = useRef<string | null>(null)

  const onWsMessage = useCallback(
    async (msg: WsServerMessage) => {
      if (!gameId) return

      if (msg.type === 'GAME_STATE') {
        setError(null)
        applyFullState(wsMessageToGameState(msg as Extract<WsServerMessage, { type: 'GAME_STATE' }>))
        return
      }

      if (msg.type === 'MOVE_ACCEPTED') {
        setError(null)
        const ma = msg as MsgMoveAccepted
        if (!ma.clientMoveId) {
          try {
            const st = await getGameState(gameId)
            applyFullState(st)
          } catch {
            setState((prev) =>
              prev
                ? {
                    ...prev,
                    fen: ma.fen,
                    clocks: ma.clocks ? { whiteMs: ma.clocks.whiteMs, blackMs: ma.clocks.blackMs } : prev.clocks,
                  }
                : prev
            )
            chessLoad(ma.fen)
          }
          return
        }
        pendingClientMoveId.current = null
        setState((prev) =>
          prev
            ? {
                ...prev,
                fen: ma.fen,
                clocks: ma.clocks ? { whiteMs: ma.clocks.whiteMs, blackMs: ma.clocks.blackMs } : prev.clocks,
              }
            : prev
        )
        chessLoad(ma.fen)
        return
      }

      if (msg.type === 'MOVE_REJECTED') {
        setError(String((msg as { reason?: string }).reason || 'MOVE_REJECTED'))
        pendingClientMoveId.current = null
        try {
          const st = await getGameState(gameId)
          applyFullState(st)
        } catch {
          // ignore
        }
        return
      }

      if (msg.type === 'GAME_FINISHED') {
        const gf = msg as MsgGameFinished
        setState((prev) => (prev ? { ...prev, status: 'FINISHED', result: gf.result, finishReason: gf.reason } : prev))
      }
    },
    [gameId, applyFullState, setError, setState, chessLoad]
  )

  const [wsStatus, setWsStatus] = useState<WsStatus>('disconnected')

  // Reconnect only when gameId or token changes; intentionally omit state/moves so we don't
  // reconnect on every state update. Full state is applied on connect via GAME_STATE or getGameState.
  useEffect(() => {
    if (!gameId || !token) return

    const ws = new GameWsClient({
      onStatus: (s: WsStatus) => setWsStatus(s),
      onMessage: (m: WsServerMessage) => void onWsMessage(m),
    })
    wsRef.current = ws
    ws.connect({ gameId, token })

    return () => {
      ws.close()
      wsRef.current = null
    }
  }, [gameId, token, onWsMessage])

  const sendMove = useCallback(
    (uci: string, clientMoveId: string) => {
      pendingClientMoveId.current = clientMoveId
      wsRef.current?.send({ type: 'MOVE', gameId: gameId!, clientMoveId, uci })
    },
    [gameId]
  )

  return { wsStatus, sendMove, wsRef }
}
