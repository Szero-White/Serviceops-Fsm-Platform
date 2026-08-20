import type { Priority } from './common'

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
  protectedDemo?: boolean
}
