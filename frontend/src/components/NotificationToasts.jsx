import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { X } from 'lucide-react'
import { useNotifications } from '../context/NotificationsContext'

/**
 * Red pop-up stack shown when a new notification arrives. Each toast disappears on its
 * own after a few seconds; clicking one marks it read and opens the linked page.
 */
export default function NotificationToasts() {
  const { toasts, dismissToast, markAsRead } = useNotifications()
  const navigate = useNavigate()

  // A toast can be dismissed by keyboard without reaching for the mouse.
  useEffect(() => {
    if (toasts.length === 0) return undefined

    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        toasts.forEach((toast) => dismissToast(toast.id))
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [toasts, dismissToast])

  const open = async (toast) => {
    dismissToast(toast.id)
    await markAsRead(toast.id)

    if (toast.link) {
      navigate(toast.link)
    }
  }

  if (toasts.length === 0) {
    return null
  }

  return (
    <div
      className="pointer-events-none fixed right-4 top-20 z-[60] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-3"
      role="region"
      aria-label="New notifications"
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="alert"
          className="pointer-events-auto animate-[toast-in_.25s_ease-out] rounded-2xl border border-rose-300/40 bg-rose-600 p-4 text-white shadow-2xl shadow-rose-900/40"
        >
          <div className="flex items-start gap-3">
            <span
              aria-hidden="true"
              className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/20 text-sm font-bold"
            >
              !
            </span>

            <button
              type="button"
              onClick={() => open(toast)}
              className="min-w-0 flex-1 text-left"
            >
              <p className="text-sm font-bold">{toast.title}</p>
              <p className="mt-1 text-xs leading-5 text-rose-50">{toast.message}</p>
              {toast.link && (
                <p className="mt-2 text-[0.7rem] font-semibold uppercase tracking-wider text-rose-100">
                  Click to open
                </p>
              )}
            </button>

            <button
              type="button"
              onClick={() => dismissToast(toast.id)}
              aria-label={`Dismiss ${toast.title}`}
              className="shrink-0 rounded-lg p-1 text-rose-100 transition hover:bg-white/15"
            >
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}