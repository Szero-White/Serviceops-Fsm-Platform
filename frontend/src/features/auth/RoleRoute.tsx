import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import type { UserRole } from '../../types'
import { useAuth } from './AuthContext'

export function RoleRoute({ allowedRoles, children }: { allowedRoles: readonly UserRole[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return children
}
