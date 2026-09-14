import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CustomerServiceOutlined,
  DatabaseOutlined,
  GlobalOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

export type FeatureColor = 'primary' | 'accent' | 'neutral'

export type NavLink = { label: string; href: string }
export type Stat = { value: string; label: string; icon: ReactNode }
export type Feature = { icon: ReactNode; colorKey: FeatureColor; title: string; desc: string }
export type Step = { step: string; title: string; desc: string }
export type OperationalScenario = {
  title: string
  audience: string
  icon: ReactNode
  text: string
}
export type DeploymentOption = {
  name: string
  badge: string | null
  desc: string
  features: string[]
  cta: string
  highlight: boolean
}
export type Integration = { icon: ReactNode; name: string; desc: string }
export type MockMetric = { label: string; value: string; bg: string; icon: string }
export type MockRow = { id: string; client: string; status: string; statusBg: string }
export type FooterLink = { label: string; href: string }

export const NAV_LINKS: NavLink[] = [
  { label: 'Tính năng', href: '#features' },
  { label: 'Quy trình', href: '#how' },
  { label: 'Kịch bản', href: '#scenarios' },
  { label: 'Triển khai', href: '#deployment' },
]

export const STATS: Stat[] = [
  { value: '5', label: 'Vai trò nghiệp vụ được phân quyền rõ ràng', icon: <TeamOutlined /> },
  { value: '1', label: 'Luồng vận hành xuyên suốt từ tiếp nhận đến hoàn tất', icon: <SafetyCertificateOutlined /> },
  { value: '24/7', label: 'Bản dùng thử trực tuyến sẵn sàng trải nghiệm', icon: <RocketOutlined /> },
  { value: '1', label: 'Nhật ký thay đổi tập trung để tra cứu', icon: <AuditOutlined /> },
]

export const FEATURES: Feature[] = [
  {
    icon: <CustomerServiceOutlined />,
    colorKey: 'primary',
    title: 'Quản lý yêu cầu dịch vụ',
    desc: 'Tiếp nhận, phân loại và điều phối mọi yêu cầu khách hàng trên một giao diện thống nhất.',
  },
  {
    icon: <CalendarOutlined />,
    colorKey: 'accent',
    title: 'Điều phối và lịch công việc',
    desc: 'Phân công kỹ thuật viên, đặt khung giờ và ngăn lịch chồng lấn để điều phối công việc rõ ràng.',
  },
  {
    icon: <AppstoreOutlined />,
    colorKey: 'neutral',
    title: 'Hồ sơ thiết bị đầy đủ',
    desc: 'Theo dõi số sê-ri, loại thiết bị, bảo hành, ngày lắp đặt và lịch sử yêu cầu dịch vụ liên quan.',
  },
  {
    icon: <DatabaseOutlined />,
    colorKey: 'neutral',
    title: 'Kho phụ tùng có lịch sử rõ ràng',
    desc: 'Quản lý tồn hiện tại, ngưỡng cảnh báo, kiểm kê và lịch sử nhập, cấp, sử dụng, hoàn trả hoặc điều chỉnh.',
  },
  {
    icon: <BarChartOutlined />,
    colorKey: 'neutral',
    title: 'Tổng quan vận hành',
    desc: 'Màn tổng quan giúp theo dõi yêu cầu đang mở, tiến độ công việc, rủi ro thiếu phụ tùng và kết quả gần đây.',
  },
  {
    icon: <SafetyCertificateOutlined />,
    colorKey: 'neutral',
    title: 'Phân quyền và bảo mật',
    desc: 'Mỗi vai trò chỉ nhìn thấy và thao tác đúng phần việc được giao; dữ liệu giữa các doanh nghiệp được tách biệt.',
  },
]

export const FEATURE_COLORS: Record<FeatureColor, { bg: string; fg: string }> = {
  primary: { bg: '#eef4f8', fg: '#3f6f93' },
  accent: { bg: '#eef6f4', fg: '#4f7f7b' },
  neutral: { bg: '#f3f6f8', fg: '#596b78' },
}

export const HOW_IT_WORKS: Step[] = [
  { step: '01', title: 'Tiếp nhận yêu cầu', desc: 'Khách hàng báo nhu cầu, hệ thống ghi nhận yêu cầu và mức độ ưu tiên.' },
  { step: '02', title: 'Điều phối kỹ thuật viên', desc: 'Yêu cầu đủ thông tin được chuyển thành phiếu công việc; điều phối viên chọn kỹ thuật viên và xếp lịch phù hợp.' },
  { step: '03', title: 'Thực hiện và cập nhật', desc: 'Kỹ thuật viên cập nhật tiến độ, phụ tùng sử dụng và bằng chứng tại hiện trường.' },
  { step: '04', title: 'Hoàn thành và tra cứu', desc: 'Kết quả xử lý, thanh toán, biên nhận, lịch sử kho và các thay đổi quan trọng đều được lưu để đối chiếu.' },
]

