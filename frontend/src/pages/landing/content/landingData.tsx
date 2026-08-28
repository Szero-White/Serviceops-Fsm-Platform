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
  { value: '5', label: 'Vai trò nghiệp vụ có phân quyền', icon: <TeamOutlined /> },
  { value: 'CI', label: 'Backend, frontend và E2E quality gates', icon: <SafetyCertificateOutlined /> },
  { value: '3', label: 'Dịch vụ trong production Docker stack', icon: <RocketOutlined /> },
  { value: '1', label: 'Luồng dịch vụ end-to-end được truy vết', icon: <AuditOutlined /> },
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
    desc: 'Phân công kỹ thuật viên, đặt khung giờ và chặn lịch chồng lấn để điều phối công việc rõ ràng.',
  },
  {
    icon: <AppstoreOutlined />,
    colorKey: 'neutral',
    title: 'Hồ sơ thiết bị toàn diện',
    desc: 'Theo dõi serial, loại thiết bị, bảo hành, ngày lắp đặt và liên kết thiết bị với lịch sử yêu cầu dịch vụ.',
  },
  {
    icon: <DatabaseOutlined />,
    colorKey: 'neutral',
    title: 'Kho phụ tùng có truy vết',
    desc: 'Quản lý tồn hiện tại, ngưỡng tồn tối thiểu, kiểm kê và ledger nhập/sử dụng/hoàn trả/điều chỉnh gắn với nghiệp vụ thực tế.',
  },
  {
    icon: <BarChartOutlined />,
    colorKey: 'neutral',
    title: 'Tổng quan vận hành',
    desc: 'Dashboard tổng hợp yêu cầu mở, tiến độ phiếu, rủi ro phụ tùng, tỷ lệ hoàn tất và dữ liệu vận hành gần đây.',
  },
  {
    icon: <SafetyCertificateOutlined />,
    colorKey: 'neutral',
    title: 'Phân quyền và bảo mật',
    desc: 'JWT authentication, role-based access, multi-tenant isolation và audit log cho thao tác quan trọng.',
  },
]

export const FEATURE_COLORS: Record<FeatureColor, { bg: string; fg: string }> = {
  primary: { bg: '#eef4f8', fg: '#3f6f93' },
  accent: { bg: '#eef6f4', fg: '#4f7f7b' },
  neutral: { bg: '#f3f6f8', fg: '#596b78' },
}

export const HOW_IT_WORKS: Step[] = [
  { step: '01', title: 'Tiếp nhận yêu cầu', desc: 'Khách hàng gửi yêu cầu, hệ thống tạo ticket và phân loại ưu tiên.' },
  { step: '02', title: 'Điều phối kỹ thuật viên', desc: 'Phiếu công việc được bàn giao từ Service Request; điều phối viên chọn kỹ thuật viên và xếp lịch phù hợp.' },
  { step: '03', title: 'Thực hiện và cập nhật', desc: 'Kỹ thuật viên cập nhật tiến độ, phụ tùng và bằng chứng tại hiện trường.' },
  { step: '04', title: 'Hoàn thành và truy vết', desc: 'Hoàn tất công việc, giữ lịch sử trạng thái, inventory ledger, biên nhận thanh toán và audit trail để đối soát.' },
]

export const OPERATIONAL_SCENARIOS: OperationalScenario[] = [
  {
    title: 'Điều phối công việc trong ngày',
    audience: 'Dispatcher',
    icon: <CalendarOutlined />,
    text: 'Theo dõi yêu cầu chưa xử lý, phân công kỹ thuật viên, kiểm tra xung đột lịch và chuyển trạng thái phiếu trên cùng một luồng.',
  },
  {
    title: 'Thực hiện dịch vụ tại hiện trường',
    audience: 'Technician',
    icon: <ThunderboltOutlined />,
    text: 'Xem phiếu được giao, cập nhật tiến độ, ghi nhận phụ tùng đã dùng và hoàn tất công việc với lịch sử trạng thái được lưu lại.',
  },
  {
    title: 'Kiểm soát tồn kho và truy vết',
    audience: 'Warehouse / Owner',
    icon: <DatabaseOutlined />,
    text: 'Theo dõi tồn hiện tại, kiểm kê chênh lệch, hoàn trả theo Work Order và ledger biến động kho có actor, thời gian và tồn sau giao dịch.',
  },
]

