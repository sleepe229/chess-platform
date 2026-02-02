import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '../../shared/ui/Card'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { useAuthStore } from '../../shared/auth/authStore'
import { getMe, getRatings, updateMe, type RatingDto } from './profileApi'
import { loadMe } from '../auth/authApi'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

const TIME_CONTROLS = ['BULLET', 'BLITZ', 'RAPID', 'CLASSICAL'] as const

export function ProfilePage() {
  const authMe = useAuthStore((s) => s.me)
  const setMe = useAuthStore((s) => s.setMe)

  const meQuery = useQuery({ queryKey: ['me'], queryFn: getMe })
  const userId = meQuery.data?.id || authMe?.userId

  const ratingsQuery = useQuery({
    queryKey: ['ratings', userId],
    queryFn: () => getRatings(userId!),
    enabled: Boolean(userId),
  })

  const [username, setUsername] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [country, setCountry] = useState('')
  const [bio, setBio] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!meQuery.data) return
    setUsername(meQuery.data.username ?? '')
    setAvatarUrl(meQuery.data.avatarUrl ?? '')
    setCountry(meQuery.data.country ?? '')
    setBio(meQuery.data.bio ?? '')
  }, [meQuery.data])

  const ratingsByType = useMemo(() => {
    const map = new Map<string, RatingDto>()
    for (const r of ratingsQuery.data?.ratings ?? []) {
      map.set(String(r.type).toUpperCase(), r)
    }
    return map
  }, [ratingsQuery.data])

  async function onSave() {
    setError(null)
    setSaving(true)
    try {
      await updateMe({
        username: username.trim() || undefined,
        avatarUrl: avatarUrl.trim() || undefined,
        country: country.trim() || undefined,
        bio: bio.trim() || undefined,
      })
      // refresh header profile
      await loadMe()
      setMe(useAuthStore.getState().me)
      await meQuery.refetch()
      await ratingsQuery.refetch()
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Failed to save profile')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
      <Card>
        <div className="text-lg font-semibold">Profile</div>
        <div className="mt-1 text-sm text-slate-300">Update your public profile fields.</div>

        <div className="mt-6 grid gap-3">
          <Input label="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
          <Input label="Avatar URL" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} />
          <Input label="Country" value={country} onChange={(e) => setCountry(e.target.value)} />

          <label className="block">
            <div className="text-xs font-medium text-slate-300">Bio</div>
            <textarea
              className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-50 outline-none focus:ring-2 focus:ring-sky-500/60"
              rows={4}
              value={bio}
              onChange={(e) => setBio(e.target.value)}
            />
          </label>

          {error ? <div className="text-sm text-red-400">{error}</div> : null}

          <Button onClick={onSave} disabled={saving} className="w-full">
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </div>
      </Card>

      <Card>
        <div className="text-lg font-semibold">Ratings</div>
        <div className="mt-1 text-sm text-slate-300">Bullet / Blitz / Rapid / Classical</div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          {TIME_CONTROLS.map((t) => {
            const r = ratingsByType.get(t)
            return (
              <div key={t} className="rounded-lg border border-slate-800 bg-slate-950 p-4">
                <div className="text-xs text-slate-400">{t}</div>
                <div className="mt-1 text-2xl font-semibold">{r ? Math.round(r.rating) : '—'}</div>
                <div className="mt-2 text-xs text-slate-400">
                  RD: {r ? Number(r.deviation).toFixed(1) : '—'} · Games: {r?.games_played ?? '—'}
                </div>
              </div>
            )
          })}
        </div>

        <div className="mt-4 text-xs text-slate-500">
          Data source: <span className="font-mono">GET /v1/users/&lt;id&gt;/ratings</span>
        </div>
      </Card>
    </div>
  )
}

