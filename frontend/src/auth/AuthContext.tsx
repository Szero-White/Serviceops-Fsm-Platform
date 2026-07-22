import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/services'
import type { CurrentUser } from '../types'

interface AuthContextValue {
  user: CurrentUser | null
  authenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readStoredUser(): CurrentUser | null {
  try {
    const value = localStorage.getItem('serviceops.user')
    return value ? (JSON.parse(value) as CurrentUser) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(readStoredUser)

  const value = useMemo<AuthContextValue>(() => ({
    user,
    authenticated: Boolean(user && localStorage.getItem('serviceops.accessToken')),
    login: async (username: string, password: string) => {
      const response = await authApi.login(username, password)
      localStorage.setItem('serviceops.accessToken', response.accessToken)
      localStorage.setItem('serviceops.user', JSON.stringify(response.user))
      setUser(response.user)
    },
    logout: () => {
      localStorage.removeItem('serviceops.accessToken')
      localStorage.removeItem('serviceops.user')
      setUser(null)
    },
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
