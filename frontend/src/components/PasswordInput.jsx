import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

/**
 * Password input with a show/hide toggle. Used by login, register, reset-password and the
 * profile change-password form so the behaviour is identical everywhere.
 */
export default function PasswordInput({
  id,
  name,
  label,
  value,
  onChange,
  autoComplete = 'current-password',
  placeholder,
  hint,
  error,
  required = true,
}) {
  const [visible, setVisible] = useState(false)
  const Icon = visible ? EyeOff : Eye

  return (
    <div>
      <label htmlFor={id} className="mb-2 block text-sm font-medium text-slate-700">
        {label}
      </label>

      <div className="relative">
        <input
          id={id}
          // Required: callers commonly use name-based handlers (event.target.name).
          // Without it a controlled field would never update and appear "untypable".
          name={name ?? id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          placeholder={placeholder}
          required={required}
          aria-invalid={error ? 'true' : undefined}
          className={`field mt-2 pr-11 ${
            error ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : ''
          }`}
        />

        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? `Hide ${label}` : `Show ${label}`}
          aria-pressed={visible}
          title={visible ? 'Hide password' : 'Show password'}
          className="absolute right-1 top-1/2 mt-0.5 -translate-y-1/2 rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
        >
          <Icon size={17} aria-hidden="true" />
        </button>
      </div>

      {error ? (
        <p className="mt-1 text-xs font-medium text-rose-600">{error}</p>
      ) : (
        hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>
      )}
    </div>
  )
}