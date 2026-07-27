import { http } from '../../api/http'
import type { UserAccount } from '../../types'

export const usersApi = {
  list: () => http.get<UserAccount[]>('/users').then((response) => response.data),
  create: (payload: Record<string, unknown>) => http.post<UserAccount>('/users', payload).then((response) => response.data),
  update: (id: string, payload: Record<string, unknown>) => http.put<UserAccount>(`/users/${id}`, payload).then((response) => response.data),
  delete: (id: string) => http.delete<void>(`/users/${id}`).then((response) => response.data),
}
