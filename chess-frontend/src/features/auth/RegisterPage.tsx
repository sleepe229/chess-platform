import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from './authApi'
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
        </form>

        <div className="mt-4 text-sm text-slate-300">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </Card>
    </div>
  )
}

