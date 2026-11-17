import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { formatNpr } from '../../utils/format'

export default function AdminDashboard() {
  const [stats, setStats] = useState({ users: 0, courses: 0, pending: 0, revenue: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const load = async () => {
      try {
        const [users, courses, pending, revenue] = await Promise.all([
          api.get('/api/admin/users'),
          api.get('/api/courses'),
          api.get('/api/admin/courses/pending'),
          api.get('/api/admin/revenue'),
        ])

        if (!active) return

        setStats({
          users: (users.data?.data || []).length,
          courses: (courses.data?.data || []).length,
          pending: (pending.data?.data || []).length,
          revenue: Number(revenue.data?.data || 0),
        })
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load platform statistics.')
      } finally {
        if (active) setLoading(false)
      }
    }

    load()

    return () => {
      active = false
    }
  }, [])

  const cards = [
    { label: 'Total users', value: stats.users },
    { label: 'Published courses', value: stats.courses },
    { label: 'Pending reviews', value: stats.pending },
    { label: 'Revenue', value: formatNpr(stats.revenue) },
  ]
  const quickLinks = [
    { label: 'Review course approvals', to: '/admin/courses' },
    { label: 'Manage platform users', to: '/admin/users' },
    { label: 'Manage catalog categories', to: '/admin/categories' },
    { label: 'Review payment records', to: '/admin/payments' },
  ]
  return (
    <DashboardLayout
      role="ADMIN"
      title="Administration center"
      subtitle="Review platform health, approvals, and operational metrics."
    >
      {error && <p className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {loading && <p className="mb-5 text-slate-600">Loading platform statistics...</p>}
      {!loading && !error && (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {cards.map((item) => (
          <div key={item.label} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <p className="text-sm text-slate-500">{item.label}</p>
            <p className="mt-3 text-3xl font-bold text-slate-900">{item.value}</p>
          </div>
        ))}
      </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
          <h2 className="text-xl font-bold text-slate-900">Platform activity</h2>
          <div className="mt-5 space-y-4 text-sm text-slate-600">
            <Link to="/admin/courses" className="block rounded-xl bg-white p-4 ring-1 ring-slate-200"><p className="font-semibold text-slate-900">Pending course approvals</p><p className="mt-1">{stats.pending} courses are waiting for review.</p></Link>
            <Link to="/admin/users" className="block rounded-xl bg-white p-4 ring-1 ring-slate-200"><p className="font-semibold text-slate-900">Registered users</p><p className="mt-1">Manage {stats.users} platform accounts.</p></Link>
            <Link to="/admin/payments" className="block rounded-xl bg-white p-4 ring-1 ring-slate-200"><p className="font-semibold text-slate-900">Payment records</p><p className="mt-1">Review transactions and platform revenue.</p></Link>
          </div>
        </div>

        <div className="rounded-2xl bg-slate-900 p-5 text-slate-50">
          <h2 className="text-xl font-bold">Quick actions</h2>
          <ul className="mt-5 space-y-3">
            {quickLinks.map((item) => (
              <li key={item.to} className="rounded-xl bg-slate-800 p-3 text-sm text-slate-200">
                <Link to={item.to} className="block">{item.label}</Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </DashboardLayout>
  )
}
