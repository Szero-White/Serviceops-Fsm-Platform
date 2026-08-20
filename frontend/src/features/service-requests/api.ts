import { http } from '../../api/http'
import type { PageResponse, ServiceRequest, WorkOrder } from '../../types'

export const serviceRequestsApi = {
  list: (search = '', status?: string, page = 0, size = 20) =>
    http.get<PageResponse<ServiceRequest>>('/service-requests', { params: { search, status, page, size } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<ServiceRequest>('/service-requests', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<ServiceRequest>(`/service-requests/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/service-requests/${id}`).then((response) => response.data),
  cancel: (id: string) => http.post<ServiceRequest>(`/service-requests/${id}/cancel`).then((response) => response.data),
  convert: (id: string) => http.post<WorkOrder>(`/work-orders/from-service-request/${id}`).then((response) => response.data),
}
