import { useEffect } from 'react'
import { useToastStore } from '../toast/toastStore'

const AUTO_DISMISS_MS = 5000

export function ToastBar() {
  const { toasts, remove } = useToastStore()

  return (
    <div
      className="fixed right-[max(1rem,env(safe-area-inset-right))] top-[max(1rem,env(safe-area-inset-top))] z-[100] flex flex-col gap-2 max-w-[min(24rem,calc(100vw-2rem))] pointer-events-none"
      aria-live="polite"
    >
      {toasts.map((t) => (
        <ToastItem key={t.id} item={t} onDismiss={() => remove(t.id)} />
      ))}
    </div>
  )
}

function ToastItem({
  item,
  onDismiss,
}: {
  item: { id: string; message: string; type: string }
  onDismiss: () => void
}) {
  useEffect(() => {
    const t = setTimeout(onDismiss, AUTO_DISMISS_MS)
    return () => clearTimeout(t)
  }, [onDismiss])

  const bg =
    item.type === 'error'
      ? 'bg-red-900/95 border-red-700'
      : item.type === 'success'
        ? 'bg-emerald-900/95 border-emerald-700'
        : 'bg-slate-800/95 border-slate-600'

  return (
    <div
      className={`pointer-events-auto rounded-lg border px-4 py-3 shadow-lg ${bg} text-slate-100 text-sm flex items-start justify-between gap-3`}
      role="alert"
    >
      <span>{item.message}</span>
      <button
        type="button"
        onClick={onDismiss}
        className="shrink-0 text-slate-400 hover:text-slate-200"
        aria-label="Dismiss"
      >
        ×
      </button>
    </div>
  )
}
