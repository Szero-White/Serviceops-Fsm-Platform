import type { UserRole, WorkOrderStatus } from '../../../types'

export const WORK_ORDER_STATUS_OPTIONS = [
  { value: 'OPEN', label: 'Đang mở' },
  { value: 'SCHEDULED', label: 'Đã lên lịch' },
  { value: 'ASSIGNED', label: 'Đã phân công' },
  { value: 'ON_THE_WAY', label: 'Đang di chuyển' },
  { value: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { value: 'WAITING_FOR_PARTS', label: 'Chờ phụ tùng' },
  { value: 'COMPLETED', label: 'Đã hoàn thành' },
  { value: 'CUSTOMER_ACCEPTED', label: 'Khách xác nhận' },
  { value: 'CLOSED', label: 'Đã đóng' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
  { value: 'REOPENED', label: 'Mở lại' },
]

export const PRIORITY_OPTIONS = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

export const TRANSITION_LABELS: Partial<Record<WorkOrderStatus, string>> = {
  ON_THE_WAY: 'Bắt đầu di chuyển',
  IN_PROGRESS: 'Bắt đầu / tiếp tục',
  WAITING_FOR_PARTS: 'Chờ phụ tùng',
  COMPLETED: 'Hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách xác nhận',
  CLOSED: 'Đóng phiếu',
  REOPENED: 'Mở lại',
  CANCELLED: 'Huỷ phiếu',
}

const TRANSITIONS: Partial<Record<WorkOrderStatus, WorkOrderStatus[]>> = {
  ASSIGNED: ['ON_THE_WAY', 'CANCELLED'],
  ON_THE_WAY: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED'],
  WAITING_FOR_PARTS: ['IN_PROGRESS', 'CANCELLED'],
  COMPLETED: ['CUSTOMER_ACCEPTED', 'REOPENED'],
  CUSTOMER_ACCEPTED: ['CLOSED', 'REOPENED'],
  REOPENED: ['IN_PROGRESS', 'CANCELLED'],
  CANCELLED: ['REOPENED'],
}

const TECHNICIAN_ALLOWED_TRANSITIONS = new Set<WorkOrderStatus>([
  'ON_THE_WAY',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'COMPLETED',
])

export function availableWorkOrderTransitions(status: WorkOrderStatus, role?: UserRole): WorkOrderStatus[] {
  const transitions = TRANSITIONS[status] ?? []
  if (role !== 'TECHNICIAN') return transitions
  return transitions.filter((target) => TECHNICIAN_ALLOWED_TRANSITIONS.has(target))
}
