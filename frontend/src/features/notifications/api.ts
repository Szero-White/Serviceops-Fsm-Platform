import { http } from '../../api/http'
import type { NotificationItem, PageResponse } from '../../types'

export const notificationsApi = {
  list: () => http.get<PageResponse<NotificationItem>>('/notifications', { params: { page: 0, size: 30 } }).then((response) => response.data),
  unreadCount: () => http.get<{ count: number }>('/notifications/unread-count').then((response) => response.data.count),
  markRead: (id: string) => http.patch<NotificationItem>(`/notifications/${id}/read`).then((response) => response.data),
}
