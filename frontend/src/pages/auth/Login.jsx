import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import PasswordInput from '../../components/PasswordInput'
import { dashboardPathFor } from '../../utils/format'
import SiteNavbar from '../../components/SiteNavbar'
import SiteFooter from '../../components/SiteFooter'

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, session, isAuthenticated } = useAuth()

  const [form, setForm] = useState({ username: '', password: '' })
  const [rememberMe, setRememberMe] = useState(true)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate to={dashboardPathFor(session?.role)} replace />
  }

  // Preserved by the 401 interceptor so an expired session returns here instead of losing context.
  const redirectTo = new URLSearchParams(location.search).get('redirect')

  // Reject anything that is not a same-origin path. `//evil.com` and absolute URLs
  // would otherwise be honoured as a redirect target.
  const safeRedirect =
    redirectTo && redirectTo.startsWith('/') && !redirectTo.startsWith('//')
      ? redirectTo
      : null

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (!form.username.trim() || !form.password) {
      setError('Enter your username and password.')
      return
    }

    setIsSubmitting(true)

    try {
      const result = await login({
        username: form.username.trim(),
        password: form.password,
        rememberMe,
      })
      navigate(safeRedirect || dashboardPathFor(result?.role), { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="portal-frame">
      <SiteNavbar />
      <div className="grid flex-1 items-center gap-10 px-6 py-12 lg:grid-cols-[1fr_28rem] lg:px-16">
      <section className="mx-auto w-full max-w-2xl text-white">
        <p className="text-sm font-bold uppercase tracking-[0.24em] text-[#f45135]">Welcome back</p>
        <h1 className="mt-5 text-5xl font-extrabold leading-tight md:text-7xl">Keep your learning moving.</h1>
        <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">Sign in to continue your courses, pick up your next lesson, and stay close to your progress.</p>
      </section>
      <section className="surface-cream mx-auto w-full max-w-md rounded-2xl p-8 shadow-2xl shadow-black/30">
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#f45135]">CourseFlow account</p>
        <h2 className="mt-3 text-3xl font-extrabold text-brand-navy">Sign in</h2>

        {location.state?.message && (
          <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {location.state.message}
          </div>
        )}

        {error && (
          <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
          <div>
            <label htmlFor="login-username" className="mb-2 block text-sm font-medium text-slate-700">Username</label>
            <input
              id="login-username"
              name="username"
              autoComplete="username"
              required
              value={form.username}
              onChange={handleChange}
              className="field mt-2"
              placeholder="Enter username"
            />
          </div>

          <PasswordInput
            id="login-password"
            name="password"
            label="Password"
            autoComplete="current-password"
            value={form.password}
            onChange={handleChange}
            placeholder="••••••••"
          />

          <div className="flex flex-wrap items-center justify-between gap-3">
            <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500"
              />
              Remember me
            </label>

            <Link to="/forgot-password" className="text-sm font-semibold text-teal-700 underline underline-offset-2 hover:text-teal-800">
              Forgot password?
            </Link>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="coral-action w-full disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        {!rememberMe && (
          <p className="mt-3 text-xs text-slate-500">
            Without &quot;remember me&quot; you will be signed out when this tab is closed.
          </p>
        )}

        <p className="mt-5 text-sm text-slate-600">
          New to CourseFlow?{' '}
          <Link to="/register" className="font-semibold text-teal-700 underline">
            Create an account
          </Link>
        </p>
      </section>
      </div>
      <SiteFooter />
    </main>
  )
}