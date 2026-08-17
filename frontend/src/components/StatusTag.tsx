import { Tag } from 'antd'
import type { AssetStatus, Priority, RequestChannel, ServiceRequestStatus, WorkOrderStatus } from '../types'
import type { SemanticTone } from './PresentationBadge'

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

const statusTones: Record<string, SemanticTone> = {
  DRAFT: 'neutral',
  OPEN: 'info',
  SCHEDULED: 'info',
  ASSIGNED: 'info',
  ON_THE_WAY: 'info',
  IN_PROGRESS: 'info',
  WAITING_FOR_PARTS: 'warning',
  COMPLETED: 'success',
  CUSTOMER_ACCEPTED: 'success',
  CLOSED: 'success',
  CANCELLED: 'danger',
  REOPENED: 'warning',
  CONVERTED: 'success',
  ACTIVE: 'success',
  IN_SERVICE: 'warning',
  OUT_OF_SERVICE: 'warning',
  RETIRED: 'neutral',
}

const priorityLabels: Record<Priority, string> = {
  LOW: 'Thấp',
  NORMAL: 'Bình thường',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
}

const priorityTones: Record<Priority, SemanticTone> = {
  LOW: 'neutral',
  NORMAL: 'neutral',
  HIGH: 'warning',
  URGENT: 'danger',
}

const fallbackChannelLabels: Record<string, string> = {
  PHONE: 'Điện thoại',
  EMAIL: 'Email',
  WEBSITE: 'Website',
  ZALO: 'Zalo',
  WALK_IN: 'Trực tiếp',
  INTERNAL: 'Nội bộ',
}

function semanticClass(tone: SemanticTone) {
  return `semantic-tag semantic-tag--${tone}`
}

const channelColorNames = new Set(['blue', 'green', 'cyan', 'geekblue', 'purple', 'orange', 'red', 'default'])

function channelColorClass(color?: string) {
  const normalized = color?.trim().toLowerCase() ?? 'default'
  return `channel-color-${channelColorNames.has(normalized) ? normalized : 'default'}`
}

export function StatusTag({ status }: { status: WorkOrderStatus | ServiceRequestStatus | AssetStatus }) {
  return (
    <Tag className={semanticClass(statusTones[status] ?? 'neutral')}>
      <span className="semantic-tag-dot" aria-hidden="true" />
      {statusLabels[status] ?? status}
    </Tag>
  )
}

export function PriorityTag({ priority }: { priority: Priority }) {
  return (
    <Tag className={`${semanticClass(priorityTones[priority])} priority-tag`}>
      <span className="semantic-tag-dot" aria-hidden="true" />
      {priorityLabels[priority]}
    </Tag>
  )
}

export function ChannelTag({ channel, label, color }: { channel: RequestChannel; label?: string; color?: string }) {
  return (
    <Tag className={`${semanticClass('neutral')} channel-tag`}>
      <span className={`channel-tag-dot ${channelColorClass(color)}`} aria-hidden="true" />
      {label ?? fallbackChannelLabels[channel] ?? channel}
    </Tag>
  )
}
