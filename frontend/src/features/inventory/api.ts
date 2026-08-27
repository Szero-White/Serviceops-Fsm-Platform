import { http } from '../../api/http'
import type { InventoryTransaction, InventoryTransactionType, OutstandingPart, PageResponse, ReturnablePart, SparePart, SparePartImportResult, StocktakeResult, WorkOrderPartRequest, WorkOrderPartRequestStatus, WorkOrderPartUsage } from '../../types'

export const inventoryApi = {
  list: (search = '', page = 0, size = 20, active?: boolean) =>
    http.get<PageResponse<SparePart>>('/spare-parts', { params: { search, page, size, active } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<SparePart>('/spare-parts', payload).then((response) => response.data),
  updateReorderLevel: (id: string, reorderLevel: number) => http.patch<SparePart>(`/spare-parts/${id}/reorder-level`, { reorderLevel }).then((response) => response.data),
  setActive: (id: string, active: boolean) => http.patch<SparePart>(`/spare-parts/${id}/active`, { active }).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/spare-parts/${id}`).then((response) => response.data),
  importStock: (id: string, payload: { quantity: number; note: string }) => http.post<SparePart>(`/spare-parts/${id}/import`, payload).then((response) => response.data),
  stocktake: (id: string, payload: { actualQuantity: number; reason: string }) => http.post<StocktakeResult>(`/spare-parts/${id}/stocktake`, payload).then((response) => response.data),
  transactions: (params: { search?: string; type?: InventoryTransactionType; fromTime?: string; toTime?: string; page?: number; size?: number }) =>
    http.get<PageResponse<InventoryTransaction>>('/inventory-transactions', { params }).then((response) => response.data),
  partRequests: (params: { status?: WorkOrderPartRequestStatus; search?: string; page?: number; size?: number }) =>
    http.get<PageResponse<WorkOrderPartRequest>>('/part-requests', { params }).then((response) => response.data),
  outstandingParts: (search = '') =>
    http.get<OutstandingPart[]>('/part-outstanding', { params: { search } }).then((response) => response.data),
  workOrderPartRequests: (workOrderId: string) =>
    http.get<WorkOrderPartRequest[]>(`/work-orders/${workOrderId}/part-requests`).then((response) => response.data),
  createPartRequest: (workOrderId: string, payload: { sparePartId: string; quantity: number; note: string }) =>
    http.post<WorkOrderPartRequest>(`/work-orders/${workOrderId}/part-requests`, payload).then((response) => response.data),
  updatePartRequest: (requestId: string, payload: { quantity: number; note: string }) =>
    http.patch<WorkOrderPartRequest>(`/part-requests/${requestId}`, payload).then((response) => response.data),
  cancelPartRequest: (requestId: string, reason: string) =>
    http.post<WorkOrderPartRequest>(`/part-requests/${requestId}/cancel`, { reason }).then((response) => response.data),
  markPartRequestUnavailable: (requestId: string, reason: string) =>
    http.post<WorkOrderPartRequest>(`/part-requests/${requestId}/unavailable`, { reason }).then((response) => response.data),
  issuePartRequest: (requestId: string) =>
    http.post<WorkOrderPartRequest>(`/part-requests/${requestId}/issue`).then((response) => response.data),
  workOrderPartUsage: (workOrderId: string) =>
    http.get<WorkOrderPartUsage[]>(`/work-orders/${workOrderId}/part-usage`).then((response) => response.data),
  updatePartUsage: (workOrderId: string, payload: { sparePartId: string; usedQuantity: number }) =>
    http.put<WorkOrderPartUsage>(`/work-orders/${workOrderId}/part-usage`, payload).then((response) => response.data),
  returnable: (workOrderId: string, sparePartId: string) => http.get<ReturnablePart>(`/work-orders/${workOrderId}/parts/${sparePartId}/returnable`).then((response) => response.data),
  returnPart: (workOrderId: string, sparePartId: string, payload: { quantity: number; note: string }) =>
    http.post<ReturnablePart>(`/work-orders/${workOrderId}/parts/${sparePartId}/return`, payload).then((response) => response.data),
  exportCsv: (search = '') => http.get<Blob>('/spare-parts/export', { params: { search }, responseType: 'blob' }).then((response) => response.data),
  importTemplate: () => http.get<Blob>('/spare-parts/import-template', { responseType: 'blob' }).then((response) => response.data),
  importCsv: (file: File, commit = false) => {
    const form = new FormData(); form.append('file', file); form.append('commit', String(commit))
    return http.post<SparePartImportResult>('/spare-parts/import', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((response) => response.data)
  },
}
