import { http } from '../../api/http'
import type { Customer, CustomerImportResult, PageResponse } from '../../types'

export const customersApi = {
  list: (search = '', page = 0, size = 20, active?: boolean) =>
    http.get<PageResponse<Customer>>('/customers', { params: { search, page, size, active } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<Customer>('/customers', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Customer>(`/customers/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/customers/${id}`).then((response) => response.data),
  exportCsv: (search = '') =>
    http.get<Blob>('/customers/export', { params: { search }, responseType: 'blob' }).then((response) => response.data),
  importTemplate: () =>
    http.get<Blob>('/customers/import-template', { responseType: 'blob' }).then((response) => response.data),
  importCsv: (file: File, commit = false) => {
    const form = new FormData()
    form.append('file', file)
    form.append('commit', String(commit))
    return http.post<CustomerImportResult>('/customers/import', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((response) => response.data)
  },
}
