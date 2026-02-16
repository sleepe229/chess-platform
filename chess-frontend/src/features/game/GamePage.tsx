import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Chess } from 'chess.js'
import { useAuthStore } from '../../shared/auth/authStore'
import { Card } from '../../shared/ui/Card'
import { acceptDraw, offerDraw, resign, type GameState, STARTING_FEN } from './gameApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'
import { getPublicProfile } from '../profile/profileApi'
import { useGameState } from './hooks/useGameState'
import { useClocks } from './hooks/useClocks'
import { useGameWs } from './hooks/useGameWs'
import { ClockPanel } from './components/ClockPanel'
import { MoveList } from './components/MoveList'
import { GameActions } from './components/GameActions'
import { BoardSection } from './components/BoardSection'

type Color = 'white' | 'black'

function uciFromSquares(from: string, to: string, promotion?: string | null) {
  return `${from}${to}${promotion ?? ''}`
}

export function GamePage() {
  const { gameId } = useParams<{ gameId: string }>()
  const token = useAuthStore((s) => s.tokens?.accessToken) || ''
  const me = useAuthStore((s) => s.me)

  const chessRef = useRef(new Chess())
  const { state, setState, error, setError, applyFullState } = useGameState(gameId)

  const applyFullStateWithChess = useCallback(
    (st: GameState) => {
      applyFullState(st)
      try {
        chessRef.current.load(st.fen)
      } catch {
        // ignore
      }
    },
    [applyFullState]
  )

  const { wsStatus, sendMove } = useGameWs({
    gameId,
    token,
    applyFullState: applyFullStateWithChess,
    setError,
    setState,
    chessLoad: (fen) => {
      try {
        chessRef.current.load(fen)
      } catch {
        // ignore
      }
    },
  })

  const myColor: Color | null = useMemo(() => {
    if (!me?.userId || !state) return null
    if (String(state.whiteId) === me.userId) return 'white'
    if (String(state.blackId) === me.userId) return 'black'
    return null
  }, [me?.userId, state?.whiteId, state?.blackId])

  const opponentId = useMemo(() => {
    if (!state || !myColor) return null
    return myColor === 'white' ? String(state.blackId) : String(state.whiteId)
  }, [state, myColor])

  const [opponentUsername, setOpponentUsername] = useState<string | null>(null)
  const [confirmResign, setConfirmResign] = useState(false)
  const [confirmOfferDraw, setConfirmOfferDraw] = useState(false)
  const [confirmAcceptDraw, setConfirmAcceptDraw] = useState(false)

  const { localWhiteMs, localBlackMs } = useClocks(
    state?.clocks?.whiteMs,
    state?.clocks?.blackMs,
    state?.status ?? null,
    state?.sideToMove ?? null
  )

  useEffect(() => {
    if (state?.fen) {
      try {
        chessRef.current.load(state.fen)
      } catch {
        // ignore
      }
    }
  }, [state?.fen])

  // Load opponent profile
  useEffect(() => {
    if (!opponentId) return
    let cancelled = false
    getPublicProfile(opponentId)
      .then((profile) => {
        if (!cancelled) setOpponentUsername(profile.username)
      })
      .catch(() => {
        if (!cancelled) setOpponentUsername(null)
      })
    return () => {
      cancelled = true
    }
  }, [opponentId])

  function onPieceDrop(sourceSquare: string, targetSquare: string) {
    if (!state || !gameId || !token) return false

    const side = (state.sideToMove || '').toUpperCase()
    if ((side === 'WHITE' && myColor !== 'white') || (side === 'BLACK' && myColor !== 'black')) return false

    const chess = chessRef.current
    const fenBefore = chess.fen()
    const move = chess.move({ from: sourceSquare, to: targetSquare })
    if (!move) return false

    const promotionPiece = move.promotion ? String(move.promotion).toLowerCase() : null
    const uci = uciFromSquares(sourceSquare, targetSquare, promotionPiece)
    const clientMoveId = crypto.randomUUID()

    setState((prev) =>
      prev
        ? {
            ...prev,
            fen: chess.fen(),
            moves: [...(prev.moves || []), { ply: (prev.moves?.length || 0) + 1, uci, san: move.san }],
          }
        : prev
    )

    sendMove(uci, clientMoveId)

    if (wsStatus === 'disconnected') {
      chess.load(fenBefore)
      return false
    }
    return true
  }

  async function doResign() {
    if (!gameId) return
    try {
      const st = await resign(gameId)
      applyFullStateWithChess(st)
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Failed to resign')
    }
  }

  async function doOfferDraw() {
    if (!gameId) return
    try {
      const st = await offerDraw(gameId)
      applyFullStateWithChess(st)
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Failed to offer draw')
    }
  }

  async function doAcceptDraw() {
    if (!gameId) return
    try {
      const st = await acceptDraw(gameId)
      applyFullStateWithChess(st)
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Failed to accept draw')
    }
  }

  const drawOfferedByOpponent =
    state?.status === 'RUNNING' &&
    opponentId != null &&
    state.drawOfferedBy != null &&
    String(state.drawOfferedBy) === opponentId

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
      <div className="order-2 lg:order-none space-y-4">
        <Card>
          <div className="text-sm text-slate-400">Opponent</div>
          <div className="mt-1 font-medium">{opponentUsername || 'Opponent'}</div>
        </Card>

        <GameActions
          status={state?.status ?? null}
          drawOfferedByOpponent={!!drawOfferedByOpponent}
          onResign={doResign}
          onOfferDraw={doOfferDraw}
          onAcceptDraw={doAcceptDraw}
          confirmResign={confirmResign}
          setConfirmResign={setConfirmResign}
          confirmOfferDraw={confirmOfferDraw}
          setConfirmOfferDraw={setConfirmOfferDraw}
          confirmAcceptDraw={confirmAcceptDraw}
          setConfirmAcceptDraw={setConfirmAcceptDraw}
          myUsername={me?.username || me?.userId || '—'}
          boardOrientation={boardOrientation}
        />
      </div>

      <div className="order-first lg:order-none">
        <BoardSection
        fen={fen}
        boardOrientation={boardOrientation}
        allowDragging={state?.status === 'RUNNING'}
        wsStatus={wsStatus}
        status={state?.status ?? null}
        result={state?.result ?? null}
        finishReason={state?.finishReason ?? null}
        sideToMove={state?.sideToMove ?? null}
        error={error}
        onPieceDrop={onPieceDrop}
        />
      </div>

      <div className="order-3 lg:order-none space-y-4">
        <ClockPanel
          whiteMs={localWhiteMs}
          blackMs={localBlackMs}
          sideToMove={state?.sideToMove ?? null}
        />
        <MoveList moves={state?.moves ?? []} />
      </div>
    </div>
  )
}
