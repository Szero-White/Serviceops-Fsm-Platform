import { Tag } from 'antd'
import type { AssetStatus, Priority, RequestChannel, ServiceRequestStatus, WorkOrderStatus } from '../types'

const statusLabels: Record<string, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  SCHEDULED: 'Đã lên lịch',
  ASSIGNED: 'Đã phân công',
  ON_THE_WAY: 'Đang di chuyển',
  IN_PROGRESS: 'Đang thực hiện',
  WAITING_FOR_PARTS: 'Chờ phụ tùng',
  COMPLETED: 'Đã hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách xác nhận',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã huỷ',
  REOPENED: 'Mở lại',
  CONVERTED: 'Đã tạo phiếu',
  ACTIVE: 'Hoạt động',
  IN_SERVICE: 'Đang sửa chữa',
  OUT_OF_SERVICE: 'Tạm ngưng',
  RETIRED: 'Thanh lý',
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
  ACTIVE: 'blue',
  IN_SERVICE: 'purple',
  OUT_OF_SERVICE: 'orange',
  RETIRED: 'default',
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

const fallbackChannelLabels: Record<string, string> = {
  PHONE: 'Điện thoại',
  EMAIL: 'Email',
  WEBSITE: 'Website',
  ZALO: 'Zalo',
  WALK_IN: 'Trực tiếp',
  INTERNAL: 'Nội bộ',
}

export function StatusTag({ status }: { status: WorkOrderStatus | ServiceRequestStatus | AssetStatus }) {
  return <Tag className="status-tag" color={statusColors[status] ?? 'default'}>{statusLabels[status] ?? status}</Tag>
}

export function PriorityTag({ priority }: { priority: Priority }) {
  return <Tag className="status-tag" color={priorityColors[priority]}>{priorityLabels[priority]}</Tag>
}

export function ChannelTag({ channel, label, color }: { channel: RequestChannel; label?: string; color?: string }) {
  return <Tag className="status-tag" color={color}>{label ?? fallbackChannelLabels[channel] ?? channel}</Tag>
}
