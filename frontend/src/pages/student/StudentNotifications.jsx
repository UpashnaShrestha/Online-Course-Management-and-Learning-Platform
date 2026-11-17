import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BellOff, CheckCheck } from 'lucide-react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import { useNotifications } from '../../context/NotificationsContext'

export default function StudentNotifications({ role = 'STUDENT' }) {
  const { notifications, unreadCount, loading, error, markAsRead, markAllAsRead } =
    useNotifications()

  const [filter, setFilter] = useState('ALL')

  const visible = notifications.filter((item) => {
    if (filter === 'UNREAD') return !item.read
    if (filter === 'READ') return item.read
    return true
  })

  const subtitles = {
    STUDENT: 'Stay up to date with your learning activity.',
    INSTRUCTOR: 'Course approvals, submissions and grading updates.',
    ADMIN: 'Platform activity and operational alerts.',
  }

  return (
    <DashboardLayout
      role={role}
      title="Notifications"
      subtitle={subtitles[role] || subtitles.STUDENT}
    >
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          {[
            { key: 'ALL', label: `All (${notifications.length})` },
            { key: 'UNREAD', label: `Unread (${unreadCount})` },
            { key: 'READ', label: 'Read' },
          ].map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilter(tab.key)}
              aria-pressed={filter === tab.key}
              className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                filter === tab.key
                  ? 'bg-[#0f172a] text-white'
                  : 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            onClick={markAllAsRead}
            className="flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
          >
            <CheckCheck size={15} aria-hidden="true" />
            Mark all as read
          </button>
        )}
      </div>

      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="text-slate-600">Loading notifications...</p>}

      {!loading && notifications.length === 0 && (
        <div className="rounded-2xl bg-slate-50 p-10 text-center ring-1 ring-slate-200">
          <BellOff size={32} aria-hidden="true" className="mx-auto text-slate-400" />
          <h2 className="mt-4 text-xl font-bold text-slate-900">No notifications yet</h2>
          <p className="mx-auto mt-2 max-w-md text-sm text-slate-600">
            Enroll in a course, submit an assignment or get graded and updates will appear here.
          </p>
          <Link
            to="/courses"
            className="mt-5 inline-block rounded-lg bg-[#0f172a] px-4 py-2 text-sm font-semibold text-white hover:bg-[#263f7a]"
          >
            Browse courses
          </Link>
        </div>
      )}

      {!loading && notifications.length > 0 && visible.length === 0 && (
        <p className="rounded-2xl bg-slate-50 p-6 text-center text-slate-600 ring-1 ring-slate-200">
          No {filter.toLowerCase()} notifications.
        </p>
      )}

      <ul className="space-y-3">
        {visible.map((notification) => (
          <li
            key={notification.id}
            className={`rounded-2xl p-5 ring-1 transition ${
              notification.read
                ? 'bg-white ring-slate-200'
                : 'bg-rose-50/70 ring-rose-200'
            }`}
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  {!notification.read && (
                    <span
                      aria-label="Unread"
                      className="h-2 w-2 shrink-0 rounded-full bg-rose-600"
                    />
                  )}
                  <p className="font-bold text-slate-900">{notification.title}</p>
                  {notification.type && (
                    <span className="rounded-full bg-white px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wider text-slate-600 ring-1 ring-slate-200">
                      {notification.type}
                    </span>
                  )}
                </div>

                <p className="mt-1.5 text-sm leading-6 text-slate-600">{notification.message}</p>

                <p className="mt-2 text-xs text-slate-500">
                  {notification.createdAt
                    ? new Date(notification.createdAt).toLocaleString()
                    : ''}
                </p>
              </div>

              <div className="flex shrink-0 gap-2">
                {notification.link && (
                  <Link
                    to={notification.link}
                    onClick={() => !notification.read && markAsRead(notification.id)}
                    className="rounded-lg bg-[#0f172a] px-3 py-2 text-xs font-semibold text-white transition hover:bg-[#263f7a]"
                  >
                    Open
                  </Link>
                )}

                {!notification.read && (
                  <button
                    type="button"
                    onClick={() => markAsRead(notification.id)}
                    className="rounded-lg bg-white px-3 py-2 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
                  >
                    Mark read
                  </button>
                )}
              </div>
            </div>
          </li>
        ))}
      </ul>
    </DashboardLayout>
  )
}