import type { Priority } from './common'
import type { WorkOrderStatus } from './work-order'

export interface ScheduleAppointment {
  appointmentId: string
  workOrderId: string
  workOrderCode: string
  summary: string
  customerName: string
  priority: Priority
  status: WorkOrderStatus
  technicianId: string
  technicianName: string
  startTime: string
  endTime: string
}

export interface DispatchQueueItem {
  workOrderId: string
  workOrderCode: string
  summary: string
  customerName: string
  priority: Priority
  status: WorkOrderStatus
  createdAt: string
}

export interface ScheduleBoard {
  rangeStart: string
  rangeEnd: string
  appointments: ScheduleAppointment[]
  dispatchQueue: DispatchQueueItem[]
  dispatchQueueTotal: number
}

export interface MyScheduleItem {
  appointmentId: string
  workOrderId: string
  workOrderCode: string
  summary: string
  customerName: string
  customerAddress?: string
  assetLabel?: string
  priority: Priority
  status: WorkOrderStatus
  startTime: string
  endTime: string
}

export interface MySchedule {
  technicianId: string
  technicianName: string
  rangeStart: string
  rangeEnd: string
  appointments: MyScheduleItem[]
}