export const DEPLOYMENT_OPTIONS: DeploymentOption[] = [
  {
    name: 'Evaluation',
    badge: null,
    highlight: false,
    desc: 'Dành cho reviewer hoặc đội kỹ thuật muốn chạy nhanh toàn bộ luồng nghiệp vụ.',
    features: ['Java 21 + Spring Boot', 'React + TypeScript', 'PostgreSQL local hoặc Testcontainers', 'Demo accounts theo 5 vai trò'],
    cta: 'Mở bản demo',
  },
  {
    name: 'Production-like',
    badge: 'Sẵn sàng kiểm thử',
    highlight: true,
    desc: 'Mô hình single-node rõ ràng để kiểm thử deployment, backup và health checks trước khi public demo.',
    features: ['Nginx phục vụ frontend', 'Spring Boot backend container', 'PostgreSQL 17 private network', 'Health checks + backup/restore + CI'],
    cta: 'Xem luồng vận hành',
  },
  {
    name: 'Product rollout',
    badge: 'Mở rộng theo nhu cầu',
    highlight: false,
    desc: 'Các hạng mục productization được ưu tiên theo nhu cầu vận hành thật, không thêm công nghệ chỉ để làm đẹp CV.',
    features: ['SLA / promised service windows', 'Preventive maintenance agreements', 'Technician mobile/PWA khi có use case', 'Object storage / SSO khi có yêu cầu triển khai'],
    cta: 'Xem định hướng',
  },
]

export const INTEGRATIONS: Integration[] = [
  { icon: <GlobalOutlined />, name: 'REST API', desc: 'HTTP/JSON cho frontend và tích hợp nội bộ' },
  { icon: <DatabaseOutlined />, name: 'PostgreSQL', desc: 'Flyway migrations và Testcontainers' },
  { icon: <RocketOutlined />, name: 'Docker + Nginx', desc: 'Production-like Compose và health checks' },
  { icon: <BarChartOutlined />, name: 'Observability', desc: 'Actuator health, metrics và Prometheus endpoint' },
]

export const CAPABILITY_LABELS = [
  'Tiếp nhận yêu cầu',
  'Thiết bị khách hàng',
  'Phiếu công việc',
  'Điều phối lịch tuần',
  'Kho phụ tùng',
  'Audit & báo cáo',
]

export const MOCK_METRICS: MockMetric[] = [
  { label: 'Phiếu', value: '24', bg: '#eef4f8', icon: 'WO' },
  { label: 'Đang xử lý', value: '8', bg: '#f3f6f8', icon: 'IP' },
  { label: 'Hoàn thành', value: '14', bg: '#eef6f4', icon: 'OK' },
  { label: 'Phụ tùng', value: '312', bg: '#faf6ee', icon: 'SP' },
]

export const MOCK_ROWS: MockRow[] = [
  { id: 'WO-0041', client: 'Minh Quang Electric', status: 'Đang xử lý', statusBg: '#faf6ee' },
  { id: 'WO-0040', client: 'TechCool HVAC', status: 'Hoàn thành', statusBg: '#eef6f4' },
  { id: 'WO-0039', client: 'Lan Anh Elevator', status: 'Chờ phân công', statusBg: '#f1f5f9' },
]

export const MOCK_NAV_ITEMS = ['Tổng quan', 'Work Orders', 'Kỹ thuật viên', 'Kho phụ tùng', 'Báo cáo']

export const FOOTER_LINKS: Record<string, FooterLink[]> = {
  'Sản phẩm': [
    { label: 'Tính năng', href: '#features' },
    { label: 'Quy trình', href: '#how' },
    { label: 'Triển khai', href: '#deployment' },
  ],
  'Kỹ thuật': [
    { label: 'Năng lực nền tảng', href: '#stats' },
    { label: 'Tích hợp', href: '#integrations' },
    { label: 'Đăng nhập demo', href: '/login' },
  ],
  'Repository': [
    { label: 'GitHub', href: 'https://github.com/Szero-White/Serviceops-Fsm-Platform' },
    { label: 'Kiến trúc & tài liệu', href: 'https://github.com/Szero-White/Serviceops-Fsm-Platform/tree/main/docs' },
  ],
}
