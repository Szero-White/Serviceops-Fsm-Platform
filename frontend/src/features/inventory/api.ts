import { http } from '../../api/http'
import type { PageResponse, SparePart, SparePartImportResult } from '../../types'

export const inventoryApi = {
  list: (search = '', page = 0, size = 20, active?: boolean) =>
    http.get<PageResponse<SparePart>>('/spare-parts', { params: { search, page, size, active } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<SparePart>('/spare-parts', payload).then((response) => response.data),
  setActive: (id: string, active: boolean) =>
    http.patch<SparePart>(`/spare-parts/${id}/active`, { active }).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/spare-parts/${id}`).then((response) => response.data),
  importStock: (id: string, payload: { quantity: number; note: string }) =>
    http.post<SparePart>(`/spare-parts/${id}/import`, payload).then((response) => response.data),
  exportCsv: (search = '') =>
    http.get<Blob>('/spare-parts/export', { params: { search }, responseType: 'blob' }).then((response) => response.data),
  importTemplate: () =>
    http.get<Blob>('/spare-parts/import-template', { responseType: 'blob' }).then((response) => response.data),
  importCsv: (file: File, commit = false) => {
    const form = new FormData()
    form.append('file', file)
    form.append('commit', String(commit))
    return http.post<SparePartImportResult>('/spare-parts/import', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((response) => response.data)
  },
}
