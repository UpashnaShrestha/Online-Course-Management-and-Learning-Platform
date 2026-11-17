import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import api from '../../services/api'
import PasswordInput from '../../components/PasswordInput'
import PasswordRules from '../../components/PasswordRules'
import SiteNavbar from '../../components/SiteNavbar'
import SiteFooter from '../../components/SiteFooter'
import {
  OTP_LENGTH,
  isValidOtp,
  validateEmail,
  validatePassword,
} from '../../utils/validation'

export default function ResetPassword() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const emailFromUrl = searchParams.get('email') || ''

  const [email, setEmail] = useState(emailFromUrl)
  const [otp, setOtp] = useState('')
  const [passwords, setPasswords] = useState({ newPassword: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  const setPassword = (field) => (event) => {
    const { value } = event.target
    setPasswords((current) => ({ ...current, [field]: value }))
    setErrors((current) => {
      const next = { ...current }
      delete next[field]
      return next
    })
    setError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    const problems = {}
    const emailProblem = validateEmail(email)
    if (emailProblem) {
      problems.email = emailProblem
    }
    if (!isValidOtp(otp)) {
      problems.otp = `Enter the ${OTP_LENGTH}-digit code from your email.`
    }
    const passwordProblem = validatePassword(passwords.newPassword)
    if (passwordProblem) {
      problems.newPassword = passwordProblem
    }
    if (passwords.confirmPassword !== passwords.newPassword) {
      problems.confirmPassword = 'Passwords do not match.'
    }

    setErrors(problems)
    if (Object.keys(problems).length > 0) return

    setIsSubmitting(true)

    try {
      await api.post('/api/auth/reset-password', {
        email: email.trim(),
        otp: otp.trim(),
        newPassword: passwords.newPassword,
        confirmPassword: passwords.confirmPassword,
      })
      setDone(true)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to reset your password.')
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
          <h1 className="mt-3 text-3xl font-extrabold text-brand-navy">Choose a new password</h1>

          {done ? (
            <>
              <div className="mt-6 rounded-2xl bg-emerald-50 p-6 text-center">
                <p className="font-semibold text-emerald-900">Password updated</p>
                <p className="mt-2 text-sm text-emerald-800">
                  You can now sign in with your new password.
                </p>
              </div>

              <button
                type="button"
                onClick={() => navigate('/login', { replace: true })}
                className="coral-action mt-6 w-full"
              >
                Go to sign in
              </button>
            </>
          ) : (
            <>
              <p className="mt-2 text-slate-600">
                Enter the code we emailed you along with your new password.
              </p>

              {error && (
                <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
                <div>
                  <label htmlFor="reset-email" className="mb-2 block text-sm font-medium text-slate-700">
                    Email address
                  </label>
                  <input
                    id="reset-email"
                    type="email"
                    autoComplete="email"
                    required
                    value={email}
                    readOnly={Boolean(emailFromUrl)}
                    onChange={(event) => {
                      setEmail(event.target.value)
                      setErrors((current) => {
                        const next = { ...current }
                        delete next.email
                        return next
                      })
                    }}
                    className={`field mt-2 ${errors.email ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : ''}`}
                    placeholder="you@example.com"
                  />
                  {errors.email && <p className="mt-1 text-xs font-medium text-rose-600">{errors.email}</p>}
                </div>

                <div>
                  <label htmlFor="reset-otp" className="mb-2 block text-sm font-medium text-slate-700">
                    One-time code
                  </label>
                  <input
                    id="reset-otp"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    required
                    maxLength={OTP_LENGTH}
                    value={otp}
                    onChange={(event) => {
                      setOtp(event.target.value.replace(/\D/g, ''))
                      setErrors((current) => {
                        const next = { ...current }
                        delete next.otp
                        return next
                      })
                    }}
                    className={`field mt-2 text-center font-mono text-lg tracking-[0.4em] ${
                      errors.otp ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : ''
                    }`}
                    placeholder="000000"
                  />
                  {errors.otp ? (
                    <p className="mt-1 text-xs font-medium text-rose-600">{errors.otp}</p>
                  ) : (
                    <p className="mt-1 text-xs text-slate-500">
                      {OTP_LENGTH} digits from your email. Valid for 10 minutes.
                    </p>
                  )}
                </div>

                <PasswordInput
                  id="reset-password"
                  label="New password"
                  value={passwords.newPassword}
                  onChange={setPassword('newPassword')}
                  autoComplete="new-password"
                  error={errors.newPassword}
                />
                <PasswordRules value={passwords.newPassword} className="mb-4" />

                <PasswordInput
                  id="reset-confirm"
                  label="Confirm new password"
                  value={passwords.confirmPassword}
                  onChange={setPassword('confirmPassword')}
                  autoComplete="new-password"
                  error={errors.confirmPassword}
                />

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="coral-action w-full disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? 'Updating...' : 'Update password'}
                </button>
              </form>

              <p className="mt-5 text-sm text-slate-600">
                Did not get a code?{' '}
                <Link to="/forgot-password" className="font-semibold text-teal-700 underline">
                  Send it again
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