import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'
import api from '../services/api'
import { formatNpr, initialsOf } from '../utils/format'

function ReceiptRow({ label, value, valueClass = 'text-white', mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 border-t border-white/10 py-3">
      <span className="text-sm text-slate-400">{label}</span>
      <span
        className={`text-right text-sm font-semibold ${valueClass} ${mono ? 'font-mono tracking-tight' : ''}`}
      >
        {value}
      </span>
    </div>
  )
}

export default function PaymentSuccessPage() {
  const { paymentId } = useParams()
  const navigate = useNavigate()

  const [payment, setPayment] = useState(null)
  const [course, setCourse] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const instructor = useMemo(() => {
    const first = course?.instructors?.[0]
    if (!first) return null

    return {
      name: `${first.firstName || ''} ${first.lastName || ''}`.trim() || first.username || 'Course instructor',
      initials: initialsOf(`${first.firstName || ''} ${first.lastName || ''}`),
    }
  }, [course])

  const courseId = payment?.courseId ?? course?.id
  const isPaid = payment?.paymentStatus === 'SUCCESS'

  useEffect(() => {
    let active = true

    const loadReceipt = async () => {
      try {
        const paymentResponse = await api.get(`/api/student/payments/${paymentId}`)
        const paymentData = paymentResponse.data?.data

        if (!active) return
        setPayment(paymentData)

        if (paymentData?.courseId) {
          const courseResponse = await api.get(`/api/courses/${paymentData.courseId}`)
          if (active) setCourse(courseResponse.data?.data || null)
        }
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load this receipt.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadReceipt()

    return () => {
      active = false
    }
  }, [paymentId])

  return (
    <main className="portal-frame flex min-h-screen flex-col">
      <SiteNavbar />

      <div className="flex-1 px-6 py-12">
        <div className="mx-auto max-w-3xl">
          <div className="rounded-3xl border border-white/10 bg-[#0b1628] p-7 shadow-2xl shadow-black/30">
            <div className="flex flex-col items-center text-center">
              <div
                className={`flex h-16 w-16 items-center justify-center rounded-full text-3xl ${
                  isPaid ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'
                }`}
              >
                {isPaid ? '✓' : '!'}
              </div>
              <p className="mt-6 text-sm font-bold uppercase tracking-[0.28em] text-[#f45135]">
                Payment receipt
              </p>
              <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-white">
                {isPaid ? 'You are enrolled' : 'Payment not completed'}
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-400">
                {isPaid
                  ? 'Thanks for your purchase. Every lesson, assignment and certificate in this course is now unlocked.'
                  : 'This payment is not marked as successful, so no enrollment was created. Browse the catalog and try enrolling again.'}
              </p>
            </div>

            {loading && (
              <div className="mt-8 flex flex-col items-center gap-4 py-6">
                <div className="h-9 w-9 animate-spin rounded-full border-4 border-white/10 border-t-[#f45135]" aria-hidden="true" />
                <p className="text-sm text-slate-400">Loading your receipt...</p>
              </div>
            )}

            {!loading && error && (
              <p className="mt-8 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">
                {error}
              </p>
            )}

            {!loading && !error && payment && (
              <>
                <div className="mt-8 flex gap-4">
                  <div className="h-20 w-20 shrink-0 rounded-2xl bg-gradient-to-br from-teal-500 via-cyan-500 to-indigo-600" />
                  <div className="min-w-0">
                    <p className="text-xs font-bold uppercase tracking-[0.18em] text-teal-300">
                      {course?.category?.name || 'Course'}
                    </p>
                    <p className="mt-2 text-lg font-bold leading-snug text-white">
                      {payment.courseTitle || course?.title}
                    </p>
                    {instructor && (
                      <p className="mt-2 flex items-center gap-2 text-sm text-slate-400">
                        <span
                          aria-hidden="true"
                          className="flex h-6 w-6 items-center justify-center rounded-full bg-[#fff5d6] text-[0.6rem] font-extrabold text-[#0f172a]"
                        >
                          {instructor.initials}
                        </span>
                        Taught by {instructor.name}
                      </p>
                    )}
                  </div>
                </div>

                <div className="mt-7">
                  <ReceiptRow label="Amount paid" value={formatNpr(payment.amount)} valueClass="text-[#fff5d6] text-base" />
                  <ReceiptRow label="Payment method" value={payment.paymentMethod} />
                  <ReceiptRow label="Transaction ID" value={payment.transactionId || '—'} mono />
                  <ReceiptRow label="Payment status" value={payment.paymentStatus} valueClass="text-emerald-300" />
                  <ReceiptRow
                    label="Paid at"
                    value={payment.paidAt ? new Date(payment.paidAt).toLocaleString() : '—'}
                  />
                  <ReceiptRow
                    label="Receipt number"
                    value={`#${payment.id}`}
                    mono
                  />
                </div>
              </>
            )}

            {!loading && (
              <div className="mt-8 space-y-3">
                {courseId && payment?.paymentStatus === 'SUCCESS' && (
                  <button
                    type="button"
                    onClick={() => navigate(`/student/courses/${courseId}/learn`)}
                    className="w-full rounded-xl bg-[#f45135] px-5 py-4 text-sm font-bold text-white shadow-lg shadow-[#f45135]/20 transition hover:bg-[#ff694d]"
                  >
                    Start learning
                  </button>
                )}
                <div className="grid gap-3 sm:grid-cols-2">
                  <Link
                    to="/student/courses"
                    className="rounded-xl border border-white/15 px-5 py-3 text-center text-sm font-semibold text-slate-300 transition hover:bg-white/5"
                  >
                    My courses
                  </Link>
                  <Link
                    to="/student/payments"
                    className="rounded-xl border border-white/15 px-5 py-3 text-center text-sm font-semibold text-slate-300 transition hover:bg-white/5"
                  >
                    Payment history
                  </Link>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <SiteFooter />
    </main>
  )
}
