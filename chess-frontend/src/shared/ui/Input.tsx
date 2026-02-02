import clsx from 'clsx'

export function Input({
  label,
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="block">
      <div className="text-xs font-medium text-slate-300">{label}</div>
      <input
        className={clsx(
          'mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 outline-none focus:ring-2 focus:ring-sky-500/60',
          className,
        )}
        {...props}
      />
    </label>
  )
}

