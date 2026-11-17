import axios from 'axios'
import { clearSession, patchStoredSession, readSession } from './session'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Only these are safe to replay. A POST that was already committed server-side
// (enroll, pay, submit, mark complete) must never be retried after a 401.
const REPLAYABLE_METHODS = new Set(['get', 'head', 'options', 'put', 'delete'])

api.interceptors.request.use((config) => {
  const accessToken = readSession()?.accessToken

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

/**
 * Access tokens are short lived, so a 401 is expected during a normal session.
 * Exchange the stored refresh token for a new pair instead of forcing a logout.
 * Concurrent 401s share one in-flight refresh.
 */
let refreshPromise = null

async function refreshAccessToken() {
  const refreshToken = readSession()?.refreshToken

  if (!refreshToken) {
    return false
  }

  try {
    // Bare axios (not `api`) so a failed refresh cannot recurse through this interceptor.
    const { data } = await axios.post(`${BASE_URL}/api/auth/refresh`, { refreshToken })
    const payload = data?.data || data

    if (!payload?.accessToken) {
      return false
    }

    patchStoredSession({
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken || refreshToken,
    })

    return true
  } catch {
    return false
  }
}

function runRefresh() {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

let signingOut = false

function signOut() {
  // Guard against N concurrent 401s each triggering a full-page navigation.
  if (signingOut || window.location.pathname === '/login') {
    return
  }

  signingOut = true
  clearSession()

  const target = `${window.location.pathname}${window.location.search}`
  window.location.assign(`/login?redirect=${encodeURIComponent(target)}`)
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const method = (originalRequest?.method || 'get').toLowerCase()

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retried ||
      !REPLAYABLE_METHODS.has(method)
    ) {
      // A failed non-replayable request still means the session is gone.
      if (error.response?.status === 401 && !REPLAYABLE_METHODS.has(method)) {
        signOut()
      }

      return Promise.reject(error)
    }

    originalRequest._retried = true

    const refreshed = await runRefresh()

    if (!refreshed) {
      signOut()
      return Promise.reject(error)
    }

    const accessToken = readSession()?.accessToken

    // The session may have been cleared while the refresh was in flight.
    if (!accessToken) {
      signOut()
      return Promise.reject(error)
    }

    originalRequest.headers = { ...originalRequest.headers, Authorization: `Bearer ${accessToken}` }
    return api(originalRequest)
  },
)

export default api