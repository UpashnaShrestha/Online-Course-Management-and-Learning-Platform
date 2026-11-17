/**
 * The API base URL, without a trailing slash.
 *
 * Uploaded images are stored and returned as host-independent paths like
 * `/uploads/profile-images/x.png`. That is right to persist -- it survives the backend moving to
 * a different host -- but the browser cannot use it directly: the React dev server runs on a
 * different port from the API, so `<img src="/uploads/...">` would request the dev server and
 * 404. Every render site therefore goes through resolveImageUrl.
 */
const API_BASE_URL = (
  import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/+$/, '')

/**
 * Turns a stored image reference into something an <img> can load.
 *
 * - absolute URLs (http/https) pass through untouched, so images hosted elsewhere still work;
 * - data: URLs are left alone;
 * - blank input yields '';
 * - anything else is treated as a path on the API and gets the base URL prepended.
 */
export function resolveImageUrl(value) {
  if (!value) return ''

  const trimmed = String(value).trim()
  if (!trimmed) return ''
  if (/^(https?:)?\/\//i.test(trimmed)) return trimmed
  if (trimmed.startsWith('data:')) return trimmed

  return `${API_BASE_URL}${trimmed.startsWith('/') ? '' : '/'}${trimmed}`
}

/**
 * Formats a numeric amount as Nepali Rupees, e.g. 500 -> "Rs.500", 125000 -> "Rs.1,25,000".
 */
export function formatNpr(amount) {
  const value = Number(amount)

  if (!Number.isFinite(value)) {
    return 'Rs.0'
  }

  return `Rs.${Math.round(value).toLocaleString('en-IN')}`
}

/**
 * Dummy payment methods offered on the simulated checkout page.
 * The values map 1:1 to the backend `PaymentMethod` enum.
 */
export const PAYMENT_METHODS = [
  { value: 'ESEWA', label: 'eSewa', hint: 'Nepal\'s leading mobile wallet' },
  { value: 'KHALTI', label: 'Khalti', hint: 'Mobile wallet and card payments' },
  { value: 'CARD', label: 'Credit / Debit Card', hint: 'Visa, Mastercard, UnionPay' },
  { value: 'WALLET', label: 'Digital Wallet', hint: 'Generic wallet checkout' },
  { value: 'DEMO', label: 'Demo Payment', hint: 'No real gateway is contacted' },
]

/**
 * Builds the initials shown on the instructor avatar, e.g. "Aarav Shrestha" -> "AS".
 */
export function initialsOf(name = '') {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || '')
    .join('')
}

/**
 * Single source of truth for the role -> dashboard path mapping, so the navbar,
 * login, register and route guards can never drift apart.
 */
export function dashboardPathFor(role) {
  if (role === 'ADMIN') return '/admin'
  if (role === 'INSTRUCTOR') return '/instructor'
  return '/student'
}

/**
 * Renders a 5-star string, clamping the value so out-of-range stored data
 * cannot throw a RangeError during render.
 */
export function starRating(rating, max = 5) {
  const value = Number(rating)
  const safe = Number.isFinite(value) ? Math.min(Math.max(Math.round(value), 0), max) : 0

  return `${'★'.repeat(safe)}${'☆'.repeat(max - safe)}`
}
