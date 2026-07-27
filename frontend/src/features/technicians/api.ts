import { http } from '../../api/http'
import type { Technician } from '../../types'

export const techniciansApi = {
  list: (activeOnly = true) => http.get<Technician[]>('/technicians', { params: { activeOnly } }).then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<Technician>('/technicians', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<Technician>(`/technicians/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/technicians/${id}`).then((response) => response.data),
}
