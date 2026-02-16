import { useMemo, useState } from 'react'
import { useQuery, useQueries } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../shared/auth/authStore'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { getGames, type GameFact } from '../analytics/analyticsApi'
import { getPublicProfile } from '../profile/profileApi'

function formatResult(result: string): string {
  if (result === '1-0' || result === 'WHITE_WINS') return 'White wins'
  if (result === '0-1' || result === 'BLACK_WINS') return 'Black wins'
  if (result === '1/2-1/2' || result === 'DRAW') return 'Draw'
  return result
}

function opponentId(game: GameFact, myId: string): string {
  return game.whitePlayerId === myId ? game.blackPlayerId : game.whitePlayerId
}

export function GamesPage() {
  const me = useAuthStore((s) => s.me)
  const [page, setPage] = useState(0)
  const size = 20

  const q = useQuery({
    queryKey: ['analytics-games', me?.userId, page],
    queryFn: () => getGames({ playerId: me!.userId, page, size }),
    enabled: Boolean(me?.userId),
  })

  const rows = useMemo(() => q.data?.content ?? [], [q.data])
  const totalPages = q.data?.totalPages ?? 0

  const opponentIds = useMemo(
    () => (me ? [...new Set(rows.map((g) => opponentId(g, me.userId)))] : []),
    [rows, me]
  )

  const opponentProfiles = useQueries({
    queries: opponentIds.map((id) => ({
      queryKey: ['profile', id],
      queryFn: () => getPublicProfile(id),
      enabled: Boolean(id),
    })),
  })

  const opponentUsernameMap = useMemo(() => {
    const m = new Map<string, string>()
    opponentIds.forEach((id, i) => {
      const data = opponentProfiles[i]?.data
      if (data?.username) m.set(id, data.username)
    })
    return m
  }, [opponentIds, opponentProfiles])

  return (
    <Card>
      <div className="text-lg font-semibold">History</div>
      <div className="mt-1 text-sm text-slate-300">
        Finished games from Analytics service (<span className="font-mono">/v1/analysis/games</span>).
      </div>

      {/* Desktop: table */}
      <div className="mt-6 overflow-auto hidden md:block">
        <table className="min-w-[720px] w-full text-sm">
          <thead className="text-xs text-slate-400">
            <tr className="border-b border-slate-800">
              <th className="py-2 text-left">When</th>
              <th className="py-2 text-left">Result</th>
              <th className="py-2 text-left">Time control</th>
              <th className="py-2 text-left">Opponent</th>
              <th className="py-2 text-left">Game</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((g) => (
              <tr key={g.gameId} className="border-b border-slate-800/60">
                <td className="py-2 pr-4 text-slate-300">
                  {new Date(g.finishedAt).toLocaleString()}
                </td>
                <td className="py-2 pr-4">
                  <span
                    className={
                      g.result === '1-0' || g.result === 'WHITE_WINS'
                        ? 'text-emerald-400'
                        : g.result === '0-1' || g.result === 'BLACK_WINS'
                          ? 'text-red-400'
                          : 'text-slate-300'
                    }
                  >
                    {formatResult(g.result)}
                  </span>
                </td>
                <td className="py-2 pr-4 text-slate-300">{g.timeControlType}</td>
                <td className="py-2 pr-4 text-slate-300">
                  {me ? opponentUsernameMap.get(opponentId(g, me.userId)) ?? opponentId(g, me.userId).slice(0, 8) + '…' : '—'}
                </td>
                <td className="py-2">
                  <Link className="font-mono" to={`/games/${g.gameId}`}>
                    {g.gameId.slice(0, 8)}…
                  </Link>
                </td>
              </tr>
            ))}
            {rows.length === 0 && !q.isLoading ? (
              <tr>
                <td className="py-4 text-slate-400" colSpan={5}>
                  No games yet. Games appear here after they finish (event from game service).
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      {/* Mobile: cards */}
      <div className="mt-6 md:hidden space-y-3">
        {rows.length === 0 && !q.isLoading ? (
          <p className="text-sm text-slate-400 py-4">No games yet. Games appear here after they finish (event from game service).</p>
        ) : (
          rows.map((g) => (
            <Link
              key={g.gameId}
              to={`/games/${g.gameId}`}
              className="block rounded-lg border border-slate-800 bg-slate-950/80 p-4 text-left hover:border-slate-700 transition-colors"
            >
              <div className="flex items-center justify-between gap-2">
                <span
                  className={
                    g.result === '1-0' || g.result === 'WHITE_WINS'
                      ? 'text-emerald-400 font-medium'
                      : g.result === '0-1' || g.result === 'BLACK_WINS'
                        ? 'text-red-400 font-medium'
                        : 'text-slate-300 font-medium'
                  }
                >
                  {formatResult(g.result)}
                </span>
                <span className="text-xs text-slate-500">{g.timeControlType}</span>
              </div>
              <div className="mt-1 text-sm text-slate-400">
                {new Date(g.finishedAt).toLocaleString()}
              </div>
              <div className="mt-1 text-sm text-slate-300">
                vs {me ? opponentUsernameMap.get(opponentId(g, me.userId)) ?? opponentId(g, me.userId).slice(0, 8) + '…' : '—'}
              </div>
            </Link>
          ))
        )}
      </div>

      <div className="mt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="text-xs text-slate-500">
          Page {(q.data?.number ?? page) + 1} / {totalPages || '—'} ({q.data?.totalElements ?? 0} total)
        </div>
        <div className="flex gap-2">
          <Button
            variant="secondary"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0 || q.isLoading}
          >
            Prev
          </Button>
          <Button
            variant="secondary"
            onClick={() => setPage((p) => p + 1)}
            disabled={q.isLoading || (totalPages > 0 && page + 1 >= totalPages)}
          >
            Next
          </Button>
        </div>
      </div>
    </Card>
  )
}
