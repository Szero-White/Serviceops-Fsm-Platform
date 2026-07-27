import { http } from '../../api/http'
import type { Dashboard } from '../../types'

export const dashboardApi = {
  get: () => http.get<Dashboard>('/dashboard').then((response) => response.data),
}
