import { useEffect, useMemo, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Chess } from 'chess.js'
import { Chessboard, type PieceDropHandlerArgs } from 'react-chessboard'
import { useAuthStore } from '../../shared/auth/authStore'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { GameWsClient, type WsServerMessage, type WsStatus } from '../../shared/ws/GameWsClient'

type MsgMoveAccepted = WsServerMessage & { type: 'MOVE_ACCEPTED'; fen: string; clocks?: { whiteMs: number; blackMs: number } }
type MsgGameFinished = WsServerMessage & { type: 'GAME_FINISHED'; result: string; reason: string }
import { acceptDraw, getGameState, offerDraw, resign, type GameState } from './gameApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'
import { STARTING_FEN } from './gameApi'

type Color = 'white' | 'black'

function uciFromSquares(from: string, to: string, promotion?: string | null) {
  return `${from}${to}${promotion ?? ''}`
}

export function GamePage() {
  const { gameId } = useParams<{ gameId: string }>()
  const token = useAuthStore((s) => s.tokens?.accessToken) || ''
  const me = useAuthStore((s) => s.me)

  const [wsStatus, setWsStatus] = useState<WsStatus>('disconnected')
  const [state, setState] = useState<GameState | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [localWhiteMs, setLocalWhiteMs] = useState<number | null>(null)
  const [localBlackMs, setLocalBlackMs] = useState<number | null>(null)

  const chessRef = useRef(new Chess())
  const wsRef = useRef<GameWsClient | null>(null)
  const pendingClientMoveId = useRef<string | null>(null)

  const myColor: Color | null = useMemo(() => {
    if (!me?.userId || !state) return null
    if (String(state.whiteId) === me.userId) return 'white'
    if (String(state.blackId) === me.userId) return 'black'
    return null
  }, [me?.userId, state?.whiteId, state?.blackId])

  useEffect(() => {
    if (!gameId) return
    let cancelled = false

    ;(async () => {
      try {
        const st = await getGameState(gameId)
        if (cancelled) return
        applyFullState(st)
      } catch (e: unknown) {
        if (!cancelled) setError(getErrorMessage(e) || 'Failed to load game state')
      }
    })()

    return () => {
      cancelled = true
    }
  }, [gameId])

  useEffect(() => {
    if (!gameId || !token) return

    const ws = new GameWsClient({
      onStatus: (s) => setWsStatus(s),
      onMessage: (m) => onWsMessage(m),
    })
    wsRef.current = ws
    ws.connect({ gameId, token, lastSeenPly: state?.moves?.length ?? 0 })

    return () => {
      ws.close()
      wsRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gameId, token])

  // local clocks tick (best-effort)
  useEffect(() => {
    if (!state?.clocks) return
    setLocalWhiteMs(state.clocks.whiteMs)
    setLocalBlackMs(state.clocks.blackMs)
  }, [state?.clocks?.whiteMs, state?.clocks?.blackMs])

  useEffect(() => {
    if (!state?.status || state.status !== 'RUNNING') return
    const interval = window.setInterval(() => {
      const side = (state.sideToMove || '').toUpperCase()
      if (side === 'WHITE') setLocalWhiteMs((v) => (typeof v === 'number' ? Math.max(0, v - 1000) : v))
      if (side === 'BLACK') setLocalBlackMs((v) => (typeof v === 'number' ? Math.max(0, v - 1000) : v))
    }, 1000)
    return () => window.clearInterval(interval)
  }, [state?.status, state?.sideToMove])

  function applyFullState(st: GameState) {
    setState(st)
    try {
      chessRef.current.load(st.fen)
    } catch {
      // ignore; board will still render from fen
    }
  }

  async function onWsMessage(msg: WsServerMessage) {
    if (!gameId) return

    if (msg.type === 'GAME_STATE') {
      setError(null)
      const gs = msg as Extract<WsServerMessage, { type: 'GAME_STATE' }>
      const st: GameState = {
        gameId: String(gs.gameId),
        whiteId: String(gs.whiteId),
        blackId: String(gs.blackId),
        fen: gs.fen,
        moves: (gs.moves ?? []).map((m) => ({
          ply: m.ply,
          uci: m.uci,
          san: m.san ?? null,
        })),
        clocks: gs.clocks ? { whiteMs: gs.clocks.whiteMs, blackMs: gs.clocks.blackMs } : null,
        status: gs.status ?? null,
        sideToMove: gs.sideToMove ?? null,
      }
      applyFullState(st)
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
              : prev,
          )
          chessRef.current.load(ma.fen)
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
          : prev,
      )
      chessRef.current.load(ma.fen)
      return
    }

    if (msg.type === 'MOVE_REJECTED') {
      setError(String((msg as { reason?: string }).reason || 'MOVE_REJECTED'))
      pendingClientMoveId.current = null
      // Resync from server to rollback
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
      return
    }
  }

  function onPieceDrop(sourceSquare: string, targetSquare: string) {
    if (!state || !gameId) return false
    if (!token) return false

    // basic turn check (client-side)
    const side = (state.sideToMove || '').toUpperCase()
    if ((side === 'WHITE' && myColor !== 'white') || (side === 'BLACK' && myColor !== 'black')) {
      return false
    }

    // chess.js move validation
    const chess = chessRef.current
    const fenBefore = chess.fen()
    const move = chess.move({
      from: sourceSquare,
      to: targetSquare,
    })

    if (!move) return false

    // optimistic update in UI
    const clientMoveId = crypto.randomUUID()
    pendingClientMoveId.current = clientMoveId
    setState((prev) =>
      prev
        ? {
            ...prev,
            fen: chess.fen(),
            moves: [...(prev.moves || []), { ply: (prev.moves?.length || 0) + 1, uci: uciFromSquares(sourceSquare, targetSquare, null), san: move.san }],
          }
        : prev,
    )

    wsRef.current?.send({
      type: 'MOVE',
      gameId,
      clientMoveId,
      uci: uciFromSquares(sourceSquare, targetSquare, null),
    })

    // if server rejects later, we will resync; keep a local rollback if ws is down immediately
    if (wsStatus === 'disconnected') {
      chess.load(fenBefore)
      return false
    }

    return true
  }

  async function onResign() {
    if (!gameId) return
    if (!window.confirm('Resign the game?')) return
    const st = await resign(gameId)
    applyFullState(st)
  }

  async function onOfferDraw() {
    if (!gameId) return
    if (!window.confirm('Offer a draw?')) return
    const st = await offerDraw(gameId)
    applyFullState(st)
  }

  async function onAcceptDraw() {
    if (!gameId) return
    if (!window.confirm('Accept draw?')) return
    const st = await acceptDraw(gameId)
    applyFullState(st)
  }

  if (!gameId) return null

  const boardOrientation = myColor ?? 'white'
  const fen = state?.fen || STARTING_FEN

  if (state === null && !error) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-6 py-4">
          <div className="text-slate-300">Loading game…</div>
          <div className="mt-1 text-xs text-slate-500">{gameId}</div>
        </div>
      </div>
    )
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_1.6fr_1fr]">
      <div className="space-y-4">
        <Card>
          <div className="text-sm text-slate-400">Opponent</div>
          <div className="mt-1 font-medium">{myColor === 'white' ? state?.blackId : state?.whiteId}</div>
          <div className="mt-2 text-xs text-slate-500">gameId: {gameId}</div>
        </Card>

        <Card>
          <div className="text-sm text-slate-400">You</div>
          <div className="mt-1 font-medium">{me?.username || me?.userId}</div>
          <div className="mt-2 text-xs text-slate-500">Color: {boardOrientation}</div>
        </Card>

        <Card className="space-y-2">
          <Button variant="danger" className="w-full" onClick={onResign}>
            Resign
          </Button>
          <Button variant="secondary" className="w-full" onClick={onOfferDraw}>
            Offer draw
          </Button>
          <Button variant="secondary" className="w-full" onClick={onAcceptDraw}>
            Accept draw
          </Button>
        </Card>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="text-sm">
            <span className="text-slate-400">WS:</span>{' '}
            <span className={wsStatus === 'connected' ? 'text-emerald-400' : wsStatus === 'reconnecting' ? 'text-amber-400' : 'text-red-400'}>
              {wsStatus}
            </span>
          </div>
          <div className="text-sm text-slate-300">{state?.status === 'FINISHED' ? `Finished: ${state?.result ?? ''} (${state?.finishReason ?? ''})` : state?.sideToMove ? `${state.sideToMove} to move` : ''}</div>
        </div>

        <div className="mx-auto w-full max-w-[720px]">
          <Chessboard
            options={{
              position: fen,
              boardOrientation,
              allowDragging: state?.status === 'RUNNING',
              boardStyle: {
                borderRadius: '8px',
                boxShadow: '0 0 0 1px rgba(148, 163, 184, 0.15)',
              },
              onPieceDrop: ({ sourceSquare, targetSquare }: PieceDropHandlerArgs) => {
                if (!targetSquare) return false
                return onPieceDrop(sourceSquare, targetSquare)
              },
            }}
          />
        </div>

        {error ? <div className="text-sm text-red-400">{error}</div> : null}
      </div>

      <div className="space-y-4">
        <Card>
          <div className="text-xs text-slate-400">Clocks</div>
          <div className="mt-2 grid grid-cols-2 gap-3 text-sm">
            <Clock label="White" ms={localWhiteMs} active={(state?.sideToMove || '').toUpperCase() === 'WHITE'} />
            <Clock label="Black" ms={localBlackMs} active={(state?.sideToMove || '').toUpperCase() === 'BLACK'} />
          </div>
        </Card>

        <Card>
          <div className="text-xs text-slate-400">Moves</div>
          <div className="mt-3 max-h-[420px] overflow-auto text-sm">
            <table className="w-full">
              <tbody>
                {chunkPairs(state?.moves || []).map((pair) => (
                  <tr key={pair.no} className="border-b border-slate-800/60">
                    <td className="py-2 pr-2 text-slate-500 w-10">{pair.no}.</td>
                    <td className="py-2 pr-2">{pair.white ?? ''}</td>
                    <td className="py-2">{pair.black ?? ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  )
}

function msToClock(ms: number | null | undefined) {
  if (typeof ms !== 'number') return '—'
  const total = Math.max(0, Math.floor(ms / 1000))
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function Clock({ label, ms, active }: { label: string; ms: number | null; active: boolean }) {
  return (
    <div className={`rounded-md border px-3 py-2 ${active ? 'border-sky-500/60 bg-sky-500/10' : 'border-slate-800 bg-slate-950'}`}>
      <div className="text-xs text-slate-400">{label}</div>
      <div className="mt-1 text-lg font-semibold tabular-nums">{msToClock(ms)}</div>
    </div>
  )
}

function chunkPairs(moves: { ply: number; san?: string | null; uci?: string | null }[]) {
  const rows: { no: number; white?: string; black?: string }[] = []
  for (let i = 0; i < moves.length; i += 2) {
    const no = Math.floor(i / 2) + 1
    rows.push({
      no,
      white: moves[i]?.san || moves[i]?.uci || '',
      black: moves[i + 1]?.san || moves[i + 1]?.uci || '',
    })
  }
  return rows
}

