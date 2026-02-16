export type WsStatus = 'connected' | 'reconnecting' | 'disconnected'

export type WsServerMessage =
  | {
      type: 'GAME_STATE'
      gameId: string
      whiteId: string
      blackId: string
      fen: string
      moves?: { ply: number; uci: string; san?: string | null }[]
      clocks?: { whiteMs: number; blackMs: number }
      status?: string | null
      sideToMove?: string | null
      drawOfferedBy?: string | null
    }
  | {
      type: 'MOVE_ACCEPTED'
      gameId: string
      clientMoveId?: string | null
      ply?: number | null
      fen: string
      clocks?: { whiteMs: number; blackMs: number }
    }
  | { type: 'MOVE_REJECTED'; gameId: string; clientMoveId?: string | null; reason: string }
  | { type: 'GAME_FINISHED'; gameId: string; result: string; reason: string }
  | ({ type: string } & Record<string, unknown>)

type Handlers = {
  onStatus?: (s: WsStatus) => void
  onMessage?: (msg: WsServerMessage) => void
}

function wsBaseUrl(): string {
  const v = import.meta.env.VITE_WS_BASE_URL
  if (v && v.trim().length > 0) return v.trim().replace(/\/$/, '')
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${location.host}`
}

const BACKOFF_MS = [1000, 2000, 5000, 10000, 30000]

export class GameWsClient {
  private ws: WebSocket | null = null
  private closedByClient = false
  private reconnectAttempt = 0
  private queue: string[] = []
  private handlers: Handlers
  private gameId: string | null = null
  private token: string | null = null
  private lastSeenPly: number | null = null

  constructor(handlers: Handlers = {}) {
    this.handlers = handlers
  }

  connect(opts: { gameId: string; token: string; lastSeenPly?: number | null }) {
    this.closedByClient = false
    this.gameId = opts.gameId
    this.token = opts.token
    this.lastSeenPly = opts.lastSeenPly ?? null
    this.open()
  }

  close() {
    this.closedByClient = true
    this.handlers.onStatus?.('disconnected')
    try {
      this.ws?.close()
    } catch {
      // ignore
    } finally {
      this.ws = null
    }
  }

  send(json: unknown) {
    const payload = JSON.stringify(json)
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(payload)
      return
    }
    this.queue.push(payload)
  }

  private open() {
    if (!this.gameId || !this.token) return

    this.handlers.onStatus?.(this.reconnectAttempt === 0 ? 'connected' : 'reconnecting')

    const url = `${wsBaseUrl()}/ws/game/${encodeURIComponent(this.gameId)}?token=${encodeURIComponent(this.token)}`
    const ws = new WebSocket(url)
    this.ws = ws

    ws.onopen = () => {
      this.reconnectAttempt = 0
      this.handlers.onStatus?.('connected')
      // drain queue first
      for (const msg of this.queue.splice(0)) {
        ws.send(msg)
      }
      // SYNC
      this.send({
        type: 'SYNC',
        gameId: this.gameId,
        lastSeenPly: this.lastSeenPly ?? 0,
      })
    }

    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(String(ev.data)) as WsServerMessage
        this.handlers.onMessage?.(msg)
      } catch {
        // ignore malformed
      }
    }

    ws.onerror = () => {
      // websocket errors are followed by close; no-op
    }

    ws.onclose = () => {
      if (this.closedByClient) return
      this.handlers.onStatus?.('reconnecting')
      const wait = BACKOFF_MS[Math.min(this.reconnectAttempt, BACKOFF_MS.length - 1)]!
      this.reconnectAttempt += 1
      window.setTimeout(() => this.open(), wait)
    }
  }
}

