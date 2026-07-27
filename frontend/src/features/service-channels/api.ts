import { http } from '../../api/http'
import type { ServiceChannel } from '../../types'

export const serviceChannelsApi = {
  list: (activeOnly = false) => http.get<ServiceChannel[]>('/service-channels', { params: { activeOnly } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<ServiceChannel>('/service-channels', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<ServiceChannel>(`/service-channels/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/service-channels/${id}`).then((response) => response.data),
}
