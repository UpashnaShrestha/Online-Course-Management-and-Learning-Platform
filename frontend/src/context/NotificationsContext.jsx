import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from './AuthContext'
import api from '../services/api'

const POLL_INTERVAL_MS = 15000
const TOAST_DURATION_MS = 6000

const NotificationsContext = createContext(null)

/**
 * Polls the notification endpoint and exposes:
 *  - the notification list and unread count (for the bell badge)
 *  - toasts: red pop-ups raised when a notification the user has not seen arrives
 *
 * Polling is used rather than a websocket so no extra infrastructure is required.
 */
export function NotificationsProvider({ children }) {
  const { isAuthenticated } = useAuth()

  const [notifications, setNotifications] = useState([])
  const [toasts, setToasts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Ids already shown as a toast, so re-polling never re-raises the same pop-up.
  const seenIdsRef = useRef(null)

  const dismissToast = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const refresh = useCallback(async ({ silent = true } = {}) => {
    try {
      const response = await api.get('/api/users/me/notifications')
      const incoming = response.data?.data || []

      if (seenIdsRef.current === null) {
        // First load: adopt existing notifications silently so the user is not
        // spammed with toasts for everything that happened while they were away.
        seenIdsRef.current = new Set(incoming.map((item) => item.id))
      } else {
        const fresh = incoming.filter((item) => !seenIdsRef.current.has(item.id))

        if (fresh.length > 0) {
          incoming.forEach((item) => seenIdsRef.current.add(item.id))

          // Newest first, and only the most recent few so the screen is not flooded.
          const raised = fresh.slice(0, 3).map((item) => ({
            id: item.id,
            title: item.title,
            message: item.message,
            link: item.link,
          }))

          setToasts((current) => [...raised, ...current].slice(0, 3))
        }
      }

      setNotifications(incoming)
      setError('')
    } catch (requestError) {
      // Never surface a polling failure as a blocking error: the badge simply stops updating.
      if (!silent) {
        setError(requestError.response?.data?.message || 'Unable to load notifications.')
      }
    } finally {
      setLoading(false)
    }
  }, [])

  const markAsRead = useCallback(async (id) => {
    // Optimistic: the row greys out immediately, and is corrected if the call fails.
    setNotifications((current) =>
      current.map((item) => (item.id === id ? { ...item, read: true } : item)),
    )

    try {
      await api.put(`/api/notifications/${id}/read`)
    } catch {
      refresh({ silent: true })
    }
  }, [refresh])

  const markAllAsRead = useCallback(async () => {
    setNotifications((current) => current.map((item) => ({ ...item, read: true })))

    try {
      await api.put('/api/users/me/notifications/read-all')
    } catch {
      refresh({ silent: true })
    }
  }, [refresh])

  useEffect(() => {
    // Never poll while signed out: the endpoint would 401, and the response
    // interceptor would treat that as an expired session and bounce the
    // anonymous visitor to /login.
    if (!isAuthenticated) {
      setNotifications([])
      setToasts([])
      seenIdsRef.current = null
      setLoading(false)
      return undefined
    }

    refresh({ silent: false })

    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        refresh({ silent: true })
      }
    }, POLL_INTERVAL_MS)

    return () => clearInterval(timer)
  }, [isAuthenticated, refresh])

  // Auto-dismiss each toast on its own timer. Keyed by toast id so a newly arrived
// toast does not restart the countdown of the ones already on screen.
  useEffect(() => {
    if (toasts.length === 0) return undefined

    const timers = toasts.map((toast) =>
      setTimeout(() => dismissToast(toast.id), TOAST_DURATION_MS),
    )

    return () => timers.forEach(clearTimeout)
  }, [toasts, dismissToast])

  const value = useMemo(
    () => ({
      notifications,
      unreadCount: notifications.filter((item) => !item.read).length,
      toasts,
      loading,
      error,
      refresh,
      markAsRead,
      markAllAsRead,
      dismissToast,
    }),
    [notifications, toasts, loading, error, refresh, markAsRead, markAllAsRead, dismissToast],
  )

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>
}

export function useNotifications() {
  const context = useContext(NotificationsContext)

  if (!context) {
    throw new Error('useNotifications must be used within a NotificationsProvider')
  }

  return context
}