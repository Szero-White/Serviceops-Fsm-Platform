import { http } from '../../api/http'
import type { AuditLog, PageResponse } from '../../types'

export const auditApi = {
  list: (page = 0, size = 100) => http.get<PageResponse<AuditLog>>('/audit-logs', { params: { page, size } }).then((response) => response.data),
}
