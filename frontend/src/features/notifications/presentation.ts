import type { NotificationItem } from '../../types'

type NotificationDisplayItem = Pick<NotificationItem, 'title' | 'message'>

const legacyStatusLabels: Record<string, string> = {
  ON_THE_WAY: 'Kỹ thuật viên đang di chuyển',
  'Đang di chuyển': 'Kỹ thuật viên đang di chuyển',
  IN_PROGRESS: 'Công việc đang thực hiện',
  'Đang thực hiện': 'Công việc đang thực hiện',
  WAITING_FOR_PARTS: 'Phiếu đang chờ phụ tùng',
  'Chờ phụ tùng': 'Phiếu đang chờ phụ tùng',
  COMPLETED: 'Phiếu đã hoàn thành',
  'Đã hoàn thành': 'Phiếu đã hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách đã xác nhận',
  'Khách hàng đã nghiệm thu': 'Khách đã xác nhận',
  CLOSED: 'Phiếu đã đóng',
  'Đã đóng': 'Phiếu đã đóng',
  CANCELLED: 'Phiếu đã hủy',
  'Đã hủy': 'Phiếu đã hủy',
  REOPENED: 'Phiếu cần xử lý lại',
  'Đã mở lại': 'Phiếu cần xử lý lại',
}

const legacyStatusMessages: Record<string, string> = {
  ON_THE_WAY: 'Kỹ thuật viên đã bắt đầu di chuyển. Theo dõi tiến độ trong Phiếu công việc.',
  'Đang di chuyển': 'Kỹ thuật viên đã bắt đầu di chuyển. Theo dõi tiến độ trong Phiếu công việc.',
  IN_PROGRESS: 'Kỹ thuật viên đã bắt đầu xử lý. Theo dõi tiến độ trong Phiếu công việc.',
  'Đang thực hiện': 'Kỹ thuật viên đã bắt đầu xử lý. Theo dõi tiến độ trong Phiếu công việc.',
  WAITING_FOR_PARTS: 'Công việc đang chờ phụ tùng. Mở phiếu để kiểm tra tình trạng và phối hợp xử lý.',
  'Chờ phụ tùng': 'Công việc đang chờ phụ tùng. Mở phiếu để kiểm tra tình trạng và phối hợp xử lý.',
  COMPLETED: 'Kỹ thuật viên đã hoàn thành xử lý. Theo dõi phản hồi khách hàng trên phiếu.',
  'Đã hoàn thành': 'Kỹ thuật viên đã hoàn thành xử lý. Theo dõi phản hồi khách hàng trên phiếu.',
  CUSTOMER_ACCEPTED: 'Khách hàng đã xác nhận kết quả. Xem Phiếu công việc nếu cần kiểm tra chi tiết.',
  'Khách hàng đã nghiệm thu': 'Khách hàng đã xác nhận kết quả. Xem Phiếu công việc nếu cần kiểm tra chi tiết.',
  CLOSED: 'Phiếu đã kết thúc. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  'Đã đóng': 'Phiếu đã kết thúc. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  CANCELLED: 'Phiếu đã bị hủy. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  'Đã hủy': 'Phiếu đã bị hủy. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  REOPENED: 'Phiếu đã được mở lại. Mở phiếu để xem lý do và bước xử lý tiếp theo.',
  'Đã mở lại': 'Phiếu đã được mở lại. Mở phiếu để xem lý do và bước xử lý tiếp theo.',
}

/**
 * Presentation-only compatibility for actionable notifications created before the centralized
 * NotificationCopy contract. Obsolete CRUD/audit-style bell rows are removed by Flyway V7, so
 * this module stays focused on preserving readable business hand-offs rather than carrying an
 * ever-growing list of historical CRUD strings inside AppLayout.
 */
