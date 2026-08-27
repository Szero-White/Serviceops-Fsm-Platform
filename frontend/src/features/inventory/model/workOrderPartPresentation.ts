import type { WorkOrderPartRequestStatus, WorkOrderStatus } from '../../../types'

export const PART_REQUEST_STATUS_LABELS: Record<WorkOrderPartRequestStatus, string> = {
  REQUESTED: 'Chờ cấp',
  ISSUED: 'Đã cấp',
  CANCELLED: 'Đã hủy',
  UNAVAILABLE: 'Không thể cấp',
  EXPIRED: 'Hết hiệu lực',
}

export const PART_REQUEST_STATUS_OPTIONS = Object.entries(PART_REQUEST_STATUS_LABELS).map(([value, label]) => ({
  value: value as WorkOrderPartRequestStatus,
  label,
}))

const PART_REQUEST_ALLOWED_STATUSES = new Set<WorkOrderStatus>([
  'ASSIGNED',
  'ON_THE_WAY',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'REOPENED',
])

const PART_USAGE_ALLOWED_STATUSES = new Set<WorkOrderStatus>([
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'REOPENED',
  'COMPLETED',
])

export function canRequestPart(status: WorkOrderStatus) {
  return PART_REQUEST_ALLOWED_STATUSES.has(status)
}

export function canConfirmPartUsage(status: WorkOrderStatus) {
  return PART_USAGE_ALLOWED_STATUSES.has(status)
}
