import type { ReactNode } from 'react'
import { Tag } from 'antd'
import type { UserRole } from '../types'
import { USER_ROLE_LABELS } from '../constants/userRoles'
import { auditActionLabel } from '../presentation/businessText'

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

export function RoleTag({ role }: { role: UserRole }) {
  return <Tag className={`${toneClass('neutral')} role-tag`}>{USER_ROLE_LABELS[role]}</Tag>
}

export function WarrantyTag({ underWarranty }: { underWarranty: boolean }) {
  return (
    <Tag className={toneClass(underWarranty ? 'success' : 'danger')}>
      <span className="semantic-tag-dot" aria-hidden="true" />
      {underWarranty ? 'Còn bảo hành' : 'Hết bảo hành'}
    </Tag>
  )
}

export function AuditActionTag({ action }: { action: string }) {
  const tone: SemanticTone = ['CANCEL', 'DELETE', 'DELETE_FILE', 'DELETE_HISTORY'].includes(action) ? 'danger' : 'neutral'
  return <Tag className={`${toneClass(tone)} audit-action-tag`}>{auditActionLabel(action)}</Tag>
}
