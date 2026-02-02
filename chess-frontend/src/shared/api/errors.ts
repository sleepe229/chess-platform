export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly traceId?: string
  readonly details?: unknown

  constructor(opts: { status: number; message: string; code?: string; traceId?: string; details?: unknown }) {
    super(opts.message)
    this.name = 'ApiError'
    this.status = opts.status
    this.code = opts.code
    this.traceId = opts.traceId
    this.details = opts.details
  }
}

