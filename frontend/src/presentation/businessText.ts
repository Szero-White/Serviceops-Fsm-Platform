export const BUSINESS_CODE_LABELS: Record<string, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  SCHEDULED: 'Đã lên lịch',
  ASSIGNED: 'Đã phân công',
  ON_THE_WAY: 'Đang di chuyển',
  IN_PROGRESS: 'Đang thực hiện',
  WAITING_FOR_PARTS: 'Chờ phụ tùng',
  COMPLETED: 'Đã hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách đã xác nhận',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã hủy',
  REOPENED: 'Mở lại để xử lý',
  CONVERTED: 'Đã tạo phiếu công việc',
  ACTIVE: 'Hoạt động',
  IN_SERVICE: 'Đang sửa chữa',
  OUT_OF_SERVICE: 'Tạm ngưng sử dụng',
  RETIRED: 'Ngừng sử dụng',

  OWNER: 'Chủ sở hữu',
  CUSTOMER_SERVICE: 'Chăm sóc khách hàng',
  DISPATCHER: 'Điều phối viên',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
  SYSTEM: 'Hệ thống',

  UNPAID: 'Chưa thanh toán',
  TRANSFER_PENDING_VERIFICATION: 'Chờ xác minh chuyển khoản',
  CASH_PENDING_HANDOVER: 'Chờ bàn giao tiền mặt',
  COUNTER_PAYMENT_PENDING: 'Chờ thanh toán tại quầy',
  SETTLED: 'Đã đối soát',
  BANK_TRANSFER: 'Chuyển khoản',
  CASH: 'Tiền mặt',

  REQUESTED: 'Chờ cấp phụ tùng',
  REQUEST: 'Yêu cầu phụ tùng',
  ISSUED: 'Đã cấp phụ tùng',
  USED: 'Đã sử dụng',
  UNAVAILABLE: 'Không thể cấp',
  EXPIRED: 'Đã hết hiệu lực',

  IMPORT: 'Nhập kho',
  ISSUE: 'Cấp phụ tùng',
  CONSUME: 'Sử dụng phụ tùng',
  RETURN: 'Hoàn trả phụ tùng',
  ADJUSTMENT_IN: 'Điều chỉnh tăng',
  ADJUSTMENT_OUT: 'Điều chỉnh giảm',

  PHONE: 'Điện thoại',
  EMAIL: 'Email',
  WEBSITE: 'Trang web',
  ZALO: 'Zalo',
  WALK_IN: 'Khách đến trực tiếp',
  INTERNAL: 'Nội bộ',
}

export const AUDIT_ACTION_LABELS: Record<string, string> = {
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
  ASSIGN: 'Phân công',
  RESCHEDULE: 'Điều chỉnh lịch',
  CHANGE_STATUS: 'Cập nhật tiến độ',
  CANCEL: 'Hủy',
  CREATE_FROM_SERVICE_REQUEST: 'Tạo phiếu từ yêu cầu dịch vụ',
  CUSTOMER_ACCEPTANCE: 'Ghi nhận khách xác nhận',
  CLOSE_WORK_ORDER: 'Đóng phiếu công việc',
  DELETE_HISTORY: 'Ẩn khỏi lịch sử tra cứu',

  REQUEST_PART: 'Yêu cầu phụ tùng',
  UPDATE_PART_REQUEST: 'Cập nhật yêu cầu phụ tùng',
  CANCEL_PART_REQUEST: 'Hủy yêu cầu phụ tùng',
  PART_REQUEST_UNAVAILABLE: 'Ghi nhận không thể cấp phụ tùng',
  ISSUE_PART: 'Cấp phụ tùng',
  CONFIRM_PART_USAGE: 'Xác nhận phụ tùng đã sử dụng',
  CONSUME_PART: 'Ghi nhận phụ tùng đã sử dụng',
  RETURN_PART: 'Nhận lại phụ tùng',
  IMPORT_STOCK: 'Nhập thêm tồn kho',
  STOCKTAKE: 'Kiểm kê tồn kho',
  UPDATE_REORDER_LEVEL: 'Cập nhật ngưỡng cảnh báo tồn',
  REACTIVATE: 'Cho phép sử dụng lại',
  DISCONTINUE: 'Ngừng sử dụng',
  IMPORT_CUSTOMERS: 'Nhập danh sách khách hàng',
  IMPORT_ASSETS: 'Nhập danh sách thiết bị',
  IMPORT_SPARE_PARTS: 'Nhập danh sách phụ tùng',

  UPDATE_BILLING_DRAFT: 'Cập nhật chi phí dự kiến',
  UPDATE_PAYMENT_PROFILE: 'Cập nhật thông tin nhận thanh toán',
  REPORT_BANK_TRANSFER: 'Ghi nhận khách đã chuyển khoản',
  COLLECT_CASH: 'Ghi nhận đã nhận tiền mặt',
  REQUEST_COUNTER_PAYMENT: 'Ghi nhận khách sẽ thanh toán tại quầy',
  VERIFY_BANK_TRANSFER: 'Xác nhận tiền chuyển khoản',
  CONFIRM_CASH_HANDOVER: 'Xác nhận nhận bàn giao tiền mặt',
  SETTLE_COUNTER_TRANSFER: 'Xác nhận chuyển khoản tại quầy',
  SETTLE_COUNTER_CASH: 'Xác nhận thu tiền mặt tại quầy',
  ISSUE_PAYMENT_RECEIPT: 'Phát hành biên nhận',

  UPLOAD_FILE: 'Thêm tệp đính kèm',
  RENAME_FILE: 'Đổi tên tệp đính kèm',
  DELETE_FILE: 'Xóa tệp đính kèm',

  AI_HELP_BLOCKED: 'Từ chối câu hỏi ngoài phạm vi',
  AI_HELP_LOCAL: 'Hướng dẫn bằng dữ liệu nội bộ',
  AI_HELP_GEMINI: 'Hướng dẫn bằng Gemini',
  AI_HELP_FALLBACK: 'Hướng dẫn bằng dữ liệu nội bộ',
  AI_DRAFT_LOCAL: 'Gợi ý nội dung bằng dữ liệu nội bộ',
  AI_DRAFT_GEMINI: 'Gợi ý nội dung bằng Gemini',
  AI_DRAFT_FALLBACK: 'Gợi ý nội dung bằng dữ liệu nội bộ',
  SEED: 'Khởi tạo dữ liệu mẫu',
}

