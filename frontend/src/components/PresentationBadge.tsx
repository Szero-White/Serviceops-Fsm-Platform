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

const auditActionLabels: Record<string, string> = {
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  ASSIGN: 'Phân công',
  CHANGE_STATUS: 'Đổi trạng thái',
  CONSUME_PART: 'Xuất phụ tùng',
  IMPORT_STOCK: 'Nhập kho',
  CANCEL: 'Hủy',
  UPLOAD_FILE: 'Tải tệp',
  AI_HELP_LOCAL: 'Trợ lý nội bộ',
  AI_HELP_GEMINI: 'Trợ lý AI',
  SEED: 'Khởi tạo dữ liệu',
}

export function AuditActionTag({ action }: { action: string }) {
  const tone: SemanticTone = action === 'CANCEL' ? 'danger' : 'neutral'
  return <Tag className={`${toneClass(tone)} audit-action-tag`}>{auditActionLabels[action] ?? action}</Tag>
}
