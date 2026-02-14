import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthGate } from './features/auth/AuthGate'
import { LoginPage } from './features/auth/LoginPage'
import { RegisterPage } from './features/auth/RegisterPage'
import { AppLayout } from './shared/layout/AppLayout'
import { RequireAuth } from './features/auth/RequireAuth'
import { LobbyPage } from './features/lobby/LobbyPage'
import { ProfilePage } from './features/profile/ProfilePage'
import { GamePage } from './features/game/GamePage'
import { GamesPage } from './features/history/GamesPage'
import { GameReviewPage } from './features/history/GameReviewPage'
import { ErrorBoundary } from './shared/ErrorBoundary'

export default function App() {
  return (
    <AuthGate>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/lobby" replace />} />

          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            path="/lobby"
            element={
              <RequireAuth>
                <LobbyPage />
              </RequireAuth>
            }
          />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage />
              </RequireAuth>
            }
          />
          <Route
            path="/game/:gameId"
            element={
              <RequireAuth>
                <ErrorBoundary>
                  <GamePage />
                </ErrorBoundary>
              </RequireAuth>
            }
          />
          <Route
            path="/games"
            element={
              <RequireAuth>
                <GamesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/games/:gameId"
            element={
              <RequireAuth>
                <GameReviewPage />
              </RequireAuth>
            }
          />

          <Route path="*" element={<Navigate to="/lobby" replace />} />
        </Route>
      </Routes>
    </AuthGate>
  )
}