export const AUDIT_ENTITY_LABELS: Record<string, string> = {
  SERVICE_CHANNEL: 'Kênh tiếp nhận',
  SERVICE_REQUEST: 'Yêu cầu dịch vụ',
  WORK_ORDER: 'Phiếu công việc',
  CUSTOMER: 'Khách hàng',
  ASSET: 'Thiết bị',
  TECHNICIAN_PROFILE: 'Hồ sơ kỹ thuật viên',
  SPARE_PART: 'Phụ tùng',
  INVENTORY: 'Kho phụ tùng',
  USER_ACCOUNT: 'Tài khoản người dùng',
  ATTACHMENT: 'Tệp đính kèm',
  PAYMENT: 'Thanh toán',
  TENANT: 'Thông tin doanh nghiệp',
  AI: 'Trợ lý thông minh',
  SYSTEM: 'Hệ thống',
}

export function businessCodeLabel(value?: string, fallback = 'Không xác định') {
  if (!value) return fallback
  return BUSINESS_CODE_LABELS[value] ?? fallback
}

export function auditActionLabel(action?: string) {
  if (!action) return 'Không xác định'
  return AUDIT_ACTION_LABELS[action] ?? 'Thao tác hệ thống'
}

export function auditEntityLabel(entityType?: string) {
  if (!entityType) return 'Không xác định'
  return AUDIT_ENTITY_LABELS[entityType] ?? 'Đối tượng nghiệp vụ'
}

const USER_FACING_PHRASE_REPLACEMENTS: Array<[RegExp, string]> = [
  [/\bCSKH\b/g, 'bộ phận chăm sóc khách hàng'],
  [/\bKTV\b/g, 'kỹ thuật viên'],
  [/\bWork Order\b/gi, 'phiếu công việc'],
  [/\bService Request\b/gi, 'yêu cầu dịch vụ'],
  [/\bSKU\b/g, 'mã phụ tùng'],
  [/\bCSV\b/g, 'tệp dữ liệu'],
  [/\bImport\b/g, 'Nhập'],
  [/\brole\b/gi, 'vai trò'],
  [/\blegacy\b/gi, 'dữ liệu cũ'],
  [/\busername\b/gi, 'tên đăng nhập'],
  [/\bpublic demo\b/gi, 'bản dùng thử công khai'],
  [/\baudit trail\b/gi, 'nhật ký thay đổi'],
]

const CODE_PATTERN = new RegExp(
  `\\b(${Object.keys(BUSINESS_CODE_LABELS).sort((a, b) => b.length - a.length).join('|')})\\b`,
  'g',
)

export function businessFriendlyText(value?: string) {
  if (!value?.trim()) return '—'

  let result = value
    .replaceAll('->', '→')
    .replace(/\brole=([^|]+)/gi, (_match, role) => `Vai trò: ${businessCodeLabel(String(role).trim(), 'Không xác định')}`)
    .replace(/\btopic=([^|]+)/gi, (_match, topic) => `Chủ đề: ${String(topic).trim()}`)
    .replace(/\bprovider=gemini\b/gi, 'Nguồn: Gemini')
    .replace(/\bprovider=local\b/gi, 'Nguồn: dữ liệu nội bộ')
    .replace(/\bprovider=policy\b/gi, 'Nguồn: quy tắc hệ thống')
    .replace(/\bfeature=service-request-draft\b/gi, 'Tính năng: gợi ý nội dung yêu cầu dịch vụ')
    .replace(CODE_PATTERN, (code) => BUSINESS_CODE_LABELS[code] ?? code)
    .replace(/\s*\|\s*/g, ' · ')

  for (const [pattern, replacement] of USER_FACING_PHRASE_REPLACEMENTS) {
    result = result.replace(pattern, replacement)
  }
  return result
}
