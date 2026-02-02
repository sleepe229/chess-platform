import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthTokens, UserProfile } from './types'

type AuthState = {
  tokens: AuthTokens | null
  me: UserProfile | null
  bootstrapped: boolean

  setTokens: (tokens: AuthTokens | null) => void
  setMe: (me: UserProfile | null) => void
  logout: () => void
  markBootstrapped: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      tokens: null,
      me: null,
      bootstrapped: false,

      setTokens: (tokens) => set({ tokens }),
      setMe: (me) => set({ me }),
      logout: () => set({ tokens: null, me: null }),
      markBootstrapped: () => set({ bootstrapped: true }),
    }),
    {
      name: 'chess-auth',
      partialize: (s) => ({ tokens: s.tokens }),
    },
  ),
)

