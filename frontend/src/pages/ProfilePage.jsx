import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { KeyRound, ShieldCheck, UserCog } from 'lucide-react'
import { DashboardLayout } from '../layouts/DashboardLayout'
import { useAuth } from '../context/AuthContext'
import PasswordInput from '../components/PasswordInput'
import api from '../services/api'
import { validateEmail, validatePassword } from '../utils/validation'
import PasswordRules from '../components/PasswordRules'
import AvatarUpload from '../components/AvatarUpload'
import { initialsOf, resolveImageUrl } from '../utils/format'

const inputClass = 'field mt-2'

function Field({ id, label, value, onChange, type = 'text', autoComplete, placeholder, span2 = false, maxLength }) {
  return (
    <div className={span2 ? 'sm:col-span-2' : ''}>
      <label htmlFor={id} className="mb-2 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        maxLength={maxLength}
        placeholder={placeholder}
        className={inputClass}
      />
    </div>
  )
}

export default function ProfilePage({ role = 'STUDENT' }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const { session, patchSession } = useAuth()

  const tab = searchParams.get('tab') === 'password' ? 'password' : 'profile'

  // Profile state
  const [profile, setProfile] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    bio: '',
    profileImage: '',
    professionalTitle: '',
    expertise: '',
    qualifications: '',
    teachingExperience: '',
    publicInstructorProfile: true,
  })
  const [profileErrors, setProfileErrors] = useState({})
  const [profileMessage, setProfileMessage] = useState('')
  const [profileError, setProfileError] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)

  // Password state
  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [passwordErrors, setPasswordErrors] = useState({})
  const [passwordMessage, setPasswordMessage] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let active = true

    api
      .get('/api/users/me')
      .then((response) => {
        if (!active) return
        const user = response.data?.data

        if (user) {
          setProfile({
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            email: user.email || '',
            phoneNumber: user.phoneNumber || '',
            bio: user.bio || '',
            profileImage: user.profileImage || '',
            professionalTitle: user.professionalTitle || '',
            expertise: user.expertise || '',
            qualifications: user.qualifications || '',
            teachingExperience: user.teachingExperience || '',
            publicInstructorProfile: user.publicInstructorProfile ?? true,
          })
        }
      })
      .catch((requestError) => {
        if (active) {
          setLoadError(requestError.response?.data?.message || 'Unable to load your profile.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const setField = (field) => (event) => {
    const { value } = event.target
    setProfile((current) => ({ ...current, [field]: value }))
    // Delete the key rather than blanking it, otherwise the error summary list
    // stays "present" forever with nothing in it.
    setProfileErrors((current) => {
      const next = { ...current }
      delete next[field]
      return next
    })
    setProfileMessage('')
  }

  const resetProfileForm = () => {
    setProfile({
      firstName: session?.firstName || '',
      lastName: session?.lastName || '',
      email: session?.email || '',
      phoneNumber: session?.phoneNumber || '',
      bio: session?.bio || '',
      profileImage: session?.profileImage || '',
      professionalTitle: session?.professionalTitle || '',
      expertise: session?.expertise || '',
      qualifications: session?.qualifications || '',
      teachingExperience: session?.teachingExperience || '',
      publicInstructorProfile: session?.publicInstructorProfile ?? true,
    })
    setProfileErrors({})
    setProfileError('')
    setProfileMessage('')
  }

  const saveProfile = async (event) => {
    event.preventDefault()
    setProfileError('')
    setProfileMessage('')

    const problems = {}
    if (!profile.firstName.trim()) problems.firstName = 'First name is required.'
    if (!profile.lastName.trim()) problems.lastName = 'Last name is required.'
    const emailProblem = validateEmail(profile.email)
    if (emailProblem) {
      problems.email = emailProblem
    }
    if (profile.phoneNumber && profile.phoneNumber.replace(/\D/g, '').length < 7) {
      problems.phoneNumber = 'Enter a valid phone number.'
    }

    setProfileErrors(problems)
    if (Object.keys(problems).length > 0) return

    setSavingProfile(true)

    try {
      const response = await api.put('/api/users/me', {
        firstName: profile.firstName.trim(),
        lastName: profile.lastName.trim(),
        email: profile.email.trim(),
        phoneNumber: profile.phoneNumber.trim(),
        bio: profile.bio.trim(),
        profileImage: profile.profileImage.trim(),
        professionalTitle: profile.professionalTitle.trim(),
        expertise: profile.expertise.trim(),
        qualifications: profile.qualifications.trim(),
        teachingExperience: profile.teachingExperience.trim(),
        publicInstructorProfile: profile.publicInstructorProfile,
      })

      const user = response.data?.data

      // Keep the navbar name and account summary in sync without a re-login.
      if (user) {
        patchSession({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phoneNumber: user.phoneNumber,
          profileImage: user.profileImage,
          bio: user.bio,
          professionalTitle: user.professionalTitle,
          expertise: user.expertise,
          qualifications: user.qualifications,
          teachingExperience: user.teachingExperience,
          publicInstructorProfile: user.publicInstructorProfile,
        })
      }

      setProfileMessage('Profile updated successfully.')
    } catch (requestError) {
      setProfileError(requestError.response?.data?.message || 'Unable to update your profile.')
    } finally {
      setSavingProfile(false)
    }
  }

  const setPassword = (field) => (event) => {
    const { value } = event.target
    setPasswords((current) => ({ ...current, [field]: value }))
    setPasswordErrors((current) => {
      const next = { ...current }
      delete next[field]
      return next
    })
    setPasswordMessage('')
  }

  const savePassword = async (event) => {
    event.preventDefault()
    setPasswordError('')
    setPasswordMessage('')

    const problems = {}
    if (!passwords.currentPassword) problems.currentPassword = 'Current password is required.'
    const passwordProblem = validatePassword(passwords.newPassword)
    if (passwordProblem) {
      problems.newPassword = passwordProblem
    }
    if (passwords.confirmPassword !== passwords.newPassword) {
      problems.confirmPassword = 'Passwords do not match.'
    }

    setPasswordErrors(problems)
    if (Object.keys(problems).length > 0) return

    setSavingPassword(true)

    try {
      await api.put('/api/users/me/password', {
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
        confirmPassword: passwords.confirmPassword,
      })

      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setPasswordMessage('Password changed successfully.')
    } catch (requestError) {
      setPasswordError(requestError.response?.data?.message || 'Unable to change your password.')
    } finally {
      setSavingPassword(false)
    }
  }

  const tabs = [
    { key: 'profile', label: 'Update profile', icon: UserCog },
    { key: 'password', label: 'Change password', icon: KeyRound },
  ]

  return (
    <DashboardLayout
      role={role}
      title="My profile"
      subtitle="Manage your personal details and account security."
    >
      {/* Account summary */}
      <section className="mb-6 rounded-2xl bg-white p-5 ring-1 ring-slate-200">
        <div className="flex flex-wrap items-center gap-4">
          {profile.profileImage ? (
            <img
              src={resolveImageUrl(profile.profileImage)}
              alt=""
              className="h-16 w-16 rounded-full object-cover ring-2 ring-slate-200"
            />
          ) : (
            <span
              aria-hidden="true"
              className="flex h-16 w-16 items-center justify-center rounded-full bg-[#0f172a] text-xl font-bold text-white"
            >
              {initialsOf(`${session?.firstName || ''} ${session?.lastName || ''}`.trim()
                || session?.username
                || 'U')}
            </span>
          )}

          <div className="min-w-0">
            <p className="text-lg font-bold text-slate-900">
              {session?.firstName} {session?.lastName}
            </p>
            <p className="text-sm text-slate-600">@{session?.username}</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold uppercase tracking-wider text-slate-700">
                {session?.role}
              </span>
              {session?.enabled && (
                <span className="flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                  <ShieldCheck size={12} aria-hidden="true" />
                  Active
                </span>
              )}
            </div>
          </div>

          <div className="ml-auto text-sm text-slate-500">
            <p>Member since</p>
            <p className="font-semibold text-slate-700">
              {session?.createdAt ? new Date(session.createdAt).toLocaleDateString() : '—'}
            </p>
          </div>
        </div>
      </section>

      {/* Tabs */}
      <div className="mb-5 flex flex-wrap gap-2">
        {tabs.map((item) => {
          const Icon = item.icon
          const active = tab === item.key

          return (
            <button
              key={item.key}
              type="button"
              onClick={() => setSearchParams(item.key === 'profile' ? {} : { tab: item.key })}
              aria-pressed={active}
              className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition ${
                active
                  ? 'bg-[#0f172a] text-white'
                  : 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50'
              }`}
            >
              <Icon size={15} aria-hidden="true" />
              {item.label}
            </button>
          )
        })}
      </div>

      {loading && <p className="text-slate-600">Loading your profile...</p>}

      {loadError && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center">
          <p className="font-semibold text-red-900">We could not load your profile.</p>
          <p className="mt-2 text-sm text-red-700">{loadError}</p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-5 rounded-lg bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
          >
            Try again
          </button>
        </div>
      )}

      {!loading && !loadError && tab === 'profile' && (
        <form onSubmit={saveProfile} noValidate className="rounded-2xl bg-white p-5 ring-1 ring-slate-200 md:p-6">
          <h2 className="text-lg font-bold text-slate-900">Personal details</h2>
          <p className="mt-1 text-sm text-slate-600">
            Your username cannot be changed. Everything else updates immediately.
          </p>

          {profileError && (
            <p role="alert" className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-700">
              {profileError}
            </p>
          )}
          {profileMessage && (
            <p role="status" className="mt-4 rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">
              {profileMessage}
            </p>
          )}

          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field id="profile-firstName" label="First name" value={profile.firstName}
              onChange={setField('firstName')} autoComplete="given-name" />
            <Field id="profile-lastName" label="Last name" value={profile.lastName}
              onChange={setField('lastName')} autoComplete="family-name" />
            <Field id="profile-email" label="Email" type="email" value={profile.email}
              onChange={setField('email')} autoComplete="email" />
            <Field id="profile-phone" label="Phone number" value={profile.phoneNumber}
              onChange={setField('phoneNumber')} autoComplete="tel" placeholder="+977 98XXXXXXXX" />
            <Field id="profile-image" label="Profile image URL" value={profile.profileImage}
              onChange={setField('profileImage')} placeholder="https://…" span2 maxLength={500} />

            <div className="sm:col-span-2">
              {/* Uploads immediately and writes the returned URL into the form, so the
                  existing save step needs no special case. */}
              <AvatarUpload
                value={profile.profileImage}
                onChange={(url) => setProfile((current) => ({ ...current, profileImage: url }))}
                initials={initialsOf(`${profile.firstName} ${profile.lastName}`.trim() || profile.username || 'U')}
                disabled={savingProfile}
              />
            </div>

            {role === 'INSTRUCTOR' && (
              <>
                <div className="sm:col-span-2 border-t border-slate-200 pt-4">
                  <h3 className="font-semibold text-slate-900">Public instructor profile</h3>
                  <p className="mt-1 text-sm text-slate-600">These details are shown to students on your course and public profile.</p>
                </div>
                <Field id="professional-title" label="Professional title / designation" value={profile.professionalTitle} onChange={setField('professionalTitle')} maxLength={150} placeholder="Java Developer" />
                <Field id="teaching-experience" label="Teaching experience" value={profile.teachingExperience} onChange={setField('teachingExperience')} maxLength={500} placeholder="5 years" />
                <Field id="expertise" label="Areas of expertise" value={profile.expertise} onChange={setField('expertise')} maxLength={500} placeholder="Java, Spring Boot, databases" span2 />
                <Field id="qualifications" label="Qualifications" value={profile.qualifications} onChange={setField('qualifications')} maxLength={1000} placeholder="MSc Computer Science" span2 />
                <label className="flex items-center gap-2 text-sm text-slate-700 sm:col-span-2">
                  <input type="checkbox" checked={profile.publicInstructorProfile} onChange={(event) => setProfile((current) => ({ ...current, publicInstructorProfile: event.target.checked }))} />
                  Make my instructor profile visible to students
                </label>
              </>
            )}

            <Field id="profile-image" label="Profile image URL" value={profile.profileImage}
              onChange={setField('profileImage')} placeholder="https://…" span2 maxLength={500} />

            <div className="sm:col-span-2">
              <label htmlFor="profile-bio" className="mb-2 block text-sm font-medium text-slate-700">
                Bio
              </label>
              <textarea
                id="profile-bio"
                rows={4}
                maxLength={5000}
                value={profile.bio}
                onChange={setField('bio')}
                placeholder="Tell others a little about yourself."
                className="field mt-2 resize-y"
              />
            </div>
          </div>

          {Object.keys(profileErrors).length > 0 && (
            <ul className="mt-4 space-y-1 rounded-xl bg-amber-50 p-3 text-sm text-amber-800">
              {Object.entries(profileErrors).map(([field, message]) => (
                <li key={field}>{message}</li>
              ))}
            </ul>
          )}

          <div className="mt-5 flex flex-wrap gap-3">
            <button type="submit" disabled={savingProfile}
              className="rounded-lg bg-[#0f172a] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#263f7a] disabled:opacity-60">
              {savingProfile ? 'Saving...' : 'Save changes'}
            </button>
            <button type="button" onClick={resetProfileForm}
              className="rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50">
              Reset
            </button>
          </div>
        </form>
      )}

      {!loading && !loadError && tab === 'password' && (
        <form onSubmit={savePassword} noValidate className="max-w-xl rounded-2xl bg-white p-5 ring-1 ring-slate-200 md:p-6">
          <h2 className="text-lg font-bold text-slate-900">Change password</h2>
          <p className="mt-1 text-sm text-slate-600">
            Pick a strong password. You will stay signed in on this device.
          </p>

          {passwordError && (
            <p role="alert" className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-700">
              {passwordError}
            </p>
          )}
          {passwordMessage && (
            <p role="status" className="mt-4 rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">
              {passwordMessage}
            </p>
          )}

          <div className="mt-5 space-y-4">
            <PasswordInput
              id="pw-current"
              label="Current password"
              value={passwords.currentPassword}
              onChange={setPassword('currentPassword')}
              autoComplete="current-password"
              error={passwordErrors.currentPassword}
            />
            <PasswordInput
              id="pw-new"
              label="New password"
              value={passwords.newPassword}
              onChange={setPassword('newPassword')}
              autoComplete="new-password"
              error={passwordErrors.newPassword}
            />
            <PasswordRules value={passwords.newPassword} />
            <PasswordInput
              id="pw-confirm"
              label="Confirm new password"
              value={passwords.confirmPassword}
              onChange={setPassword('confirmPassword')}
              autoComplete="new-password"
              error={passwordErrors.confirmPassword}
            />
          </div>

          {Object.keys(passwordErrors).length > 0 && (
            <ul className="mt-4 space-y-1 rounded-xl bg-amber-50 p-3 text-sm text-amber-800">
              {Object.entries(passwordErrors).map(([field, message]) => (
                <li key={field}>{message}</li>
              ))}
            </ul>
          )}

          <button type="submit" disabled={savingPassword}
            className="mt-5 rounded-lg bg-[#0f172a] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#263f7a] disabled:opacity-60">
            {savingPassword ? 'Updating...' : 'Change password'}
          </button>
        </form>
      )}
    </DashboardLayout>
  )
}