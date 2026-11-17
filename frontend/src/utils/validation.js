/**
 * Client-side form rules shared by the auth and profile forms.
 *
 * These deliberately mirror the backend constraints on the matching DTOs
 * (RegisterRequest, ForgotPasswordRequest, ResetPasswordRequest, ChangePasswordRequest).
 * If one side changes, the other must change too, or the user hits an error only after a
 * round trip that the form said was impossible.
 *
 * PASSWORD_RULES mirrors com.college...validation.PasswordPolicy.
 */

/** Matches jakarta @Email closely enough to catch typos without rejecting valid addresses. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Matches @Size(min = 3) on the username field. */
export const MIN_USERNAME_LENGTH = 3

/** Minimum accepted password length. Kept alongside PASSWORD_RULES for copy that needs it. */
export const MIN_PASSWORD_LENGTH = 8

/**
 * Must match OtpService: a fixed-width numeric code. Kept in one place so the input's
 * maxLength, the inline hint and the submit-time check cannot drift apart.
 */
export const OTP_LENGTH = 6

/**
 * The password policy, mirrored from PasswordPolicy.java.
 *
 * @typedef {{ key: string, label: string, test: (value: string) => boolean }} PasswordRule
 */

/** @type {PasswordRule[]} */
export const PASSWORD_RULES = [
  {
    key: 'minLength',
    label: `At least 8 characters`,
    test: (v) => v.length >= 8 && v.length <= 100,
  },
  { key: 'uppercase', label: 'One uppercase letter (A-Z)', test: (v) => /[A-Z]/.test(v) },
  { key: 'lowercase', label: 'One lowercase letter (a-z)', test: (v) => /[a-z]/.test(v) },
  { key: 'digit', label: 'One number (0-9)', test: (v) => /\d/.test(v) },
  { key: 'special', label: 'One special character (e.g. ! @ # $ % & *)', test: (v) => /[^A-Za-z0-9]/.test(v) },
]

/** Which rules the value currently fails. Empty array means it is acceptable. */
export function passwordFailures(value) {
  if (!value) return PASSWORD_RULES
  return PASSWORD_RULES.filter((rule) => !rule.test(value))
}

export function isValidPassword(value) {
  return passwordFailures(value).length === 0
}

/**
 * @returns {string|null} an error message, or null when the password is acceptable.
 *   An empty value returns "required" rather than a list of unmet composition rules.
 */
export function validatePassword(value) {
  if (!value) return 'Password is required.'

  const failures = passwordFailures(value)
  if (failures.length === 0) return null

  if (value.length > 100) return 'Password must not exceed 100 characters.'

  return `Password needs: ${failures.map((rule) => rule.label).join(', ')}`
}

export function isValidEmail(value) {
  return EMAIL_PATTERN.test(String(value ?? '').trim())
}

export function isValidOtp(value) {
  return new RegExp(`^\\d{${OTP_LENGTH}}$`).test(String(value ?? '').trim())
}

/** @returns {string|null} an error message, or null when the email is acceptable. */
export function validateEmail(value) {
  const email = String(value ?? '').trim()
  if (!email) return 'Email is required.'
  if (!isValidEmail(email)) return 'Enter a valid email address.'
  return null
}