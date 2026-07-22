import { Tag } from 'antd'
import type { Priority, ServiceRequestStatus, WorkOrderStatus } from '../types'

const statusLabels: Record<string, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  SCHEDULED: 'Đã lên lịch',
  ASSIGNED: 'Đã phân công',
  ON_THE_WAY: 'Đang di chuyển',
  IN_PROGRESS: 'Đang thực hiện',
  WAITING_FOR_PARTS: 'Chờ phụ tùng',
  COMPLETED: 'Đã hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách đã xác nhận',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã hủy',
  REOPENED: 'Mở lại',
  CONVERTED: 'Đã tạo phiếu',
}

const statusColors: Record<string, string> = {
  DRAFT: 'default',
  OPEN: 'blue',
  SCHEDULED: 'cyan',
  ASSIGNED: 'geekblue',
  ON_THE_WAY: 'purple',
  IN_PROGRESS: 'processing',
  WAITING_FOR_PARTS: 'orange',
  COMPLETED: 'green',
  CUSTOMER_ACCEPTED: 'lime',
  CLOSED: 'success',
  CANCELLED: 'red',
  REOPENED: 'magenta',
  CONVERTED: 'green',
}

const priorityLabels: Record<Priority, string> = {
  LOW: 'Thấp',
  NORMAL: 'Bình thường',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
}

const priorityColors: Record<Priority, string> = {
  LOW: 'default',
  NORMAL: 'blue',
  HIGH: 'orange',
  URGENT: 'red',
}

export function StatusTag({ status }: { status: WorkOrderStatus | ServiceRequestStatus }) {
  return <Tag color={statusColors[status] ?? 'default'}>{statusLabels[status] ?? status}</Tag>
}

export function PriorityTag({ priority }: { priority: Priority }) {
  return <Tag color={priorityColors[priority]}>{priorityLabels[priority]}</Tag>
}
