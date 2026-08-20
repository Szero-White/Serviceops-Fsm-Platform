import { http } from '../../api/http'
import type { Technician } from '../../types'

export type TechnicianProfileUpdate = {
  phone?: string
  skills?: string
  active?: boolean
}

export const techniciansApi = {
  list: (activeOnly = true) =>
    http
      .get<Technician[]>('/technicians', { params: { activeOnly } })
      .then((response) => response.data),

  updateProfile: (id: string, payload: TechnicianProfileUpdate) =>
    http
      .put<Technician>(`/technicians/${id}`, payload)
      .then((response) => response.data),
}
