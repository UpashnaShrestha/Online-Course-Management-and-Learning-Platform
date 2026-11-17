import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MailCheck } from 'lucide-react'
import api from '../../services/api'
import SiteNavbar from '../../components/SiteNavbar'
import SiteFooter from '../../components/SiteFooter'
import { validateEmail } from '../../utils/validation'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState(null)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    const problem = validateEmail(email)
    if (problem) {
      setFieldError(problem)
      return
    }

    setFieldError('')
    setIsSubmitting(true)

    try {
      const response = await api.post('/api/auth/forgot-password', { email: email.trim() })
      setResult(response.data?.data || {})
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to process the request.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="portal-frame">
      <SiteNavbar />

      <div className="flex flex-1 items-center justify-center px-6 py-14">
        <section className="surface-cream w-full max-w-md rounded-2xl p-8 shadow-2xl shadow-black/30">
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#f45135]">Account recovery</p>
          <h1 className="mt-3 text-3xl font-extrabold text-brand-navy">Forgot password</h1>

          {result ? (
            <>
              <div className="mt-6 flex flex-col items-center rounded-2xl bg-emerald-50 p-6 text-center">
                <MailCheck size={34} aria-hidden="true" className="text-emerald-600" />
                <p className="mt-3 font-semibold text-emerald-900">Check your inbox</p>
                <p className="mt-2 text-sm text-emerald-800">
                  If an account exists for <strong>{email.trim()}</strong>, a one-time code is
                  on its way. The code expires in 10 minutes and can only be used once.
                </p>
              </div>

              <button
                type="button"
                onClick={() => navigate(`/reset-password?email=${encodeURIComponent(email.trim())}`)}
                className="coral-action mt-6 w-full"
              >
                Enter the code
              </button>

              {/* Development convenience: only present when the server has
                  app.password-reset.expose-otp enabled. */}
              {result.otp && (
                <div className="mt-4 rounded-2xl border border-amber-300 bg-amber-50 p-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-amber-800">
                    Development mode
                  </p>
                  <p className="mt-1 text-sm text-amber-900">
                    No mail server is configured, so your code is:
                  </p>
                  <p className="mt-2 font-mono text-2xl font-bold tracking-widest text-amber-900">
                    {result.otp}
                  </p>
                </div>
              )}

              <Link
                to="/login"
                className="mt-6 block text-center text-sm font-semibold text-teal-700 underline underline-offset-2"
              >
                Back to sign in
              </Link>
            </>
          ) : (
            <>
              <p className="mt-2 text-slate-600">
                Enter the email address on your account and we will send you a one-time
                code (OTP) to choose a new password.
              </p>

              {error && (
                <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
                <div>
                  <label htmlFor="forgot-email" className="mb-2 block text-sm font-medium text-slate-700">
                    Email address
                  </label>
                  <input
                    id="forgot-email"
                    type="email"
                    autoComplete="email"
                    required
                    value={email}
                    onChange={(event) => {
                      setEmail(event.target.value)
                      setFieldError('')
                    }}
                    className={`field mt-2 ${fieldError ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : ''}`}
                    placeholder="you@example.com"
                  />
                  {fieldError && <p className="mt-1 text-xs font-medium text-rose-600">{fieldError}</p>}
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="coral-action w-full disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? 'Sending...' : 'Send OTP'}
                </button>
              </form>

              <p className="mt-5 text-sm text-slate-600">
                Remembered it?{' '}
                <Link to="/login" className="font-semibold text-teal-700 underline">
                  Sign in
                </Link>
              </p>
            </>
          )}
        </section>
      </div>

      <SiteFooter />
    </main>
  )
}