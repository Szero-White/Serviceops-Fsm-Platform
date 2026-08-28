import { http } from '../../api/http'
import type { PageResponse, WorkOrder, WorkOrderActivity, WorkOrderStatus } from '../../types'

export const workOrdersApi = {
  list: (search = '', status?: WorkOrderStatus, page = 0, size = 20) =>
    http.get<PageResponse<WorkOrder>>('/work-orders', { params: { search, status, page, size } }).then((response) => response.data),
  history: (search = '', status?: Extract<WorkOrderStatus, 'CLOSED' | 'CANCELLED'>, page = 0, size = 20) =>
    http.get<PageResponse<WorkOrder>>('/work-orders/history', { params: { search, status, page, size } }).then((response) => response.data),
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
