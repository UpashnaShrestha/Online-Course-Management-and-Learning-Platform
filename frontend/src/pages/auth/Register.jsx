import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { dashboardPathFor } from '../../utils/format'
import { MIN_USERNAME_LENGTH, validateEmail, validatePassword } from '../../utils/validation'
import PasswordInput from '../../components/PasswordInput'
import PasswordRules from '../../components/PasswordRules'
import SiteNavbar from '../../components/SiteNavbar'
import SiteFooter from '../../components/SiteFooter'

export default function Register() {
  const navigate = useNavigate()
  const { register, session, isAuthenticated } = useAuth()
  const [form, setForm] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate to={dashboardPathFor(session?.role)} replace />
  }

  const handleChange = (event) => {
    const { name, value } = event.target

    // An unnamed input would silently write to an empty-string key and the controlled
    // field would appear "untypable". Fail loudly instead of corrupting state.
    if (!name) {
      console.error('Register form: input is missing a name attribute', event.target)
      return
    }

    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => {
      const next = { ...current }
      delete next[name]
      return next
    })
  }

  const validate = () => {
    const problems = {}

    const username = form.username.trim()
    if (!username) {
      problems.username = 'Username is required.'
    } else if (username.length < MIN_USERNAME_LENGTH) {
      problems.username = `Username must be at least ${MIN_USERNAME_LENGTH} characters.`
    }

    if (!form.firstName.trim()) problems.firstName = 'First name is required.'
    if (!form.lastName.trim()) problems.lastName = 'Last name is required.'

    const emailProblem = validateEmail(form.email)
    if (emailProblem) {
      problems.email = emailProblem
    }

    const passwordProblem = validatePassword(form.password)
    if (passwordProblem) {
      problems.password = passwordProblem
    }

    if (form.confirmPassword !== form.password) {
      problems.confirmPassword = 'Passwords do not match.'
    }

    setFieldErrors(problems)
    return Object.keys(problems).length === 0
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (!validate()) return

    setIsSubmitting(true)

    try {
      await register({
        username: form.username.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        password: form.password,
      })

      navigate('/login', { state: { message: 'Account created. Sign in to continue.' } })
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const inputClass = (field) =>
    `field mt-2 ${fieldErrors[field] ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : ''}`

  const errorText = (field) =>
    fieldErrors[field] ? (
      <p className="mt-1 text-xs font-medium text-rose-600">{fieldErrors[field]}</p>
    ) : null

  return (
    <main className="portal-frame">
      <SiteNavbar />
      <div className="grid flex-1 items-center gap-10 px-6 py-12 lg:grid-cols-[1fr_34rem] lg:px-16">
      <section className="mx-auto w-full max-w-2xl text-white">
        <p className="text-sm font-bold uppercase tracking-[0.24em] text-[#f45135]">Start your next chapter</p>
        <h1 className="mt-5 text-5xl font-extrabold leading-tight md:text-7xl">Build skills that stay with you.</h1>
        <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">Create your CourseFlow account and turn good intentions into a learning rhythm you can see.</p>
      </section>
      <section className="surface-cream mx-auto w-full max-w-lg rounded-2xl p-8 shadow-2xl shadow-black/30">
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#f45135]">CourseFlow account</p>
        <h1 className="mt-3 text-3xl font-extrabold text-brand-navy">Create account</h1>

        {error && (
          <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label htmlFor="reg-firstName" className="mb-2 block text-sm font-medium text-slate-700">First name</label>
              <input id="reg-firstName" name="firstName" autoComplete="given-name" required
                value={form.firstName} onChange={handleChange} className={inputClass('firstName')} />
              {errorText('firstName')}
            </div>
            <div>
              <label htmlFor="reg-lastName" className="mb-2 block text-sm font-medium text-slate-700">Last name</label>
              <input id="reg-lastName" name="lastName" autoComplete="family-name" required
                value={form.lastName} onChange={handleChange} className={inputClass('lastName')} />
              {errorText('lastName')}
            </div>
          </div>

          <div>
            <label htmlFor="reg-username" className="mb-2 block text-sm font-medium text-slate-700">Username</label>
            <input id="reg-username" name="username" autoComplete="username" required minLength={3}
              value={form.username} onChange={handleChange} className={inputClass('username')} />
            {errorText('username')}
          </div>

          <div>
            <label htmlFor="reg-email" className="mb-2 block text-sm font-medium text-slate-700">Email</label>
            <input id="reg-email" type="email" name="email" autoComplete="email" required
              value={form.email} onChange={handleChange} className={inputClass('email')} />
            {errorText('email')}
          </div>

          <div>
            <PasswordInput
              id="reg-password"
              name="password"
              label="Password"
              value={form.password}
              onChange={handleChange}
              autoComplete="new-password"
              error={fieldErrors.password}
            />
            <PasswordRules value={form.password} />
          </div>

          <div>
            <PasswordInput
              id="reg-confirmPassword"
              name="confirmPassword"
              label="Confirm password"
              value={form.confirmPassword}
              onChange={handleChange}
              autoComplete="new-password"
              error={fieldErrors.confirmPassword}
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="coral-action w-full disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="mt-5 text-sm text-slate-600">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-teal-700 underline">
            Sign in
          </Link>
        </p>
      </section>
      </div>
      <SiteFooter />
    </main>
  )
}