import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { useAuthStore } from '../../shared/auth/authStore'
import { getStatus, joinMatchmaking, leaveMatchmaking } from './matchmakingApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

type Preset = { label: string; baseSeconds: number; incrementSeconds: number }

const MODES: { key: string; label: string; presets: Preset[] }[] = [
  { key: 'BULLET', label: 'Bullet', presets: [{ label: '1+0', baseSeconds: 60, incrementSeconds: 0 }] },
  {
    key: 'BLITZ',
    label: 'Blitz',
    presets: [
      { label: '3+0', baseSeconds: 180, incrementSeconds: 0 },
      { label: '3+2', baseSeconds: 180, incrementSeconds: 2 },
      { label: '5+0', baseSeconds: 300, incrementSeconds: 0 },
    ],
  },
  { key: 'RAPID', label: 'Rapid', presets: [{ label: '10+0', baseSeconds: 600, incrementSeconds: 0 }, { label: '15+10', baseSeconds: 900, incrementSeconds: 10 }] },
  { key: 'CLASSICAL', label: 'Classical', presets: [{ label: '30+0', baseSeconds: 1800, incrementSeconds: 0 }] },
]

export function LobbyPage() {
  const nav = useNavigate()
  const me = useAuthStore((s) => s.me)

  const [modeKey, setModeKey] = useState(MODES[1]!.key)
  const presets = useMemo(() => MODES.find((m) => m.key === modeKey)?.presets ?? [], [modeKey])
  const [preset, setPreset] = useState<Preset>(MODES[1]!.presets[0]!)
  const [rated, setRated] = useState(true)

  const [searching, setSearching] = useState(false)
  const [requestId, setRequestId] = useState<string | null>(null)
  const [waitSec, setWaitSec] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const pollTimer = useRef<number | null>(null)
  const waitTimer = useRef<number | null>(null)

  useEffect(() => {
    // reset preset on mode change
    const p = presets[0]
    if (p) setPreset(p)
  }, [modeKey, presets])

  useEffect(() => {
    return () => {
      if (pollTimer.current) window.clearInterval(pollTimer.current)
      if (waitTimer.current) window.clearInterval(waitTimer.current)
    }
  }, [])

  async function startSearch() {
    setError(null)
    setSearching(true)
    setWaitSec(0)
    try {
      const res = await joinMatchmaking({
        baseSeconds: preset.baseSeconds,
        incrementSeconds: preset.incrementSeconds,
        rated,
      })
      setRequestId(res.requestId)

      waitTimer.current = window.setInterval(() => setWaitSec((s) => s + 1), 1000)

      pollTimer.current = window.setInterval(async () => {
        try {
          const st = await getStatus(res.requestId)
          if (st.status === 'MATCHED' && st.gameId) {
            cleanupTimers()
            nav(`/game/${st.gameId}`)
          } else if (st.status === 'CANCELLED' || st.status === 'EXPIRED') {
            cleanupTimers()
            setSearching(false)
            setRequestId(null)
            setError(`Matchmaking ${st.status.toLowerCase()}`)
          }
        } catch (e: unknown) {
          // keep polling; show brief error
          setError(getErrorMessage(e) || 'Failed to poll matchmaking status')
        }
      }, 2000)
    } catch (e: unknown) {
      cleanupTimers()
      setSearching(false)
      setRequestId(null)
      setError(getErrorMessage(e) || 'Failed to start matchmaking')
    }
  }

  function cleanupTimers() {
    if (pollTimer.current) window.clearInterval(pollTimer.current)
    if (waitTimer.current) window.clearInterval(waitTimer.current)
    pollTimer.current = null
    waitTimer.current = null
  }

  async function cancelSearch() {
    if (!requestId) return
    try {
      await leaveMatchmaking({ requestId })
    } catch {
      // best-effort
    } finally {
      cleanupTimers()
      setSearching(false)
      setRequestId(null)
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
      <Card>
        <div className="text-lg font-semibold">Play online</div>
        <div className="mt-1 text-sm text-slate-300">Pick a time control and find an opponent.</div>

        <div className="mt-6 grid gap-4">
          <div>
            <div className="text-xs font-medium text-slate-300">Mode</div>
            <div className="mt-2 flex flex-wrap gap-2">
              {MODES.map((m) => (
                <Button
                  key={m.key}
                  variant={m.key === modeKey ? 'primary' : 'secondary'}
                  onClick={() => setModeKey(m.key)}
                  type="button"
                >
                  {m.label}
                </Button>
              ))}
            </div>
          </div>

          <div>
            <div className="text-xs font-medium text-slate-300">Preset</div>
            <div className="mt-2 flex flex-wrap gap-2">
              {presets.map((p) => (
                <Button
                  key={p.label}
                  variant={p.label === preset.label ? 'primary' : 'secondary'}
                  onClick={() => setPreset(p)}
                  type="button"
                >
                  {p.label}
                </Button>
              ))}
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-200 cursor-pointer">
            <input type="checkbox" checked={rated} onChange={(e) => setRated(e.target.checked)} />
            Rated
          </label>

          {error ? <div className="text-sm text-red-400">{error}</div> : null}

          {!searching ? (
            <Button className="w-full" onClick={startSearch}>
              Play
            </Button>
          ) : (
            <div className="rounded-lg border border-slate-800 bg-slate-950 p-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-medium">Searching opponent…</div>
                  <div className="text-sm text-slate-400">Waiting: {waitSec}s</div>
                </div>
                <Button variant="danger" onClick={cancelSearch}>
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </div>
      </Card>

      <Card>
        <div className="text-lg font-semibold">You</div>
        <div className="mt-2 text-sm text-slate-300">
          <div>
            <span className="text-slate-400">Username:</span> {me?.username || '—'}
          </div>
          <div className="mt-1">
            <span className="text-slate-400">User ID:</span> <span className="font-mono">{me?.userId || '—'}</span>
          </div>
        </div>
      </Card>
    </div>
  )
}