export const OPERATIONAL_SCENARIOS: OperationalScenario[] = [
  {
    title: 'Điều phối công việc trong ngày',
    audience: 'Điều phối viên',
    icon: <CalendarOutlined />,
    text: 'Theo dõi yêu cầu chưa xử lý, phân công kỹ thuật viên, kiểm tra trùng lịch và cập nhật tiến độ trên cùng một luồng.',
  },
  {
    title: 'Thực hiện dịch vụ tại hiện trường',
    audience: 'Kỹ thuật viên',
    icon: <ThunderboltOutlined />,
    text: 'Xem phiếu được giao, cập nhật tiến độ, ghi nhận phụ tùng đã dùng và hoàn tất công việc với lịch sử được lưu lại.',
  },
  {
    title: 'Kiểm soát tồn kho và truy vết',
    audience: 'Nhân viên kho / Chủ sở hữu',
    icon: <DatabaseOutlined />,
    text: 'Theo dõi tồn hiện tại, kiểm kê chênh lệch, nhận phụ tùng hoàn trả theo phiếu công việc và xem lịch sử biến động cùng người thực hiện.',
  },
]

export const DEPLOYMENT_OPTIONS: DeploymentOption[] = [
  {
    name: 'Bản dùng thử',
    badge: null,
    highlight: false,
    desc: 'Dành cho người đánh giá muốn trải nghiệm nhanh các luồng nghiệp vụ chính theo từng vai trò.',
    features: ['5 vai trò nghiệp vụ mẫu', 'Dữ liệu minh họa có sẵn', 'Luồng yêu cầu → công việc → thanh toán', 'Không cần cài đặt trên máy người dùng'],
    cta: 'Mở bản dùng thử',
  },
  {
    name: 'Bản trực tuyến',
    badge: 'Đang hoạt động',
    highlight: true,
    desc: 'Bản chạy trên Internet để kiểm tra trải nghiệm thật và khả năng hoạt động ổn định khi máy cá nhân tắt.',
    features: ['Truy cập qua trình duyệt bảo mật', 'Dữ liệu nghiệp vụ được tách biệt', 'Có kiểm tra tình trạng hoạt động', 'Có phương án sao lưu và khôi phục'],
    cta: 'Xem luồng vận hành',
  },
  {
    name: 'Mở rộng vận hành',
    badge: 'Theo nhu cầu thực tế',
    highlight: false,
    desc: 'Các chức năng mở rộng chỉ được bổ sung khi có nhu cầu vận hành thật, tránh làm hệ thống phức tạp không cần thiết.',
    features: ['Cam kết thời gian phục vụ', 'Lịch bảo trì định kỳ', 'Trải nghiệm tối ưu trên thiết bị di động', 'Lưu trữ tệp theo quy mô sử dụng'],
    cta: 'Xem định hướng',
  },
]

export const INTEGRATIONS: Integration[] = [
  { icon: <GlobalOutlined />, name: 'Kết nối dữ liệu', desc: 'Cho phép các phần của hệ thống trao đổi dữ liệu nhất quán' },
  { icon: <DatabaseOutlined />, name: 'Dữ liệu có kiểm soát', desc: 'Giữ dữ liệu nhất quán khi hệ thống được cập nhật' },
  { icon: <RocketOutlined />, name: 'Vận hành ổn định', desc: 'Giao diện và quy trình nghiệp vụ được duy trì trực tuyến ổn định' },
  { icon: <BarChartOutlined />, name: 'Theo dõi tình trạng', desc: 'Kiểm tra nhanh hệ thống có đang hoạt động bình thường hay không' },
]

export const CAPABILITY_LABELS = [
  'Tiếp nhận yêu cầu',
  'Thiết bị khách hàng',
  'Phiếu công việc',
  'Điều phối lịch tuần',
  'Kho phụ tùng',
  'Nhật ký & báo cáo',
]

export const MOCK_METRICS: MockMetric[] = [
  { label: 'Phiếu', value: '24', bg: '#eef4f8', icon: '▣' },
  { label: 'Đang xử lý', value: '8', bg: '#f3f6f8', icon: '↻' },
  { label: 'Hoàn thành', value: '14', bg: '#eef6f4', icon: '✓' },
  { label: 'Phụ tùng', value: '312', bg: '#faf6ee', icon: '◇' },
]

export const MOCK_ROWS: MockRow[] = [
  { id: 'WO-20260914-041', client: 'Điện Minh Quang', status: 'Đang xử lý', statusBg: '#faf6ee' },
  { id: 'WO-20260914-040', client: 'Điện lạnh TechCool', status: 'Hoàn thành', statusBg: '#eef6f4' },
  { id: 'WO-20260914-039', client: 'Thang máy Lan Anh', status: 'Chờ phân công', statusBg: '#f1f5f9' },
]

export const MOCK_NAV_ITEMS = ['Tổng quan', 'Phiếu công việc', 'Kỹ thuật viên', 'Kho phụ tùng', 'Báo cáo']

export const FOOTER_LINKS: Record<string, FooterLink[]> = {
  'Sản phẩm': [
    { label: 'Tính năng', href: '#features' },
    { label: 'Quy trình', href: '#how' },
    { label: 'Triển khai', href: '#deployment' },
  ],
  'Thông tin': [
    { label: 'Năng lực nền tảng', href: '#stats' },
    { label: 'Khả năng vận hành', href: '#integrations' },
    { label: 'Đăng nhập bản dùng thử', href: '/login' },
  ],
  'Tài liệu': [
    { label: 'GitHub', href: 'https://github.com/Szero-White/Serviceops-Fsm-Platform' },
    { label: 'Kiến trúc & tài liệu', href: 'https://github.com/Szero-White/Serviceops-Fsm-Platform/tree/main/docs' },
  ],
}
