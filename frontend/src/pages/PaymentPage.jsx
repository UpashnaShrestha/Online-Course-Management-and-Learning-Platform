import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'
import api from '../services/api'
import { formatNpr, initialsOf, PAYMENT_METHODS } from '../utils/format'

const PROCESS_STEPS = [
  'Contacting payment gateway',
  'Verifying the transaction',
  'Confirming the payment',
  'Creating your enrollment',
]

function Stepper({ stage }) {
  const stages = ['payment', 'processing', 'success']
  const current = stages.indexOf(stage)

  return (
    <ol className="flex flex-wrap items-center gap-x-2 gap-y-2 text-xs font-semibold uppercase tracking-[0.18em]">
      {['Checkout', 'Processing', 'Enrolled'].map((label, index) => {
        const done = index < current
        const active = index === current

        return (
          <li key={label} className="flex items-center gap-2">
            <span
              className={`flex h-6 w-6 items-center justify-center rounded-full text-[0.65rem] ${
                done || active ? 'bg-[#f45135] text-white' : 'border border-white/20 text-slate-400'
              }`}
            >
              {done ? '✓' : index + 1}
            </span>
            <span className={active ? 'text-white' : done ? 'text-slate-300' : 'text-slate-500'}>
              {label}
            </span>
            {index < 2 && <span aria-hidden="true" className="ml-1 text-slate-600">→</span>}
          </li>
        )
      })}
    </ol>
  )
}

function SummaryRow({ label, value, valueClass = 'text-white', mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 border-t border-white/10 py-3">
      <span className="text-sm text-slate-400">{label}</span>
      <span
        className={`text-right text-sm font-semibold ${valueClass} ${
          mono ? 'font-mono tracking-tight' : ''
        }`}
      >
        {value}
      </span>
    </div>
  )
}

