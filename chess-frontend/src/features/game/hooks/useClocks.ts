import { useEffect, useReducer } from 'react'

type ClockState = {
  localWhiteMs: number | null
  localBlackMs: number | null
}

type ClockAction =
  | {
      type: 'sync'
      whiteMs: number | null | undefined
      blackMs: number | null | undefined
    }
  | {
      type: 'tick'
      sideToMove: string | null
    }

function clocksReducer(state: ClockState, action: ClockAction): ClockState {
  if (action.type === 'sync') {
    return {
      localWhiteMs: action.whiteMs ?? state.localWhiteMs,
      localBlackMs: action.blackMs ?? state.localBlackMs,
    }
  }

  const side = (action.sideToMove || '').toUpperCase()
  if (side === 'WHITE' && typeof state.localWhiteMs === 'number') {
    return { ...state, localWhiteMs: Math.max(0, state.localWhiteMs - 1000) }
  }
  if (side === 'BLACK' && typeof state.localBlackMs === 'number') {
    return { ...state, localBlackMs: Math.max(0, state.localBlackMs - 1000) }
  }

  return state
}

export function useClocks(
  whiteMs: number | null | undefined,
  blackMs: number | null | undefined,
  status: string | null,
  sideToMove: string | null
) {
  const [state, dispatch] = useReducer(clocksReducer, {
    localWhiteMs: whiteMs ?? null,
    localBlackMs: blackMs ?? null,
  })

  useEffect(() => {
    dispatch({ type: 'sync', whiteMs, blackMs })
  }, [whiteMs, blackMs])

  useEffect(() => {
    if (status !== 'RUNNING') return
    const interval = window.setInterval(() => {
      dispatch({ type: 'tick', sideToMove })
    }, 1000)
    return () => window.clearInterval(interval)
  }, [status, sideToMove])

  return state
}
