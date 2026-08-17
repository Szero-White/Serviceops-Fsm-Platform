export type UserRole = 'OWNER' | 'DISPATCHER' | 'CUSTOMER_SERVICE' | 'TECHNICIAN' | 'WAREHOUSE_STAFF'
export type Priority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
