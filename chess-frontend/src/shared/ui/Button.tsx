import clsx from 'clsx'

export function Button({
  className,
  variant = 'primary',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' }) {
  return (
    <button
      className={clsx(
        'inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition focus:outline-none focus:ring-2 focus:ring-sky-500/60 disabled:opacity-60 disabled:cursor-not-allowed',
        variant === 'primary' && 'bg-sky-500 hover:bg-sky-400 text-slate-950',
        variant === 'secondary' && 'bg-slate-800 hover:bg-slate-700 text-slate-50 border border-slate-700',
        variant === 'danger' && 'bg-red-500 hover:bg-red-400 text-slate-950',
        className,
      )}
      {...props}
    />
  )
}

