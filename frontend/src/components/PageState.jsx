export function LoadingState({ label = 'Loading...' }) {
  return (
    <div className="flex min-h-48 flex-col items-center justify-center rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-teal-700" aria-hidden="true" />
      <p className="mt-4 text-sm font-medium text-slate-600">{label}</p>
    </div>
  )
}

export function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center">
      <p className="font-semibold text-red-900">We could not load this section.</p>
      <p className="mt-2 text-sm text-red-700">{message}</p>
      {onRetry && <button type="button" onClick={onRetry} className="mt-5 rounded-full bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800">Try again</button>}
    </div>
  )
}

export function EmptyState({ title, message, action }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-50 text-xl text-teal-700" aria-hidden="true">+</div>
      <h2 className="mt-4 text-xl font-bold text-slate-900">{title}</h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">{message}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
