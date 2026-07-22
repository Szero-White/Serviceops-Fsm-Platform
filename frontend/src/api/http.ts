import axios, { AxiosError } from 'axios'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export const http = axios.create({
  baseURL: API_URL,
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('serviceops.accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('serviceops.accessToken')
      localStorage.removeItem('serviceops.user')
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { detail?: string; message?: string; title?: string; errors?: Record<string, string> } | undefined
    if (data?.errors) return Object.values(data.errors)[0] ?? 'Dữ liệu không hợp lệ'
    return data?.detail ?? data?.message ?? data?.title ?? 'Không thể kết nối đến máy chủ'
  }
  return error instanceof Error ? error.message : 'Đã xảy ra lỗi ngoài dự kiến'
}

export { API_URL }
