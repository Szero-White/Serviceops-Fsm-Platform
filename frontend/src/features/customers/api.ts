import { http } from '../../api/http'
import type { Customer, PageResponse } from '../../types'

export const customersApi = {
  list: (search = '', page = 0, size = 100) =>
    http.get<PageResponse<Customer>>('/customers', { params: { search, page, size } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<Customer>('/customers', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Customer>(`/customers/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/customers/${id}`).then((response) => response.data),
}
