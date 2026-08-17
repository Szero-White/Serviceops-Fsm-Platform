import type { UserRole } from './common'

export interface CurrentUser {
  id: string
  username: string
  displayName: string
  role: UserRole
  tenantId: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: CurrentUser
}

export interface UserAccount {
  id: string
  username: string
  displayName: string
  role: UserRole
  active: boolean
  technicianProfileId?: string
  phone?: string
  skills?: string
  createdAt: string
  updatedAt: string
}
