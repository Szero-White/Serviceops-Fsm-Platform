import { http } from '../../api/http'
import type { AuditLog, PageResponse } from '../../types'

export interface AuditListParams {
  page?: number
  size?: number
  query?: string
  actor?: string
  action?: string
  entityType?: string
  from?: string
  to?: string
}

function clean(value?: string) {
  const normalized = value?.trim()
  return normalized || undefined
}

export const auditApi = {
  list: (params: AuditListParams = {}) => http.get<PageResponse<AuditLog>>('/audit-logs', {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      q: clean(params.query),
      actor: clean(params.actor),
      action: params.action,
      entityType: params.entityType,
      from: params.from,
      to: params.to,
    },
  }).then((response) => response.data),
}
