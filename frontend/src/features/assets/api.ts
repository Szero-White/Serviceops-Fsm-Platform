import { http } from '../../api/http'
import type { Asset, PageResponse } from '../../types'

export const assetsApi = {
  list: (search = '', page = 0, size = 100) =>
    http.get<PageResponse<Asset>>('/assets', { params: { search, page, size } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<Asset>('/assets', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Asset>(`/assets/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/assets/${id}`).then((response) => response.data),
}
