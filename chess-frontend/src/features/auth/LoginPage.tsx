import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { login } from './authApi'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { Card } from '../../shared/ui/Card'
import { getErrorMessage } from '../../shared/utils/getErrorMessage'

export function LoginPage() {
  const nav = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const from = (location.state as { from?: string } | null)?.from ?? '/lobby'

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email.trim(), password)
      nav(from, { replace: true })
    } catch (e: unknown) {
      setError(getErrorMessage(e) || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4">
      <Card className="w-full max-w-md">
        <div className="text-xl font-semibold">Sign in</div>
        <div className="mt-1 text-sm text-slate-300">Play online chess — fast, minimal, reliable.</div>

        <form className="mt-6 space-y-3" onSubmit={onSubmit}>
          <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error ? <div className="text-sm text-red-400">{error}</div> : null}

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>

        <div className="mt-4 text-sm text-slate-300">
          No account? <Link to="/register">Create one</Link>
        </div>
      </Card>
    </div>
  )
}

