import { http } from '../../api/http'
import type { PageResponse, SparePart } from '../../types'

export const inventoryApi = {
  list: (search = '', page = 0, size = 100) =>
    http.get<PageResponse<SparePart>>('/spare-parts', { params: { search, page, size } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<SparePart>('/spare-parts', payload).then((response) => response.data),
  importStock: (id: string, payload: { quantity: number; note: string }) =>
    http.post<SparePart>(`/spare-parts/${id}/import`, payload).then((response) => response.data),
}
