import axios, { AxiosError } from 'axios'

const API_URL = import.meta.env.VITE_API_URL || '/api/v1'

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
  if (!axios.isAxiosError(error)) {
    return 'Đã xảy ra lỗi. Vui lòng thử lại.'
  }

  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return 'Hệ thống phản hồi quá lâu. Vui lòng thử lại.'
  }

  const status = error.response?.status
  const data = error.response?.data as {
    code?: string
    detail?: string
    errors?: Record<string, string>
    requestId?: string
  } | undefined

  if (status && status >= 500) {
    return withSupportId('Hệ thống tạm thời chưa thể xử lý yêu cầu. Vui lòng thử lại sau.', data?.requestId)
  }

  if (data?.code === 'VALIDATION_ERROR' && data.errors) {
    return Object.values(data.errors)[0] ?? 'Dữ liệu không hợp lệ'
  }

  // Only application ProblemDetail responses with a known code may provide
  // user-facing detail. Never surface arbitrary proxy/runtime error strings.
  if (data?.code && data.detail) {
    return data.detail
  }

  if (!error.response) {
    return 'Không thể kết nối đến hệ thống. Vui lòng kiểm tra kết nối và thử lại.'
  }
  if (status === 403) {
    return 'Bạn không có quyền thực hiện thao tác này.'
  }
  if (status === 404) {
    return 'Không tìm thấy dữ liệu yêu cầu.'
  }

  return 'Không thể xử lý yêu cầu. Vui lòng thử lại.'
}

function withSupportId(message: string, requestId?: string): string {
  return requestId ? `${message} Mã hỗ trợ: ${requestId}` : message
}

export function apiErrorCode(error: unknown): string | undefined {
  if (!axios.isAxiosError(error)) return undefined
  const data = error.response?.data as { code?: string } | undefined
  return data?.code
}

export { API_URL }
