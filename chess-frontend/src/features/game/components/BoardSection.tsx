import { Chessboard, type PieceDropHandlerArgs } from 'react-chessboard'
import { STARTING_FEN } from '../gameApi'
import type { WsStatus } from '../../../shared/ws/GameWsClient'

type BoardSectionProps = {
  fen: string
  boardOrientation: 'white' | 'black'
  allowDragging: boolean
  wsStatus: WsStatus
  status: string | null
  result: string | null
  finishReason: string | null
  sideToMove: string | null
  error: string | null
  onPieceDrop: (source: string, target: string) => boolean
}

export function BoardSection({
  fen,
  boardOrientation,
  allowDragging,
  wsStatus,
  status,
  result,
  finishReason,
  sideToMove,
  error,
  onPieceDrop,
}: BoardSectionProps) {
  const statusText =
    status === 'FINISHED'
      ? `Finished: ${result ?? ''} (${finishReason ?? ''})`
      : sideToMove
        ? `${sideToMove} to move`
        : ''

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        {wsStatus !== 'connected' ? (
          <div className="text-sm">
            <span className={wsStatus === 'reconnecting' ? 'text-amber-400' : 'text-red-400'}>
              {wsStatus === 'reconnecting' ? 'Reconnecting…' : 'Connection lost. Reconnecting…'}
            </span>
          </div>
        ) : null}
        <div className="text-sm text-slate-300">{statusText}</div>
      </div>

      <div className="mx-auto w-full max-w-[720px]">
        <Chessboard
          options={{
            position: fen || STARTING_FEN,
            boardOrientation,
            allowDragging,
            boardStyle: {
              borderRadius: '8px',
              boxShadow: '0 0 0 1px rgba(148, 163, 184, 0.15)',
            },
            onPieceDrop: ({ sourceSquare, targetSquare }: PieceDropHandlerArgs) => {
              if (!targetSquare) return false
              return onPieceDrop(sourceSquare, targetSquare)
            },
          }}
        />
      </div>

      {error ? <div className="text-sm text-red-400">{error}</div> : null}
    </div>
  )
}
