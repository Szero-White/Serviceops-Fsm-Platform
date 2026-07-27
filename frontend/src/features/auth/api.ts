import { http } from '../../api/http'
import type { AuthResponse } from '../../types'

export const authApi = {
  login: (username: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { username, password }).then((response) => response.data),
}
