import type { UserRole } from '../types'

export type ActorRole = UserRole | 'SYSTEM'

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  OWNER: 'Chủ sở hữu',
  DISPATCHER: 'Điều phối viên',
  CUSTOMER_SERVICE: 'Chăm sóc khách hàng',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
}

export const ACTOR_ROLE_LABELS: Record<ActorRole, string> = {
  ...USER_ROLE_LABELS,
  SYSTEM: 'Hệ thống',
}

export function actorRoleLabel(role?: string) {
  return role ? (ACTOR_ROLE_LABELS[role as ActorRole] ?? role) : 'Không xác định'
}
