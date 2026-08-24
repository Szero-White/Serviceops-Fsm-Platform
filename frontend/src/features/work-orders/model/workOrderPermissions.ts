import type { UserRole } from '../../../types'

export type WorkOrderPermissions = {
  canSchedule: boolean
  canTransition: boolean
  canConsumePart: boolean
}

export function workOrderPermissions(role?: UserRole): WorkOrderPermissions {
  return {
    canSchedule: role ? ['OWNER', 'DISPATCHER'].includes(role) : false,
    canTransition: role ? ['OWNER', 'CUSTOMER_SERVICE', 'DISPATCHER', 'TECHNICIAN'].includes(role) : false,
    canConsumePart: role === 'TECHNICIAN',
  }
}
