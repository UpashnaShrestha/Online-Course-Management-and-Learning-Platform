import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { formatNpr } from '../../utils/format'

export default function StudentPayments() {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    api
      .get('/api/student/payments')
      .then((response) => {
        if (active) setPayments(response.data?.data || [])
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Unable to load payment history.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <DashboardLayout role="STUDENT" title="Payment history" subtitle="Review your course payment records and statuses.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="mb-4 text-slate-600">Loading payment history...</p>}
      {!loading && (
      <div className="overflow-x-auto rounded-2xl border border-slate-200">
        <table className="min-w-full text-left text-sm text-slate-700">
          <thead className="bg-slate-100"><tr><th scope="col" className="px-4 py-3">Transaction</th><th scope="col" className="px-4 py-3">Amount</th><th scope="col" className="px-4 py-3">Method</th><th scope="col" className="px-4 py-3">Status</th><th scope="col" className="px-4 py-3">Date</th></tr></thead>
          <tbody>
            {payments.map((payment) => <tr key={payment.id} className="border-t border-slate-200"><td className="px-4 py-3">{payment.transactionId || `Payment #${payment.id}`}</td><td className="px-4 py-3">{formatNpr(payment.amount)}</td><td className="px-4 py-3">{payment.paymentMethod}</td><td className="px-4 py-3">{payment.paymentStatus}</td><td className="px-4 py-3">{payment.createdAt ? new Date(payment.createdAt).toLocaleDateString() : ''}</td></tr>)}
          </tbody>
        </table>
        {payments.length === 0 && !error && <p className="p-6 text-slate-600">No payment records yet.</p>}
      </div>
      )}
    </DashboardLayout>
  )
}
