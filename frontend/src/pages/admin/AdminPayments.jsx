import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { formatNpr } from '../../utils/format'

export default function AdminPayments() {
  const [payments, setPayments] = useState([])
  const [revenue, setRevenue] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    Promise.all([api.get('/api/admin/payments'), api.get('/api/admin/revenue')])
      .then(([paymentsResponse, revenueResponse]) => {
        if (!active) return
        setPayments(paymentsResponse.data?.data || [])
        setRevenue(Number(revenueResponse.data?.data || 0))
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Unable to load payment data.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <DashboardLayout role="ADMIN" title="Payments and revenue" subtitle="Review platform transactions and successful revenue.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="mb-4 text-slate-600">Loading payment data...</p>}
      {!loading && (
      <>
      <div className="mb-6 rounded-2xl bg-slate-900 p-5 text-white"><p className="text-sm text-slate-300">Successful revenue</p><p className="mt-2 text-4xl font-bold">{formatNpr(revenue)}</p></div>
      <div className="overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm text-slate-700"><thead className="bg-slate-100"><tr><th scope="col" className="px-4 py-3">Transaction</th><th scope="col" className="px-4 py-3">Course</th><th scope="col" className="px-4 py-3">Amount</th><th scope="col" className="px-4 py-3">Method</th><th scope="col" className="px-4 py-3">Status</th><th scope="col" className="px-4 py-3">Date</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id} className="border-t border-slate-200"><td className="px-4 py-3">{payment.transactionId || `Payment #${payment.id}`}</td><td className="px-4 py-3">#{payment.courseId}</td><td className="px-4 py-3">{formatNpr(payment.amount)}</td><td className="px-4 py-3">{payment.paymentMethod}</td><td className="px-4 py-3">{payment.paymentStatus}</td><td className="px-4 py-3">{payment.createdAt ? new Date(payment.createdAt).toLocaleDateString() : ''}</td></tr>)}</tbody></table>{payments.length === 0 && !error && <p className="p-6 text-slate-600">No payment records found.</p>}</div>
      </>
      )}
    </DashboardLayout>
  )
}
