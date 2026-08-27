import type { UserRole } from '../types'

const OPERATIONAL_DASHBOARD_ROLES: UserRole[] = ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN']

export const ROUTE_ACCESS: Record<string, readonly UserRole[]> = {
  '/': OPERATIONAL_DASHBOARD_ROLES,
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
  '/part-requests': ['OWNER', 'WAREHOUSE_STAFF'],
  '/payments': ['OWNER', 'CUSTOMER_SERVICE'],
  '/payment-settings': ['OWNER'],
  '/inventory-stocktake': ['OWNER', 'WAREHOUSE_STAFF'],
  '/inventory-movements': ['OWNER', 'WAREHOUSE_STAFF'],
  '/audit': ['OWNER', 'DISPATCHER'],
}

export function canAccessRoute(role: UserRole | undefined, path: string) {
  return Boolean(role && ROUTE_ACCESS[path]?.includes(role))
}

export function defaultRouteForRole(role: UserRole): string {
  return role === 'WAREHOUSE_STAFF' ? '/part-requests' : '/'
}
