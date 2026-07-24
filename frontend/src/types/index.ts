export type UserRole = 'OWNER' | 'DISPATCHER' | 'CUSTOMER_SERVICE' | 'TECHNICIAN' | 'WAREHOUSE_STAFF'
export type Priority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type WorkOrderStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'SCHEDULED'
  | 'ASSIGNED'
  | 'ON_THE_WAY'
  | 'IN_PROGRESS'
  | 'WAITING_FOR_PARTS'
  | 'COMPLETED'
  | 'CUSTOMER_ACCEPTED'
  | 'CLOSED'
  | 'CANCELLED'
  | 'REOPENED'

export type ServiceRequestStatus = 'OPEN' | 'CONVERTED' | 'CANCELLED'
export type RequestChannel = string
export type AssetStatus = 'ACTIVE' | 'IN_SERVICE' | 'OUT_OF_SERVICE' | 'RETIRED'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

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

export interface Customer {
  id: string
  code: string
  name: string
  phone?: string
  email?: string
  address?: string
  notes?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface Asset {
  id: string
  customerId: string
  customerName: string
  category: string
  brand?: string
  model?: string
  serialNumber: string
  installedAt?: string
  warrantyUntil?: string
  underWarranty: boolean
  status: AssetStatus
  notes?: string
  createdAt: string
}

export interface ServiceRequest {
  id: string
  customerId: string
  customerName: string
  assetId?: string
  assetLabel?: string
  title: string
  description: string
  priority: Priority
  channel: RequestChannel
  status: ServiceRequestStatus
  createdBy: string
  createdAt: string
}

export interface ServiceChannel {
  id: string
  code: string
  name: string
  description?: string
  color: string
  sortOrder: number
  active: boolean
  systemDefined: boolean
  createdAt: string
  updatedAt: string
}

export interface WorkOrderHistory {
  id: string
  fromStatus?: WorkOrderStatus
  toStatus: WorkOrderStatus
  note?: string
  changedBy: string
  createdAt: string
}

export interface WorkOrder {
  id: string
  code: string
  serviceRequestId?: string
  customerId: string
  customerName: string
  assetId?: string
  assetLabel?: string
  technicianId?: string
  technicianName?: string
  summary: string
  description?: string
  priority: Priority
  status: WorkOrderStatus
  scheduledStart?: string
  scheduledEnd?: string
  diagnosis?: string
  resolution?: string
  completedAt?: string
  createdAt: string
  history: WorkOrderHistory[]
}

export interface Technician {
  id: string
  userId: string
  name: string
  username: string
  phone?: string
  skills?: string
  active: boolean
  accountActive: boolean
}

export interface SparePart {
  id: string
  sku: string
  name: string
  unit: string
  stockQuantity: number
  reorderLevel: number
  unitPrice: number
  lowStock: boolean
  active: boolean
  updatedAt: string
}

export interface Dashboard {
  customers: number
  assets: number
  openServiceRequests: number
  activeTechnicians: number
  openWorkOrders: number
  assignedWorkOrders: number
  inProgressWorkOrders: number
  waitingForPartsWorkOrders: number
  completedWorkOrders: number
  closedWorkOrders: number
  lowStockParts: number
  recentWorkOrders: Array<{
    id: string
    code: string
    summary: string
    customerName: string
    technicianName?: string
    status: WorkOrderStatus
    priority: Priority
    scheduledStart?: string
  }>
}

export interface AuditLog {
  id: string
  actorUsername: string
  action: string
  entityType: string
  entityId?: string
  details?: string
  createdAt: string
}

export interface NotificationItem {
  id: string
  title: string
  message: string
  readAt?: string
  createdAt: string
}

export interface AttachmentItem {
  id: string
  originalFilename: string
  contentType: string
  fileSize: number
  referenceType: string
  referenceId: string
  uploadedBy: string
  createdAt: string
}
