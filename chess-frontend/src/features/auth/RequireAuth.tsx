import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../shared/auth/authStore'

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const tokens = useAuthStore((s) => s.tokens)
  const location = useLocation()

  if (!tokens?.accessToken) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}

