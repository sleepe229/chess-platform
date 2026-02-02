import clsx from 'clsx'

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={clsx('rounded-lg border border-slate-800 bg-slate-900 p-5', className)} {...props} />
}

