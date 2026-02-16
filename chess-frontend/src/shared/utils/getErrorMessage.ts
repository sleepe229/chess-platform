import { ApiError } from '../api/errors'

type GetErrorMessageOptions = {
  /** Append traceId when available (e.g. for support). Default false. */
  includeTraceId?: boolean
}

export function getErrorMessage(e: unknown, options: GetErrorMessageOptions = {}): string {
  const { includeTraceId = false } = options
  if (e instanceof ApiError) {
    const msg = e.message
    return includeTraceId && e.traceId ? `${msg} (ref: ${e.traceId})` : msg
  }
  if (e instanceof Error) return e.message
  if (typeof e === 'string') return e
  try {
    return JSON.stringify(e)
  } catch {
    return 'Something went wrong'
  }
}

