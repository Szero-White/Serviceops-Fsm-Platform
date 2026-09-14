import { http } from '../../api/http'
import type { PageResponse, WorkOrder, WorkOrderActivity, WorkOrderHistorySummary, WorkOrderStatus } from '../../types'

export const workOrdersApi = {
  list: (search = '', statuses: WorkOrderStatus[] = [], page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    http.get<PageResponse<WorkOrder>>('/work-orders', { params: { search, status: statuses.length ? statuses.join(',') : undefined, page, size, sortBy, sortDir } }).then((response) => response.data),
  history: (search = '', statuses: WorkOrderStatus[] = [], page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    http.get<PageResponse<WorkOrder>>('/work-orders/history', { params: { search, status: statuses.length ? statuses.join(',') : undefined, page, size, sortBy, sortDir } }).then((response) => response.data),
  historySummary: (search = '') =>
    http.get<WorkOrderHistorySummary>('/work-orders/history/summary', { params: { search } }).then((response) => response.data),
  get: (id: string) => http.get<WorkOrder>(`/work-orders/${id}`).then((response) => response.data),
  schedule: (id: string, payload: { technicianId: string; startTime: string; endTime: string; reason?: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/schedule`, payload).then((response) => response.data),
  transition: (id: string, payload: { targetStatus: WorkOrderStatus; note?: string; diagnosis?: string; resolution?: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/transition`, payload).then((response) => response.data),
  timeline: (id: string) =>
    http.get<WorkOrderActivity[]>(`/work-orders/${id}/timeline`).then((response) => response.data),
  close: (id: string) =>
    http.post<WorkOrder>(`/work-orders/${id}/close`).then((response) => response.data),
  deleteFromHistory: (id: string) => http.delete<void>(`/work-orders/${id}`).then((response) => response.data),
}
