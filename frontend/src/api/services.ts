import { http } from './http'
import type {
  Asset,
  AttachmentItem,
  AuditLog,
  AuthResponse,
  Customer,
  Dashboard,
  NotificationItem,
  PageResponse,
  ServiceChannel,
  ServiceRequest,
  SparePart,
  Technician,
  WorkOrder,
  WorkOrderStatus,
} from '../types'

export const authApi = {
  login: (username: string, password: string) => http.post<AuthResponse>('/auth/login', { username, password }).then((r) => r.data),
}

export const dashboardApi = {
  get: () => http.get<Dashboard>('/dashboard').then((r) => r.data),
}

export const customersApi = {
  list: (search = '', page = 0, size = 100) => http.get<PageResponse<Customer>>('/customers', { params: { search, page, size } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<Customer>('/customers', payload).then((r) => r.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Customer>(`/customers/${id}`, payload).then((r) => r.data),
  delete: (id: string) => http.delete<void>(`/customers/${id}`).then((r) => r.data),
}

export const assetsApi = {
  list: (search = '', page = 0, size = 100) => http.get<PageResponse<Asset>>('/assets', { params: { search, page, size } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<Asset>('/assets', payload).then((r) => r.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Asset>(`/assets/${id}`, payload).then((r) => r.data),
  delete: (id: string) => http.delete<void>(`/assets/${id}`).then((r) => r.data),
}

export const serviceRequestsApi = {
  list: (search = '', status?: string, page = 0, size = 100) =>
    http.get<PageResponse<ServiceRequest>>('/service-requests', { params: { search, status, page, size } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<ServiceRequest>('/service-requests', payload).then((r) => r.data),
  cancel: (id: string) => http.post<ServiceRequest>(`/service-requests/${id}/cancel`).then((r) => r.data),
  convert: (id: string) => http.post<WorkOrder>(`/work-orders/from-service-request/${id}`).then((r) => r.data),
}

export const serviceChannelsApi = {
  list: (activeOnly = false) => http.get<ServiceChannel[]>('/service-channels', { params: { activeOnly } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<ServiceChannel>('/service-channels', payload).then((r) => r.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<ServiceChannel>(`/service-channels/${id}`, payload).then((r) => r.data),
  delete: (id: string) => http.delete<void>(`/service-channels/${id}`).then((r) => r.data),
}

export const workOrdersApi = {
  list: (search = '', status?: WorkOrderStatus, page = 0, size = 100) =>
    http.get<PageResponse<WorkOrder>>('/work-orders', { params: { search, status, page, size } }).then((r) => r.data),
  get: (id: string) => http.get<WorkOrder>(`/work-orders/${id}`).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<WorkOrder>('/work-orders', payload).then((r) => r.data),
  schedule: (id: string, payload: { technicianId: string; startTime: string; endTime: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/schedule`, payload).then((r) => r.data),
  transition: (id: string, payload: { targetStatus: WorkOrderStatus; note?: string; diagnosis?: string; resolution?: string }) =>
    http.post<WorkOrder>(`/work-orders/${id}/transition`, payload).then((r) => r.data),
  consumePart: (id: string, payload: { sparePartId: string; quantity: number; note?: string }) =>
    http.post<SparePart>(`/work-orders/${id}/parts/consume`, payload).then((r) => r.data),
}

export const techniciansApi = {
  list: (activeOnly = true) => http.get<Technician[]>('/technicians', { params: { activeOnly } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<Technician>('/technicians', payload).then((r) => r.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Technician>(`/technicians/${id}`, payload).then((r) => r.data),
  delete: (id: string) => http.delete<void>(`/technicians/${id}`).then((r) => r.data),
}

export const inventoryApi = {
  list: (search = '', page = 0, size = 100) => http.get<PageResponse<SparePart>>('/spare-parts', { params: { search, page, size } }).then((r) => r.data),
  create: (payload: Record<string, unknown>) => http.post<SparePart>('/spare-parts', payload).then((r) => r.data),
  importStock: (id: string, payload: { quantity: number; note: string }) => http.post<SparePart>(`/spare-parts/${id}/import`, payload).then((r) => r.data),
}

export const auditApi = {
  list: (page = 0, size = 100) => http.get<PageResponse<AuditLog>>('/audit-logs', { params: { page, size } }).then((r) => r.data),
}

export const notificationsApi = {
  list: () => http.get<PageResponse<NotificationItem>>('/notifications', { params: { page: 0, size: 30 } }).then((r) => r.data),
  unreadCount: () => http.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count),
  markRead: (id: string) => http.patch<NotificationItem>(`/notifications/${id}/read`).then((r) => r.data),
}

export const attachmentsApi = {
  list: (referenceType: string, referenceId: string) => http.get<AttachmentItem[]>('/attachments', { params: { referenceType, referenceId } }).then((r) => r.data),
  upload: (referenceType: string, referenceId: string, file: File) => {
    const form = new FormData()
    form.append('referenceType', referenceType)
    form.append('referenceId', referenceId)
    form.append('file', file)
    return http.post<AttachmentItem>('/attachments', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data)
  },
  downloadUrl: (id: string) => `${http.defaults.baseURL}/attachments/${id}/download`,
}
