import { create } from 'zustand'

export type ToastItem = {
  id: string
  message: string
  type: 'error' | 'info' | 'success'
  createdAt: number
}

type ToastState = {
  toasts: ToastItem[]
  add: (message: string, type?: ToastItem['type']) => void
  remove: (id: string) => void
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  add: (message, type = 'error') =>
    set((state) => ({
      toasts: [
        ...state.toasts,
        { id: crypto.randomUUID(), message, type, createdAt: Date.now() },
      ].slice(-10),
    })),
  remove: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}))