export default function PaymentPage() {
  const { paymentId } = useParams()
  const navigate = useNavigate()

  const [payment, setPayment] = useState(null)
  const [course, setCourse] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [method, setMethod] = useState('ESEWA')
  const [stage, setStage] = useState('payment')
  const [visibleStep, setVisibleStep] = useState(0)
  const tickerRef = useRef(null)
  const holdTimerRef = useRef(null)

  const instructor = useMemo(() => {
    const first = course?.instructors?.[0]
    if (!first) return null

    return {
      name: `${first.firstName || ''} ${first.lastName || ''}`.trim() || first.username || 'Course instructor',
      username: first.username,
      initials: initialsOf(`${first.firstName || ''} ${first.lastName || ''}`),
    }
  }, [course])

  const courseId = payment?.courseId ?? course?.id

  useEffect(() => {
    let active = true

    const loadPayment = async () => {
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
        setError(requestError.response?.data?.message || 'Unable to load payment details.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadPayment()

    return () => {
      active = false
    }
  }, [paymentId])

  // A payment that was already settled (e.g. after a refresh) jumps straight to the result.
  useEffect(() => {
    if (payment?.paymentStatus === 'SUCCESS') {
      setStage('success')
    } else if (payment?.paymentStatus === 'FAILED') {
      setStage('payment')
    }
  }, [payment?.paymentStatus])

  const handlePayNow = async () => {
    if (stage !== 'payment') return

    setError('')
    setStage('processing')
    setVisibleStep(0)

    tickerRef.current = setInterval(() => {
      setVisibleStep((current) => Math.min(current + 1, PROCESS_STEPS.length - 1))
    }, 550)

    // Keep the simulated gateway on screen long enough for the student to read the steps.
    const minimumProcessing = new Promise((resolve) => {
      holdTimerRef.current = setTimeout(resolve, 2200)
    })

    try {
      const [response] = await Promise.all([
        api.post(`/api/payments/${paymentId}/process`, {
          successful: true,
          paymentMethod: method,
        }),
        minimumProcessing,
      ])

      const processedPayment = response.data?.data
      setPayment(processedPayment)
      setVisibleStep(PROCESS_STEPS.length - 1)
      setStage('success')
    } catch (requestError) {
      clearTimeout(holdTimerRef.current)
      setError(requestError.response?.data?.message || 'Unable to process payment.')
      setStage('payment')
    } finally {
      clearInterval(tickerRef.current)
      tickerRef.current = null
      clearTimeout(holdTimerRef.current)
      holdTimerRef.current = null
    }
  }

  // Clear the simulated gateway timers if the student navigates away mid-payment.
  useEffect(
    () => () => {
      if (tickerRef.current) clearInterval(tickerRef.current)
      if (holdTimerRef.current) clearTimeout(holdTimerRef.current)
    },
    [],
  )

  if (loading) {
    return (
      <main className="portal-frame flex min-h-screen flex-col">
        <SiteNavbar />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-white/10 border-t-[#f45135]" aria-hidden="true" />
          <p className="text-sm text-slate-400">Preparing your secure checkout...</p>
        </div>
        <SiteFooter />
      </main>
    )
  }

  if (error && !payment) {
    return (
      <main className="portal-frame flex min-h-screen flex-col">
        <SiteNavbar />
        <div className="flex flex-1 flex-col items-center justify-center px-6">
          <div className="max-w-md rounded-3xl border border-rose-500/30 bg-[#0b1628] p-8 text-center">
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#f45135]">Payment unavailable</p>
            <h1 className="mt-4 text-2xl font-extrabold text-white">We could not open this checkout</h1>
            <p className="mt-3 text-sm leading-6 text-slate-400">{error}</p>
            <Link
              to="/courses"
              className="mt-6 inline-block rounded-lg bg-[#f45135] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#ff694d]"
            >
              Back to courses
            </Link>
          </div>
        </div>
        <SiteFooter />
      </main>
    )
  }

  const amount = payment?.amount ?? course?.price ?? 0
  const selectedMethod = PAYMENT_METHODS.find((item) => item.value === method)

  return (
    <main className="portal-frame flex min-h-screen flex-col">
      <SiteNavbar />

      {/* Step bar */}
      <div className="border-b border-white/10 bg-[#0b1628] px-6 py-4">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4">
          <Link
            to={courseId ? `/courses/${courseId}` : '/courses'}
            className="text-sm font-semibold text-slate-300 transition hover:text-white"
          >
            ← Back to course
          </Link>
          <Stepper stage={stage} />
        </div>
      </div>

      <div className="flex-1 px-6 py-12">
        <div className="mx-auto max-w-6xl">
          <p className="text-sm font-bold uppercase tracking-[0.28em] text-[#f45135]">
            Secure checkout
          </p>
          <h1 className="mt-3 text-4xl font-extrabold tracking-tight text-white">
            {stage === 'success' ? 'Payment complete' : 'Complete your enrollment'}
          </h1>
          <p className="mt-4 max-w-2xl text-base leading-7 text-slate-400">
            {stage === 'success'
              ? 'Your payment was approved and your seat in this course is now active.'
              : 'Review the course details, choose a payment method and pay to unlock every lesson, assignment and certificate.'}
          </p>

          <div className="mt-10 grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
            {/* ---------------- Order summary ---------------- */}
            <section className="rounded-3xl border border-white/10 bg-[#0b1628] p-7 shadow-2xl shadow-black/30">
              <h2 className="text-lg font-bold text-white">Order summary</h2>

              <div className="mt-6 flex gap-4">
                <div className="h-24 w-24 shrink-0 rounded-2xl bg-gradient-to-br from-teal-500 via-cyan-500 to-indigo-600" />
                <div className="min-w-0">
                  <p className="text-xs font-bold uppercase tracking-[0.18em] text-teal-300">
                    {course?.category?.name || 'Course'}
                  </p>
                  <p className="mt-2 text-xl font-bold leading-snug text-white">
                    {payment?.courseTitle || course?.title || 'Course'}
                  </p>
                  <p className="mt-2 text-sm text-slate-400">{course?.level || 'All levels'}</p>
                </div>
              </div>

              {/* Instructor */}
              <div className="mt-6 flex items-center gap-4 rounded-2xl border border-white/10 bg-[#182b4d] p-4">
                <span
                  aria-hidden="true"
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#fff5d6] text-sm font-extrabold text-[#0f172a]"
                >
                  {instructor?.initials || 'CI'}
                </span>
                <div className="min-w-0">
                  <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">
                    Course instructor
                  </p>
                  <p className="mt-1 truncate text-base font-bold text-white">
                    {instructor?.name || 'To be announced'}
                  </p>
                  {instructor?.username && (
                    <p className="mt-0.5 text-xs text-slate-400">@{instructor.username}</p>
                  )}
                </div>
              </div>

              {/* Price + transaction id */}
              <div className="mt-6">
                <SummaryRow label="Course price" value={formatNpr(amount)} />
                <SummaryRow label="Processing fee" value="Rs.0" valueClass="text-emerald-300" />
                <SummaryRow
                  label="Total payable"
                  value={formatNpr(amount)}
                  valueClass="text-[#fff5d6] text-base"
                />
              </div>

              <div className="mt-4 rounded-2xl border border-white/10 bg-[#050b14] p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">
                      Transaction / payment ID
                    </p>
                    <p className="mt-1 font-mono text-sm font-semibold text-[#fff5d6]">
                      {payment?.transactionId || `PENDING-${payment?.id}`}
                    </p>
                  </div>
                  <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-slate-300">
                    {payment?.paymentStatus || 'PENDING'}
                  </span>
                </div>
              </div>

              <p className="mt-5 text-xs leading-5 text-slate-500">
                This is a simulated gateway. No real money moves and no card data is stored.
              </p>
            </section>

            {/* ---------------- Payment panel ---------------- */}
            <section className="rounded-3xl border border-white/10 bg-[#0b1628] p-7 shadow-2xl shadow-black/30">
              {stage === 'payment' && (
                <>
                  <h2 className="text-lg font-bold text-white">Choose a payment method</h2>

                  <div className="mt-5 space-y-3">
                    {PAYMENT_METHODS.map((item) => {
                      const active = method === item.value

                      return (
                        <button
                          key={item.value}
                          type="button"
                          onClick={() => setMethod(item.value)}
                          aria-pressed={active}
                          className={`flex w-full items-center gap-4 rounded-2xl border p-4 text-left transition ${
                            active
                              ? 'border-[#f45135] bg-[#f45135]/10'
                              : 'border-white/10 bg-[#050b14] hover:border-white/25'
                          }`}
                        >
                          <span
                            aria-hidden="true"
                            className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 ${
                              active ? 'border-[#f45135]' : 'border-slate-600'
                            }`}
                          >
                            {active && <span className="h-2.5 w-2.5 rounded-full bg-[#f45135]" />}
                          </span>
                          <span className="min-w-0">
                            <span className="block text-sm font-bold text-white">{item.label}</span>
                            <span className="block text-xs text-slate-400">{item.hint}</span>
                          </span>
                        </button>
                      )
                    })}
                  </div>

                  {method === 'CARD' && (
                    <div className="mt-5 space-y-3 rounded-2xl border border-white/10 bg-[#050b14] p-4">
                      <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">
                        Demo card details
                      </p>
                      <input
                        defaultValue="4111 1111 1111 1111"
                        aria-label="Card number"
                        readOnly
                        className="w-full rounded-xl border border-white/10 bg-[#0b1628] px-4 py-3 font-mono text-sm text-slate-300"
                      />
                      <div className="grid grid-cols-2 gap-3">
                        <input
                          defaultValue="12/28"
                          aria-label="Expiry date"
                          readOnly
                          className="w-full rounded-xl border border-white/10 bg-[#0b1628] px-4 py-3 font-mono text-sm text-slate-300"
                        />
                        <input
                          defaultValue="123"
                          aria-label="CVV"
                          readOnly
                          className="w-full rounded-xl border border-white/10 bg-[#0b1628] px-4 py-3 font-mono text-sm text-slate-300"
                        />
                      </div>
                    </div>
                  )}

                  {error && (
                    <p className="mt-5 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">
                      {error}
                    </p>
                  )}

                  <button
                    type="button"
                    onClick={handlePayNow}
                    className="mt-6 w-full rounded-xl bg-[#f45135] px-5 py-4 text-sm font-bold text-white shadow-lg shadow-[#f45135]/20 transition hover:bg-[#ff694d]"
                  >
                    Pay Now · {formatNpr(amount)}
                  </button>

                  <p className="mt-3 text-center text-xs text-slate-500">
                    Paying with {selectedMethod?.label} · {formatNpr(amount)}
                  </p>

                  <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="mt-4 w-full rounded-xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-300 transition hover:bg-white/5"
                  >
                    Cancel payment
                  </button>
                </>
              )}

              {stage === 'processing' && (
                <div className="flex min-h-[28rem] flex-col items-center justify-center text-center">
                  <div className="h-14 w-14 animate-spin rounded-full border-4 border-white/10 border-t-[#f45135]" aria-hidden="true" />
                  <h2 className="mt-6 text-xl font-extrabold text-white">Processing your payment</h2>
                  <p className="mt-2 text-sm text-slate-400">
                    Please keep this window open, this usually takes a few seconds.
                  </p>

                  <ul className="mt-8 w-full space-y-3 text-left">
                    {PROCESS_STEPS.map((step, index) => {
                      const done = index < visibleStep
                      const active = index === visibleStep

                      return (
                        <li
                          key={step}
                          className={`flex items-center gap-3 rounded-2xl border p-4 text-sm transition ${
                            active
                              ? 'border-[#f45135] bg-[#f45135]/10 text-white'
                              : done
                                ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
                                : 'border-white/10 text-slate-500'
                          }`}
                        >
                          <span aria-hidden="true" className="font-mono text-xs">
                            {done ? '✓' : active ? '•' : '–'}
                          </span>
                          {step}
                        </li>
                      )
                    })}
                  </ul>
                </div>
              )}

              {stage === 'success' && (
                <div className="flex min-h-[28rem] flex-col justify-center">
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/15 text-3xl text-emerald-400">
                    ✓
                  </div>
                  <h2 className="mt-6 text-2xl font-extrabold text-white">Payment successful</h2>
                  <p className="mt-3 text-sm leading-6 text-slate-400">
                    {formatNpr(amount)} was charged to your dummy account and you are now enrolled in{' '}
                    <span className="font-semibold text-white">
                      {payment?.courseTitle || course?.title}
                    </span>
                    .
                  </p>

                  <div className="mt-6 rounded-2xl border border-white/10 bg-[#050b14] p-4">
                    <SummaryRow label="Amount paid" value={formatNpr(amount)} valueClass="text-[#fff5d6]" />
                    <SummaryRow label="Payment method" value={payment?.paymentMethod || method} />
                    <SummaryRow
                      label="Transaction ID"
                      value={payment?.transactionId || '—'}
                      mono
                    />
                    <SummaryRow label="Status" value="SUCCESS" valueClass="text-emerald-300" />
                  </div>

                  <button
                    type="button"
                    onClick={() => navigate(`/student/courses/${courseId}/learn`)}
                    className="mt-6 w-full rounded-xl bg-[#f45135] px-5 py-4 text-sm font-bold text-white shadow-lg shadow-[#f45135]/20 transition hover:bg-[#ff694d]"
                  >
                    Start learning
                  </button>

                  <button
                    type="button"
                    onClick={() => navigate(`/payment/success/${paymentId}`)}
                    className="mt-3 w-full rounded-xl border border-white/15 px-5 py-3 text-sm font-semibold text-slate-300 transition hover:bg-white/5"
                  >
                    View payment receipt
                  </button>

                  <Link
                    to="/student/courses"
                    className="mt-4 block text-center text-xs font-semibold text-slate-400 underline underline-offset-4 hover:text-white"
                  >
                    Go to my courses
                  </Link>
                </div>
              )}
            </section>
          </div>
        </div>
      </div>

      <SiteFooter />
    </main>
  )
}
