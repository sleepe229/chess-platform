import { Link } from 'react-router-dom'
import { Card } from '../../../shared/ui/Card'
import { Button } from '../../../shared/ui/Button'
import { ConfirmModal } from '../../../shared/ui/Modal'

type GameActionsProps = {
  status: string | null
  drawOfferedByOpponent: boolean
  onResign: () => void
  onOfferDraw: () => void
  onAcceptDraw: () => void
  confirmResign: boolean
  setConfirmResign: (v: boolean) => void
  confirmOfferDraw: boolean
  setConfirmOfferDraw: (v: boolean) => void
  confirmAcceptDraw: boolean
  setConfirmAcceptDraw: (v: boolean) => void
  myUsername: string
  boardOrientation: string
}

export function GameActions({
  status,
  drawOfferedByOpponent,
  onResign,
  onOfferDraw,
  onAcceptDraw,
  confirmResign,
  setConfirmResign,
  confirmOfferDraw,
  setConfirmOfferDraw,
  confirmAcceptDraw,
  setConfirmAcceptDraw,
  myUsername,
  boardOrientation,
}: GameActionsProps) {
  return (
    <>
      <ConfirmModal
        open={confirmResign}
        onClose={() => setConfirmResign(false)}
        onConfirm={onResign}
        title="Resign the game?"
        message="You will lose this game. This cannot be undone."
        confirmLabel="Resign"
        cancelLabel="Cancel"
        variant="danger"
      />
      <ConfirmModal
        open={confirmOfferDraw}
        onClose={() => setConfirmOfferDraw(false)}
        onConfirm={onOfferDraw}
        title="Offer a draw?"
        message="Your opponent will be able to accept or decline."
        confirmLabel="Offer draw"
        cancelLabel="Cancel"
      />
      <ConfirmModal
        open={confirmAcceptDraw}
        onClose={() => setConfirmAcceptDraw(false)}
        onConfirm={onAcceptDraw}
        title="Accept draw?"
        message="The game will end in a draw."
        confirmLabel="Accept"
        cancelLabel="Decline"
      />
      <Card>
        <div className="text-sm text-slate-400">You</div>
        <div className="mt-1 font-medium">{myUsername}</div>
        <div className="mt-2 text-xs text-slate-500">Color: {boardOrientation}</div>
      </Card>

      <Card className="space-y-2">
        {status === 'RUNNING' && (
          <>
            <Button
              variant="danger"
              className="w-full"
              onClick={() => setConfirmResign(true)}
              aria-label="Resign the game"
              type="button"
            >
              Resign
            </Button>
            <Button
              variant="secondary"
              className="w-full"
              onClick={() => setConfirmOfferDraw(true)}
              aria-label="Offer a draw to your opponent"
              type="button"
            >
              Offer draw
            </Button>
            {drawOfferedByOpponent && (
              <Button
                variant="secondary"
                className="w-full"
                onClick={() => setConfirmAcceptDraw(true)}
                aria-label="Accept draw offer"
                type="button"
              >
                Accept draw
              </Button>
            )}
          </>
        )}
        {status === 'FINISHED' && (
          <Link to="/lobby" className="block">
            <Button variant="primary" className="w-full" type="button">
              Back to Lobby
            </Button>
          </Link>
        )}
      </Card>
    </>
  )
}
