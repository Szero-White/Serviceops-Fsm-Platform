import { http } from '../../api/http'
import type { Asset, AssetImportResult, PageResponse } from '../../types'

export const assetsApi = {
  list: (search = '', page = 0, size = 100) =>
    http.get<PageResponse<Asset>>('/assets', { params: { search, page, size } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<Asset>('/assets', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Asset>(`/assets/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/assets/${id}`).then((response) => response.data),
  exportCsv: (search = '') =>
    http.get<Blob>('/assets/export', { params: { search }, responseType: 'blob' }).then((response) => response.data),
  importTemplate: () =>
    http.get<Blob>('/assets/import-template', { responseType: 'blob' }).then((response) => response.data),
  importCsv: (file: File, commit = false) => {
    const form = new FormData()
    form.append('file', file)
    form.append('commit', String(commit))
    return http.post<AssetImportResult>('/assets/import', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((response) => response.data)
  },
}