export function notificationDisplayText<T extends NotificationDisplayItem>(item: T): T {
  const statusUpdate = /^Cập nhật (WO-[^:]+): .+ (?:→|->) (.+)$/.exec(item.title)
  if (statusUpdate) {
    const [, code, rawTarget] = statusUpdate
    const target = rawTarget.trim()
    const title = legacyStatusLabels[target]
    if (title) {
      return {
        ...item,
        title: `${title}: ${code}`,
        message: legacyStatusMessages[target] ?? item.message,
      }
    }
  }

  const mappings: Array<{
    pattern: RegExp
    title: (code: string) => string
    message: string
  }> = [
    {
      pattern: /^(?:Có phiếu mới chờ điều phối|Phiếu mới cần điều phối|Phiếu mới chờ điều phối): (WO-.+)$/,
      title: (code) => `Cần phân công kỹ thuật viên: ${code}`,
      message: 'Mở Lịch điều phối để xem nội dung phiếu, khách hàng và phân công kỹ thuật viên.',
    },
    {
      pattern: /^(?:Công việc mới|Bạn được giao công việc mới|Bạn được phân công tiếp nhận|Bạn được phân công): (WO-.+)$/,
      title: (code) => `Bạn có công việc mới: ${code}`,
      message: 'Mở Lịch của tôi để xem khách hàng, nội dung công việc và thời gian thực hiện.',
    },
    {
      pattern: /^(?:Lịch công việc được cập nhật|Lịch thực hiện đã được cập nhật|Lịch làm việc đã thay đổi): (WO-.+)$/,
      title: (code) => `Lịch của bạn đã thay đổi: ${code}`,
      message: 'Thời gian thực hiện đã được điều chỉnh. Mở Lịch của tôi để xem lịch mới.',
    },
    {
      pattern: /^(?:Công việc đã được điều phối lại|Công việc đã được điều chuyển|Bạn không còn được phân công): (WO-.+)$/,
      title: (code) => `Bạn không còn phụ trách: ${code}`,
      message: 'Phiếu đã được chuyển sang kỹ thuật viên khác. Kiểm tra Lịch của tôi để cập nhật kế hoạch.',
    },
    {
      pattern: /^Cần xử lý phụ tùng: (WO-.+)$/,
      title: (code) => `Phiếu đang chờ phụ tùng: ${code}`,
      message: 'Kỹ thuật viên đang chờ phụ tùng. Mở phiếu để xem tình trạng và phối hợp xử lý.',
    },
    {
      pattern: /^Cần điều phối xử lý lại: (WO-.+)$/,
      title: (code) => `Phiếu cần xử lý lại: ${code}`,
      message: 'Phiếu đã được mở lại. Mở phiếu để xem lý do và sắp xếp bước xử lý tiếp theo.',
    },
    {
      pattern: /^Phiếu được mở lại: (WO-.+)$/,
      title: (code) => `Công việc cần xử lý lại: ${code}`,
      message: 'Phiếu đã được mở lại. Mở phiếu để xem lý do và tiếp tục theo phân công.',
    },
  ]

  for (const mapping of mappings) {
    const match = mapping.pattern.exec(item.title)
    if (match) {
      return {
        ...item,
        title: mapping.title(match[1]),
        message: mapping.message,
      }
    }
  }

  const inventoryTitleMappings: Array<[RegExp, (value: string) => string]> = [
    [/^Phụ tùng sắp hết: (.+)$/, (value) => `Tồn kho thấp: ${value}`],
    [/^Cần bổ sung tồn kho: (.+)$/, (value) => `Tồn kho thấp: ${value}`],
    [/^Tồn thấp sau kiểm kê: (.+)$/, (value) => `Tồn kho thấp sau kiểm kê: ${value}`],
    [/^Cần bổ sung tồn kho sau kiểm kê: (.+)$/, (value) => `Tồn kho thấp sau kiểm kê: ${value}`],
    [/^Tồn thấp theo ngưỡng mới: (.+)$/, (value) => `Tồn kho thấp theo ngưỡng mới: ${value}`],
    [/^Cần kiểm tra tồn kho: (.+)$/, (value) => `Tồn kho thấp: ${value}`],
    [/^Chênh lệch kiểm kê: (.+)$/, (value) => `Kiểm kê có chênh lệch: ${value}`],
  ]

  for (const [pattern, title] of inventoryTitleMappings) {
    const match = pattern.exec(item.title)
    if (match) {
      return { ...item, title: title(match[1]) }
    }
  }

  return item
}
