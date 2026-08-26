import type { ReactNode } from 'react'
import { Tag } from 'antd'
import type { UserRole } from '../types'

export type SemanticTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

function toneClass(tone: SemanticTone) {
  return `semantic-tag semantic-tag--${tone}`
}

export function MetaBadge({
  children,
  tone = 'neutral',
}: {
  children: ReactNode
  tone?: SemanticTone
}) {
  return <Tag className={`${toneClass(tone)} meta-badge`}>{children}</Tag>
}

export function BinaryStatusTag({
  active,
  activeLabel = 'Hoạt động',
  inactiveLabel = 'Tạm ngưng',
}: {
  active: boolean
  activeLabel?: string
  inactiveLabel?: string
}) {
  return (
    <Tag className={toneClass(active ? 'success' : 'warning')}>
      <span className="semantic-tag-dot" aria-hidden="true" />
      {active ? activeLabel : inactiveLabel}
    </Tag>
  )
}

const roleLabels: Record<UserRole, string> = {
  OWNER: 'Chủ sở hữu',
  DISPATCHER: 'Điều phối',
  CUSTOMER_SERVICE: 'CSKH',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
}

export function RoleTag({ role }: { role: UserRole }) {
  return <Tag className={`${toneClass('neutral')} role-tag`}>{roleLabels[role]}</Tag>
}

export function WarrantyTag({ underWarranty }: { underWarranty: boolean }) {
  return (
    <Tag className={toneClass(underWarranty ? 'success' : 'danger')}>
      <span className="semantic-tag-dot" aria-hidden="true" />
      {underWarranty ? 'Còn bảo hành' : 'Hết bảo hành'}
    </Tag>
  )
}

export const AUDIT_ACTION_LABELS: Record<string, string> = {
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
  ASSIGN: 'Phân công',
  RESCHEDULE: 'Điều chỉnh lịch',
  CHANGE_STATUS: 'Đổi trạng thái',
  CANCEL: 'Hủy',
  CONSUME_PART: 'Xuất phụ tùng',
  IMPORT_STOCK: 'Nhập kho',
  IMPORT_CUSTOMERS: 'Import khách hàng',
  IMPORT_ASSETS: 'Import thiết bị',
  IMPORT_SPARE_PARTS: 'Import phụ tùng',
  UPLOAD_FILE: 'Tải tệp',
  RENAME_FILE: 'Đổi tên tệp',
  DELETE_FILE: 'Xóa tệp',
  DELETE_HISTORY: 'Xóa khỏi lịch sử',
  AI_HELP_LOCAL: 'Trợ lý nội bộ',
  AI_HELP_GEMINI: 'Trợ lý AI',
  AI_HELP_FALLBACK: 'Trợ lý dự phòng',
  AI_DRAFT_LOCAL: 'Gợi ý nội bộ',
  AI_DRAFT_GEMINI: 'Gợi ý AI',
  AI_DRAFT_FALLBACK: 'Gợi ý dự phòng',
  SEED: 'Khởi tạo dữ liệu',
}

export function AuditActionTag({ action }: { action: string }) {
  const tone: SemanticTone = ['CANCEL', 'DELETE', 'DELETE_FILE', 'DELETE_HISTORY'].includes(action) ? 'danger' : 'neutral'
  return <Tag className={`${toneClass(tone)} audit-action-tag`}>{AUDIT_ACTION_LABELS[action] ?? action}</Tag>
}
