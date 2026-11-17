import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell, CheckCheck } from 'lucide-react'
import { useNotifications } from '../context/NotificationsContext'

const relativeTime = (value) => {
  if (!value) return ''

  const seconds = Math.round((Date.now() - new Date(value).getTime()) / 1000)

  if (seconds < 60) return 'just now'
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`
  return `${Math.floor(seconds / 86400)}d ago`
}

/**
 * Bell icon with an unread-count badge that opens a dropdown panel of notifications.
 */
export default function NotificationBell({ role = 'STUDENT' }) {
  const { notifications, unreadCount, loading, markAsRead, markAllAsRead, refresh } =
    useNotifications()

  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)
  const navigate = useNavigate()

  const allNotificationsPath = `/${role.toLowerCase()}/notifications`

  // Close on outside click or Escape so the panel behaves like a real popover.
  useEffect(() => {
    if (!open) return undefined

    const onPointerDown = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)

    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const select = async (notification) => {
    if (!notification.read) {
      await markAsRead(notification.id)
    }
    setOpen(false)

    if (notification.link) {
      navigate(notification.link)
    } else {
      navigate(allNotificationsPath)
    }
  }

  const recent = notifications.slice(0, 8)

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => {
          setOpen((current) => !current)
          refresh({ silent: true })
        }}
        aria-expanded={open}
        aria-haspopup="dialog"
        aria-label={
          unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'
        }
        className="relative rounded-xl border border-slate-300 bg-white p-2.5 text-slate-700 transition hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
      >
        <Bell size={18} aria-hidden="true" />

        {unreadCount > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-rose-600 px-1 text-[0.65rem] font-bold text-white ring-2 ring-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Notifications"
          className="absolute right-0 z-50 mt-3 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl"
        >
          <div className="flex items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
            <div>
              <p className="text-sm font-bold text-slate-900">Notifications</p>
              <p className="text-xs text-slate-500">
                {unreadCount > 0 ? `${unreadCount} unread` : 'All caught up'}
              </p>
            </div>

            {unreadCount > 0 && (
              <button
                type="button"
                onClick={markAllAsRead}
                className="flex items-center gap-1.5 rounded-lg bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-100"
              >
                <CheckCheck size={13} aria-hidden="true" />
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto">
            {loading && <p className="px-4 py-6 text-center text-sm text-slate-500">Loading...</p>}

            {!loading && recent.length === 0 && (
              <p className="px-4 py-6 text-center text-sm text-slate-500">No notifications yet.</p>
            )}

            {!loading &&
              recent.map((notification) => (
                <button
                  key={notification.id}
                  type="button"
                  onClick={() => select(notification)}
                  className={`block w-full border-b border-slate-100 px-4 py-3 text-left transition last:border-b-0 hover:bg-slate-50 ${
                    notification.read ? '' : 'bg-rose-50/60'
                  }`}
                >
                  <div className="flex items-start gap-2.5">
                    {!notification.read && (
                      <span
                        aria-hidden="true"
                        className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-rose-600"
                      />
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-slate-900">{notification.title}</p>
                      <p className="mt-0.5 line-clamp-2 text-xs leading-5 text-slate-600">
                        {notification.message}
                      </p>
                      <p className="mt-1 text-[0.7rem] text-slate-400">
                        {relativeTime(notification.createdAt)}
                      </p>
                    </div>
                  </div>
                </button>
              ))}
          </div>

          <button
            type="button"
            onClick={() => {
              setOpen(false)
              navigate(allNotificationsPath)
            }}
            className="w-full border-t border-slate-200 bg-slate-50 px-4 py-2.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100"
          >
            View all notifications
          </button>
        </div>
      )}
    </div>
  )
}