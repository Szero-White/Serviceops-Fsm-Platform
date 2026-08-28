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
