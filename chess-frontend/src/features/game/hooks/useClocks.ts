import { useEffect, useState } from 'react'

export function useClocks(
  whiteMs: number | null | undefined,
  blackMs: number | null | undefined,
  status: string | null,
  sideToMove: string | null
) {
  const [localWhiteMs, setLocalWhiteMs] = useState<number | null>(null)
  const [localBlackMs, setLocalBlackMs] = useState<number | null>(null)

  useEffect(() => {
    if (whiteMs != null) setLocalWhiteMs(whiteMs)
    if (blackMs != null) setLocalBlackMs(blackMs)
  }, [whiteMs, blackMs])

  useEffect(() => {
    if (status !== 'RUNNING') return
    const interval = window.setInterval(() => {
      const side = (sideToMove || '').toUpperCase()
      if (side === 'WHITE') setLocalWhiteMs((v) => (typeof v === 'number' ? Math.max(0, v - 1000) : v))
      if (side === 'BLACK') setLocalBlackMs((v) => (typeof v === 'number' ? Math.max(0, v - 1000) : v))
    }, 1000)
    return () => window.clearInterval(interval)
  }, [status, sideToMove])

  return { localWhiteMs, localBlackMs }
}
