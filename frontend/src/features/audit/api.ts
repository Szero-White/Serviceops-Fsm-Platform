import { http } from '../../api/http'
import type { AuditLog, PageResponse } from '../../types'

export interface AuditListParams {
  page?: number
  size?: number
  query?: string
  actor?: string
  actions?: string[]
  entityTypes?: string[]
  from?: string
  to?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
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
      action: params.actions?.length ? params.actions.join(',') : undefined,
      entityType: params.entityTypes?.length ? params.entityTypes.join(',') : undefined,
      from: params.from,
      to: params.to,
      sortBy: params.sortBy ?? 'createdAt',
      sortDir: params.sortDir ?? 'desc',
    },
  }).then((response) => response.data),
}
