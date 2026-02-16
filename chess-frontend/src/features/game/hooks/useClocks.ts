import { useEffect, useRef, useState } from 'react'

type ClockSnapshot = {
  whiteMs: number | null
  blackMs: number | null
  status: string | null
  sideToMove: string | null
  syncedAtMs: number
}

export function useClocks(
  whiteMs: number | null | undefined,
  blackMs: number | null | undefined,
  status: string | null,
  sideToMove: string | null
) {
  const snapshotRef = useRef<ClockSnapshot>({
    whiteMs: whiteMs ?? null,
    blackMs: blackMs ?? null,
    status,
    sideToMove,
    syncedAtMs: Date.now(),
  })

  const nextWhiteMs = whiteMs ?? snapshotRef.current.whiteMs
  const nextBlackMs = blackMs ?? snapshotRef.current.blackMs
  if (
    nextWhiteMs !== snapshotRef.current.whiteMs ||
    nextBlackMs !== snapshotRef.current.blackMs ||
    status !== snapshotRef.current.status ||
    sideToMove !== snapshotRef.current.sideToMove
  ) {
    snapshotRef.current = {
      whiteMs: nextWhiteMs,
      blackMs: nextBlackMs,
      status,
      sideToMove,
      syncedAtMs: Date.now(),
    }
  }

  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    if (status !== 'RUNNING') return
    const interval = window.setInterval(() => {
      setNowMs(Date.now())
    }, 1000)
    return () => window.clearInterval(interval)
  }, [status])

  const snapshot = snapshotRef.current
  const elapsedMs = snapshot.status === 'RUNNING' ? Math.max(0, nowMs - snapshot.syncedAtMs) : 0
  const side = (snapshot.sideToMove || '').toUpperCase()

  const localWhiteMs =
    side === 'WHITE' && typeof snapshot.whiteMs === 'number'
      ? Math.max(0, snapshot.whiteMs - elapsedMs)
      : snapshot.whiteMs

  const localBlackMs =
    side === 'BLACK' && typeof snapshot.blackMs === 'number'
      ? Math.max(0, snapshot.blackMs - elapsedMs)
      : snapshot.blackMs

  return { localWhiteMs, localBlackMs }
}
