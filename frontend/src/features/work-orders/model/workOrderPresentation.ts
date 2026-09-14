import type { UserRole, WorkOrderStatus } from '../../../types'
import { businessCodeLabel } from '../../../presentation/businessText'

const WORK_ORDER_FILTER_STATUSES: WorkOrderStatus[] = [
  'OPEN',
  'SCHEDULED',
  'ASSIGNED',
  'ON_THE_WAY',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'COMPLETED',
  'CUSTOMER_ACCEPTED',
  'CLOSED',
  'CANCELLED',
  'REOPENED',
]

export const WORK_ORDER_STATUS_OPTIONS = WORK_ORDER_FILTER_STATUSES.map((value) => ({
  value,
  label: businessCodeLabel(value, 'Trạng thái khác'),
}))

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
  CUSTOMER_ACCEPTED: 'Ghi nhận khách xác nhận',
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
  CUSTOMER_ACCEPTED: ['CLOSED'],
  REOPENED: ['IN_PROGRESS', 'CANCELLED'],
}

const ROLE_ALLOWED_TRANSITIONS: Partial<Record<UserRole, ReadonlySet<WorkOrderStatus>>> = {
  OWNER: new Set(['CANCELLED']),
  TECHNICIAN: new Set([
    'ON_THE_WAY',
    'IN_PROGRESS',
    'WAITING_FOR_PARTS',
    'COMPLETED',
    'CUSTOMER_ACCEPTED',
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
