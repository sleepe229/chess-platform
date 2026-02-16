import { Card } from '../../../shared/ui/Card'

function msToClock(ms: number | null | undefined) {
  if (typeof ms !== 'number') return '—'
  const total = Math.max(0, Math.floor(ms / 1000))
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function Clock({
  label,
  ms,
  active,
}: {
  label: string
  ms: number | null
  active: boolean
}) {
  return (
    <div
      className={`rounded-md border px-3 py-2 ${active ? 'border-sky-500/60 bg-sky-500/10' : 'border-slate-800 bg-slate-950'}`}
    >
      <div className="text-xs text-slate-400">{label}</div>
      <div className="mt-1 text-lg font-semibold tabular-nums">{msToClock(ms)}</div>
    </div>
  )
}

export function ClockPanel({
  whiteMs,
  blackMs,
  sideToMove,
}: {
  whiteMs: number | null
  blackMs: number | null
  sideToMove: string | null
}) {
  const stm = (sideToMove || '').toUpperCase()
  return (
    <Card>
      <div className="text-xs text-slate-400">Clocks</div>
      <div className="mt-2 grid grid-cols-2 gap-3 text-sm">
        <Clock label="White" ms={whiteMs} active={stm === 'WHITE'} />
        <Clock label="Black" ms={blackMs} active={stm === 'BLACK'} />
      </div>
    </Card>
  )
}
