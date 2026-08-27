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
  { value: 'CANCELLED', label: 'Đã hủy' },
  { value: 'REOPENED', label: 'Mở lại' },
]

export const ACTIVE_WORK_ORDER_STATUS_OPTIONS = WORK_ORDER_STATUS_OPTIONS.filter(
  (option) => option.value !== 'CLOSED' && option.value !== 'CANCELLED',
)

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
  REOPENED: 'Khách yêu cầu xử lý lại',
  CANCELLED: 'Hủy phiếu',
}

const TRANSITIONS: Partial<Record<WorkOrderStatus, WorkOrderStatus[]>> = {
  OPEN: ['CANCELLED'],
  SCHEDULED: ['CANCELLED'],
  ASSIGNED: ['ON_THE_WAY', 'CANCELLED'],
  ON_THE_WAY: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED'],
  WAITING_FOR_PARTS: ['IN_PROGRESS', 'CANCELLED'],
  COMPLETED: ['CUSTOMER_ACCEPTED', 'REOPENED'],
  CUSTOMER_ACCEPTED: ['CLOSED', 'REOPENED'],
  REOPENED: ['IN_PROGRESS', 'CANCELLED'],
}

export const WORK_ORDER_PART_CONSUMPTION_STATUSES = new Set<WorkOrderStatus>([
  'ASSIGNED',
  'ON_THE_WAY',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'REOPENED',
])

export function canConsumePartInStatus(status: WorkOrderStatus): boolean {
  return WORK_ORDER_PART_CONSUMPTION_STATUSES.has(status)
}

const ROLE_ALLOWED_TRANSITIONS: Partial<Record<UserRole, ReadonlySet<WorkOrderStatus>>> = {
  OWNER: new Set(['CUSTOMER_ACCEPTED', 'CLOSED', 'REOPENED', 'CANCELLED']),
  TECHNICIAN: new Set([
    'ON_THE_WAY',
    'IN_PROGRESS',
    'WAITING_FOR_PARTS',
    'COMPLETED',
    'CUSTOMER_ACCEPTED',
    'CLOSED',
    'REOPENED',
  ]),
  CUSTOMER_SERVICE: new Set(['REOPENED', 'CANCELLED']),
  DISPATCHER: new Set(['CANCELLED']),
}

export function availableWorkOrderTransitions(status: WorkOrderStatus, role?: UserRole): WorkOrderStatus[] {
  const transitions = TRANSITIONS[status] ?? []
  const allowedTransitions = role ? ROLE_ALLOWED_TRANSITIONS[role] : undefined
  return allowedTransitions
    ? transitions.filter((target) => allowedTransitions.has(target))
    : transitions
}
