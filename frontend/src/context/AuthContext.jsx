import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from '../services/api'
import { clearSession, isPersistedSession, readSession, writeSession } from '../services/session'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)
  // Drives whether the session is kept in localStorage (remember me) or sessionStorage.
  const [persist, setPersist] = useState(isPersistedSession)

  useEffect(() => {
    if (session) {
      // Never persist a stale token: api.js may have rotated both tokens since login,
      // so always carry the freshest ones forward from storage.
      const stored = readSession()
      const merged = {
        ...session,
        accessToken: stored?.accessToken ?? session.accessToken,
        refreshToken: stored?.refreshToken ?? session.refreshToken,
      }

      writeSession(merged, persist)
    } else {
      clearSession()
    }
  }, [session, persist])

  const login = useCallback(async ({ username, password, rememberMe = false }) => {
    const response = await api.post('/api/auth/login', { username, password, rememberMe })
    const payload = response.data?.data || response.data

    if (!payload?.user || !payload?.accessToken) {
      throw new Error('Login response was incomplete')
    }

    const nextSession = {
      ...payload.user,
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      role: payload.user.role || 'STUDENT',
    }
    setPersist(rememberMe)
    setSession(nextSession)
    return nextSession
  }, [])

  const register = useCallback(async ({ username, firstName, lastName, email, password }) => {
    const response = await api.post('/api/auth/register', { username, firstName, lastName, email, password })
    return response.data?.data || response.data
  }, [])

  const logout = useCallback(() => {
    setSession(null)
    clearSession()
  }, [])

  /**
   * Merge freshly loaded/edited user fields into the session so the navbar name,
   * role and avatar stay in sync without a re-login. Tokens are never touched.
   */
  const patchSession = useCallback((patch) => {
    setSession((current) => (current ? { ...current, ...patch } : current))
  }, [])

  const value = useMemo(
    () => ({
      session,
      login,
      register,
      logout,
      patchSession,
      isAuthenticated: !!session,
    }),
    [session, login, register, logout, patchSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
