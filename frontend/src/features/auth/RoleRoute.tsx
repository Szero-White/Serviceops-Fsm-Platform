import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import type { UserRole } from '../../types'
import { defaultRouteForRole } from '../../router/routeAccess'
import { useAuth } from './AuthContext'

export function RoleRoute({ allowedRoles, children }: { allowedRoles: readonly UserRole[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={defaultRouteForRole(user.role)} replace />
  }
  return children
}
