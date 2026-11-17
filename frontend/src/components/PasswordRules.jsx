import { Check, X } from 'lucide-react'
import { PASSWORD_RULES, passwordFailures } from '../utils/validation'

/**
 * Live checklist of the password policy.
 *
 * Rendered next to a password field so the user sees which rules are still outstanding
 * instead of only finding out after submitting. Rules come from PASSWORD_RULES, which is
 * mirrored from the backend's PasswordPolicy.
 */
export default function PasswordRules({ value = '', className = '' }) {
  // Before anything is typed, show the rules neutrally rather than a wall of red crosses.
  const started = value.length > 0
  const failed = started ? passwordFailures(value) : []
  const failedKeys = new Set(failed.map((rule) => rule.key))

  return (
    <ul className={`mt-3 space-y-1.5 ${className}`} aria-live="polite">
      {PASSWORD_RULES.map((rule) => {
        const unmet = started && failedKeys.has(rule.key)
        const met = started && !failedKeys.has(rule.key)

        return (
          <li
            key={rule.key}
            className={`flex items-center gap-2 text-xs ${
              unmet ? 'text-rose-600' : met ? 'text-emerald-700' : 'text-slate-500'
            }`}
          >
            <span
              aria-hidden="true"
              className={`flex h-4 w-4 shrink-0 items-center justify-center rounded-full ${
                unmet
                  ? 'bg-rose-100 text-rose-600'
                  : met
                    ? 'bg-emerald-100 text-emerald-700'
                    : 'bg-slate-200 text-slate-400'
              }`}
            >
              {met ? <Check size={11} strokeWidth={3} /> : unmet ? <X size={11} strokeWidth={3} /> : null}
            </span>
            {rule.label}
          </li>
        )
      })}
    </ul>
  )
}