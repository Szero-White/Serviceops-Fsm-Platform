import { http } from '../../api/http'
import type { PageResponse, SparePart, WorkOrder, WorkOrderStatus } from '../../types'

export const workOrdersApi = {
  list: (search = '', status?: WorkOrderStatus, page = 0, size = 100) =>
    http.get<PageResponse<WorkOrder>>('/work-orders', { params: { search, status, page, size } }).then((response) => response.data),
  get: (id: string) => http.get<WorkOrder>(`/work-orders/${id}`).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<WorkOrder>('/work-orders', payload).then((response) => response.data),
  schedule: (id: string, payload: { technicianId: string; startTime: string; endTime: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/schedule`, payload).then((response) => response.data),
  transition: (id: string, payload: { targetStatus: WorkOrderStatus; note?: string; diagnosis?: string; resolution?: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/transition`, payload).then((response) => response.data),
  consumePart: (id: string, payload: { sparePartId: string; quantity: number; note?: string }) =>
    http.post<SparePart>(`/work-orders/${id}/parts/consume`, payload).then((response) => response.data),
}
