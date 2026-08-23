import type { UserRole } from '../types'

const ALL_ROLES: UserRole[] = ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN', 'WAREHOUSE_STAFF']

export const ROUTE_ACCESS: Record<string, readonly UserRole[]> = {
  '/': ALL_ROLES,
  '/users': ['OWNER'],
  '/customers': ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'],
  '/assets': ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'],
  '/service-requests': ['OWNER', 'CUSTOMER_SERVICE'],
  '/service-channels': ['OWNER', 'CUSTOMER_SERVICE'],
  '/work-orders': ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN'],
  '/schedule': ['OWNER', 'DISPATCHER'],
  '/my-schedule': ['TECHNICIAN'],
  '/work-order-history': ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN'],
  '/technicians': ['OWNER', 'DISPATCHER'],
  '/inventory': ['OWNER', 'WAREHOUSE_STAFF', 'TECHNICIAN'],
  '/audit': ['OWNER', 'DISPATCHER'],
}

export function canAccessRoute(role: UserRole | undefined, path: string) {
  return Boolean(role && ROUTE_ACCESS[path]?.includes(role))
}
