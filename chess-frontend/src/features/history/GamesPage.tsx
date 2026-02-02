import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../shared/auth/authStore'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { getRatingHistory } from './historyApi'

export function GamesPage() {
  const me = useAuthStore((s) => s.me)
  const [page, setPage] = useState(0)
  const size = 20

  const q = useQuery({
    queryKey: ['rating-history', me?.userId, page],
    queryFn: () => getRatingHistory(me!.userId, page, size),
    enabled: Boolean(me?.userId),
  })

  const rows = useMemo(() => q.data?.content ?? [], [q.data])

  return (
    <Card>
      <div className="text-lg font-semibold">History</div>
      <div className="mt-1 text-sm text-slate-300">
        Backend doesn’t expose a dedicated <span className="font-mono">/users/&lt;id&gt;/games</span> yet, so this page uses rating history as a proxy.
      </div>

      <div className="mt-6 overflow-auto">
        <table className="min-w-[720px] w-full text-sm">
          <thead className="text-xs text-slate-400">
            <tr className="border-b border-slate-800">
              <th className="py-2 text-left">When</th>
              <th className="py-2 text-left">Result</th>
              <th className="py-2 text-left">Time</th>
              <th className="py-2 text-left">Δ</th>
              <th className="py-2 text-left">Opponent</th>
              <th className="py-2 text-left">Game</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-slate-800/60">
                <td className="py-2 pr-4 text-slate-300">{new Date(r.createdAt).toLocaleString()}</td>
                <td className="py-2 pr-4">
                  <span className={r.result === 'WIN' ? 'text-emerald-400' : r.result === 'LOSS' ? 'text-red-400' : 'text-slate-300'}>{r.result}</span>
                </td>
                <td className="py-2 pr-4 text-slate-300">{r.timeControl}</td>
                <td className="py-2 pr-4 text-slate-300">{Number(r.ratingChange).toFixed(1)}</td>
                <td className="py-2 pr-4 text-slate-400 font-mono">{r.opponentId}</td>
                <td className="py-2">
                  <Link className="font-mono" to={`/games/${r.gameId}`}>
                    {r.gameId}
                  </Link>
                </td>
              </tr>
            ))}
            {rows.length === 0 && !q.isLoading ? (
              <tr>
                <td className="py-4 text-slate-400" colSpan={6}>
                  No games yet.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-between">
        <div className="text-xs text-slate-500">
          Page {q.data?.number ?? page + 1} / {q.data?.totalPages ?? '—'}
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || q.isLoading}>
            Prev
          </Button>
          <Button
            variant="secondary"
            onClick={() => setPage((p) => p + 1)}
            disabled={q.isLoading || (q.data ? page + 1 >= q.data.totalPages : false)}
          >
            Next
          </Button>
        </div>
      </div>
    </Card>
  )
}

