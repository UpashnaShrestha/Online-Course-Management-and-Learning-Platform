const STORAGE_KEY = 'ocmlp-auth-session'

/**
 * The session lives in localStorage when the user ticked "Remember me" (so it survives
 * closing the browser) and in sessionStorage otherwise (so closing the tab signs them out).
 * Reads check both, writes go to exactly one.
 */

function safeParse(raw) {
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/** Returns whichever store currently holds the session, preferring sessionStorage. */
function activeStorage() {
  try {
    if (window.sessionStorage.getItem(STORAGE_KEY)) {
      return window.sessionStorage
    }
  } catch {
    // Private browsing / disabled storage: fall through.
  }

  return window.localStorage
}

export function readSession() {
  try {
    const fromSession = safeParse(window.sessionStorage.getItem(STORAGE_KEY))
    if (fromSession) return fromSession
  } catch {
    // ignore and try localStorage
  }

  try {
    return safeParse(window.localStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

/**
 * @param {object|null} session
 * @param {boolean} persist  true = remember me (localStorage), false = this tab only.
 */
export function writeSession(session, persist = true) {
  clearSession()

  if (!session) return

  const store = persist ? window.localStorage : window.sessionStorage
  store.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function patchStoredSession(patch) {
  const current = readSession()

  if (!current) return null

  const next = { ...current, ...patch }
  activeStorage().setItem(STORAGE_KEY, JSON.stringify(next))

  return next
}

export function clearSession() {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }

  try {
    window.sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}

/**
 * Whether the current session was written to localStorage (i.e. the user ticked
 * "Remember me"). Checks sessionStorage first because readSession() prefers it.
 */
export function isPersistedSession() {
  try {
    if (window.sessionStorage.getItem(STORAGE_KEY)) return false
  } catch {
    // fall through to localStorage
  }

  try {
    return !!window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return false
  }
}

export { STORAGE_KEY }