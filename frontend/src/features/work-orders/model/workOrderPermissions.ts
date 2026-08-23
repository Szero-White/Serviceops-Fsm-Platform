import type { UserRole } from '../../../types'

export type WorkOrderPermissions = {
  canCreate: boolean
  canSchedule: boolean
  canTransition: boolean
  canConsumePart: boolean
}

export function workOrderPermissions(role?: UserRole): WorkOrderPermissions {
  return {
    canCreate: role ? ['OWNER', 'CUSTOMER_SERVICE', 'DISPATCHER'].includes(role) : false,
    canSchedule: role ? ['OWNER', 'DISPATCHER'].includes(role) : false,
    canTransition: role ? ['OWNER', 'CUSTOMER_SERVICE', 'DISPATCHER', 'TECHNICIAN'].includes(role) : false,
    canConsumePart: role ? ['OWNER', 'WAREHOUSE_STAFF', 'TECHNICIAN'].includes(role) : false,
  }
}
