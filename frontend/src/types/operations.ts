import type { Priority } from './common'
import type { WorkOrderStatus } from './work-order'

export interface Dashboard {
  customers: number
  assets: number
  openServiceRequests: number
  activeTechnicians: number
  openWorkOrders: number
  scheduledWorkOrders: number
  assignedWorkOrders: number
  onTheWayWorkOrders: number
  inProgressWorkOrders: number
  waitingForPartsWorkOrders: number
  completedWorkOrders: number
  customerAcceptedWorkOrders: number
  closedWorkOrders: number
  reopenedWorkOrders: number
  cancelledWorkOrders: number
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

export type AttachmentPurpose = 'GENERAL' | 'WORK_EVIDENCE' | 'PAYMENT_EVIDENCE'

export interface AttachmentItem {
  id: string
  originalFilename: string
  contentType: string
  fileSize: number
  referenceType: string
  referenceId: string
  uploadedBy: string
  purpose: AttachmentPurpose
  locked: boolean
  manageable: boolean
  createdAt: string
}

export interface AiHelpResponse {
  answer: string
  steps: string[]
  relatedRoute: string
  actionLabel: string
  provider: 'local' | 'gemini' | string
}
