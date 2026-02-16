import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register, getOAuth2LoginUrl } from './authApi'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { Card } from '../../shared/ui/Card'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

export function RegisterPage() {
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await register(email.trim(), password)
      nav('/lobby', { replace: true })
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4">
      <Card className="w-full max-w-md">
        <div className="text-xl font-semibold">Create account</div>
        <div className="mt-1 text-sm text-slate-300">You’ll get JWT access + refresh tokens.</div>

        <form className="mt-6 space-y-3" onSubmit={onSubmit}>
          <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Input
            label="Password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error ? <div className="text-sm text-red-400">{error}</div> : null}

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? 'Creating…' : 'Create account'}
          </Button>

          <div className="relative my-4">
            <span className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-slate-600" />
            </span>
            <span className="relative flex justify-center text-xs text-slate-400">or</span>
          </div>
          <a
            href={getOAuth2LoginUrl('google')}
            className="flex items-center justify-center gap-2 w-full rounded-lg border border-slate-600 bg-slate-800/50 px-4 py-2.5 text-sm text-slate-200 hover:bg-slate-700/50 transition-colors"
          >
            Sign in with Google
          </a>
        </form>

        <div className="mt-4 text-sm text-slate-300">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </Card>
    </div>
  )
}

