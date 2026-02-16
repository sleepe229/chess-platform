export type WsStatus = 'connected' | 'reconnecting' | 'disconnected'

export type UnknownWsServerMessage = {
  type: 'UNKNOWN'
  rawType: string | null
  raw: Record<string, unknown>
}

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
  | UnknownWsServerMessage

type Handlers = {
  onStatus?: (s: WsStatus) => void
  onMessage?: (msg: WsServerMessage) => void
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

function asOptionalString(v: unknown): string | null | undefined {
  if (v === undefined) return undefined
  if (v === null) return null
  return String(v)
}

function asOptionalNumber(v: unknown): number | undefined {
  if (v === undefined || v === null) return undefined
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : undefined
}

function parseWsServerMessage(raw: unknown): WsServerMessage | null {
  if (!isRecord(raw)) return null
  const t = raw.type
  if (typeof t !== 'string') return null

  if (t === 'GAME_STATE') {
    const movesRaw = Array.isArray(raw.moves) ? raw.moves : undefined
    const moves = movesRaw
      ? movesRaw
          .filter(isRecord)
          .map((m) => ({
            ply: Number(m.ply ?? 0),
            uci: String(m.uci ?? ''),
            san: asOptionalString(m.san) ?? undefined,
          }))
      : undefined

    const clocksRaw = isRecord(raw.clocks) ? raw.clocks : null
    const clocks =
      clocksRaw && asOptionalNumber(clocksRaw.whiteMs) !== undefined && asOptionalNumber(clocksRaw.blackMs) !== undefined
        ? { whiteMs: asOptionalNumber(clocksRaw.whiteMs)!, blackMs: asOptionalNumber(clocksRaw.blackMs)! }
        : undefined

    return {
      type: 'GAME_STATE',
      gameId: String(raw.gameId ?? ''),
      whiteId: String(raw.whiteId ?? ''),
      blackId: String(raw.blackId ?? ''),
      fen: String(raw.fen ?? ''),
      moves,
      clocks,
      status: asOptionalString(raw.status),
      sideToMove: asOptionalString(raw.sideToMove),
    }
  }

  if (t === 'MOVE_ACCEPTED') {
    const clocksRaw = isRecord(raw.clocks) ? raw.clocks : null
    const clocks =
      clocksRaw && asOptionalNumber(clocksRaw.whiteMs) !== undefined && asOptionalNumber(clocksRaw.blackMs) !== undefined
        ? { whiteMs: asOptionalNumber(clocksRaw.whiteMs)!, blackMs: asOptionalNumber(clocksRaw.blackMs)! }
        : undefined

    return {
      type: 'MOVE_ACCEPTED',
      gameId: String(raw.gameId ?? ''),
      clientMoveId: asOptionalString(raw.clientMoveId),
      ply: raw.ply === undefined ? undefined : raw.ply === null ? null : Number(raw.ply),
      fen: String(raw.fen ?? ''),
      clocks,
    }
  }

  if (t === 'MOVE_REJECTED') {
    return {
      type: 'MOVE_REJECTED',
      gameId: String(raw.gameId ?? ''),
      clientMoveId: asOptionalString(raw.clientMoveId),
      reason: String(raw.reason ?? ''),
    }
  }

  if (t === 'GAME_FINISHED') {
    return {
      type: 'GAME_FINISHED',
      gameId: String(raw.gameId ?? ''),
      result: String(raw.result ?? ''),
      reason: String(raw.reason ?? ''),
    }
  }

  return { type: 'UNKNOWN', rawType: t, raw }
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
        const parsed = JSON.parse(String(ev.data)) as unknown
        const msg = parseWsServerMessage(parsed)
        if (msg) this.handlers.onMessage?.(msg)
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

