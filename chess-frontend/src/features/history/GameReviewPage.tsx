import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Chessboard } from 'react-chessboard'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { getGameState, type GameState } from '../game/gameApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

export function GameReviewPage() {
  const { gameId } = useParams<{ gameId: string }>()
  const [state, setState] = useState<GameState | null>(null)
  const [idx, setIdx] = useState(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!gameId) return
    let cancelled = false
    ;(async () => {
      try {
        const st = await getGameState(gameId)
        if (cancelled) return
        setState(st)
        setIdx(st.moves.length)
      } catch (e: unknown) {
        if (!cancelled) setError(getErrorMessage(e) || 'Failed to load game')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [gameId])

  const fenAtIdx = useMemo(() => {
    if (!state) return 'start'
    if (idx <= 0) return 'start'
    const last = state.moves[Math.min(idx, state.moves.length) - 1]
    return last?.fenAfter || state.fen
  }, [state, idx])

  const movesTable = useMemo(() => {
    const moves = state?.moves ?? []
    const rows: { no: number; w?: string; b?: string }[] = []
    for (let i = 0; i < moves.length; i += 2) {
      rows.push({
        no: Math.floor(i / 2) + 1,
        w: moves[i]?.san || moves[i]?.uci,
        b: moves[i + 1]?.san || moves[i + 1]?.uci,
      })
    }
    return rows
  }, [state?.moves])

  if (!gameId) return null

  return (
    <div className="grid gap-4 lg:grid-cols-[1.6fr_1fr]">
      <Card>
        <div className="flex items-center justify-between">
          <div>
            <div className="text-lg font-semibold">Game</div>
            <div className="mt-1 text-xs text-slate-500 font-mono">{gameId}</div>
          </div>
          <div className="text-sm text-slate-300">{state?.status ? `Status: ${state.status}` : ''}</div>
        </div>

        {error ? <div className="mt-4 text-sm text-red-400">{error}</div> : null}

        <div className="mt-4 mx-auto max-w-[720px]">
          <Chessboard options={{ position: fenAtIdx, allowDragging: false }} />
        </div>

        <div className="mt-4 flex items-center justify-center gap-2">
          <Button variant="secondary" onClick={() => setIdx(0)} disabled={!state}>
            {'<<'}
          </Button>
          <Button variant="secondary" onClick={() => setIdx((v) => Math.max(0, v - 1))} disabled={!state}>
            {'<'}
          </Button>
          <div className="text-sm text-slate-300 tabular-nums">
            {idx} / {state?.moves.length ?? 0}
          </div>
          <Button variant="secondary" onClick={() => setIdx((v) => Math.min((state?.moves.length ?? 0), v + 1))} disabled={!state}>
            {'>'}
          </Button>
          <Button variant="secondary" onClick={() => setIdx(state?.moves.length ?? 0)} disabled={!state}>
            {'>>'}
          </Button>
        </div>

        <div className="mt-4 text-xs text-slate-500">
          Analysis endpoint is not implemented in backend yet (no <span className="font-mono">/analysis</span> controller), so this page is a pure viewer for now.
        </div>
      </Card>

      <Card>
        <div className="text-lg font-semibold">Moves</div>
        <div className="mt-3 max-h-[560px] overflow-auto text-sm">
          <table className="w-full">
            <tbody>
              {movesTable.map((r) => (
                <tr key={r.no} className="border-b border-slate-800/60">
                  <td className="py-2 pr-2 text-slate-500 w-10">{r.no}.</td>
                  <td className="py-2 pr-2">{r.w ?? ''}</td>
                  <td className="py-2">{r.b ?? ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

