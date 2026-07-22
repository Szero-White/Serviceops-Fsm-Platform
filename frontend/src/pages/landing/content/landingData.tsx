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
  TrophyOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

export type FeatureColor = 'blue' | 'purple' | 'green' | 'orange' | 'cyan' | 'red'

export type NavLink = { label: string; href: string }
export type Stat = { value: string; label: string; icon: ReactNode }
export type Feature = { icon: ReactNode; colorKey: FeatureColor; title: string; desc: string }
export type Step = { step: string; title: string; desc: string }
export type Testimonial = {
  name: string
  role: string
  company: string
  avatar: string
  avatarColor: string
  rating: number
  text: string
}
export type PricingTier = {
  name: string
  price: string
  unit: string
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
  { label: 'Khách hàng', href: '#testimonials' },
  { label: 'Bảng giá', href: '#pricing' },
]

export const STATS: Stat[] = [
  { value: '500+', label: 'Doanh nghiệp tin dùng', icon: <TrophyOutlined /> },
  { value: '98%', label: 'Tỉ lệ hoàn thành đúng hẹn', icon: <CalendarOutlined /> },
  { value: '3.2x', label: 'Tăng năng suất kỹ thuật viên', icon: <ThunderboltOutlined /> },
  { value: '<2h', label: 'Thời gian onboarding', icon: <RocketOutlined /> },
]

export const FEATURES: Feature[] = [
  {
    icon: <CustomerServiceOutlined />,
    colorKey: 'blue',
    title: 'Quản lý yêu cầu dịch vụ',
    desc: 'Tiếp nhận, phân loại và điều phối mọi yêu cầu khách hàng trên một giao diện thống nhất.',
  },
  {
    icon: <CalendarOutlined />,
    colorKey: 'purple',
    title: 'Lịch và Work Order thông minh',
    desc: 'Lập lịch kỹ thuật viên theo kỹ năng, vị trí và khả năng. Giao việc và theo dõi tiến độ rõ ràng.',
  },
  {
    icon: <AppstoreOutlined />,
    colorKey: 'green',
    title: 'Hồ sơ thiết bị toàn diện',
    desc: 'Lịch sử bảo trì, bảo hành, nhật ký sửa chữa và vị trí lắp đặt trong một nơi để tra cứu nhanh.',
  },
  {
    icon: <DatabaseOutlined />,
    colorKey: 'orange',
    title: 'Kho phụ tùng thông minh',
    desc: 'Quản lý tồn kho, cảnh báo ngưỡng tối thiểu và gắn giao dịch phụ tùng trực tiếp với work order.',
  },
  {
    icon: <BarChartOutlined />,
    colorKey: 'cyan',
    title: 'Báo cáo và phân tích',
    desc: 'Dashboard KPI: tỉ lệ hoàn thành, thời gian phản hồi và hiệu suất từng kỹ thuật viên.',
  },
  {
    icon: <SafetyCertificateOutlined />,
    colorKey: 'red',
    title: 'Phân quyền và bảo mật',
    desc: 'JWT authentication, role-based access, multi-tenant isolation và audit log cho thao tác quan trọng.',
  },
]

export const FEATURE_COLORS: Record<FeatureColor, { bg: string; fg: string }> = {
  blue: { bg: '#eff6ff', fg: '#2563eb' },
  purple: { bg: '#f5f3ff', fg: '#7c3aed' },
  green: { bg: '#ecfdf5', fg: '#059669' },
  orange: { bg: '#fff7ed', fg: '#d97706' },
  cyan: { bg: '#ecfeff', fg: '#0891b2' },
  red: { bg: '#fef2f2', fg: '#dc2626' },
}

export const HOW_IT_WORKS: Step[] = [
  { step: '01', title: 'Tiếp nhận yêu cầu', desc: 'Khách hàng gửi yêu cầu, hệ thống tạo ticket và phân loại ưu tiên.' },
  { step: '02', title: 'Điều phối kỹ thuật viên', desc: 'Dispatcher chỉ định kỹ thuật viên phù hợp và tạo work order.' },
  { step: '03', title: 'Thực hiện và cập nhật', desc: 'Kỹ thuật viên cập nhật tiến độ, phụ tùng và bằng chứng tại hiện trường.' },
  { step: '04', title: 'Hoàn thành và báo cáo', desc: 'Đóng việc, cập nhật kho, ghi audit log và tổng hợp KPI lên dashboard.' },
]

export const TESTIMONIALS: Testimonial[] = [
  {
    name: 'Nguyễn Văn Minh',
    role: 'Giám đốc vận hành',
    company: 'TechCool HVAC',
    avatar: 'M',
    avatarColor: '#2563eb',
    rating: 5,
    text: 'ServiceOps giúp đội ngũ phân công công việc nhanh hơn, dữ liệu thiết bị rõ ràng và giảm sai sót điều phối.',
  },
  {
    name: 'Trần Thị Lan',
    role: 'CEO',
    company: 'Lan Anh Elevator',
    avatar: 'L',
    avatarColor: '#7c3aed',
    rating: 5,
    text: 'Tỉ lệ hoàn thành đúng hẹn tăng rõ rệt sau khi đội ngũ chuyển từ spreadsheet sang quy trình tập trung.',
  },
  {
    name: 'Lê Thanh Phong',
    role: 'Trưởng phòng kỹ thuật',
    company: 'Phong Phú Engineering',
    avatar: 'P',
    avatarColor: '#059669',
    rating: 5,
    text: 'Quản lý 40 kỹ thuật viên và hàng trăm thiết bị dễ hơn nhiều, kho phụ tùng cũng có cảnh báo kịp thời.',
  },
]

export const PRICING: PricingTier[] = [
  {
    name: 'Starter',
    price: '1.990.000',
    unit: '/tháng',
    badge: null,
    highlight: false,
    desc: 'Cho doanh nghiệp mới bắt đầu số hóa vận hành dịch vụ',
    features: ['Tối đa 10 kỹ thuật viên', 'Khách hàng và thiết bị', 'Work order và lịch làm việc', 'Kho phụ tùng cơ bản', 'Báo cáo tháng', 'Email support'],
    cta: 'Dùng thử miễn phí 14 ngày',
  },
  {
    name: 'Professional',
    price: '4.490.000',
    unit: '/tháng',
    badge: 'Phổ biến nhất',
    highlight: true,
    desc: 'Cho đội ngũ tăng trưởng cần báo cáo sâu và phân quyền rõ ràng',
    features: ['Tối đa 50 kỹ thuật viên', 'Tất cả tính năng Starter', 'Dashboard và KPI nâng cao', 'Audit log đầy đủ', 'Multi-tenant isolation', 'Priority support 8/5'],
    cta: 'Bắt đầu ngay',
  },
  {
    name: 'Enterprise',
    price: 'Liên hệ',
    unit: '',
    badge: null,
    highlight: false,
    desc: 'Giải pháp tùy chỉnh cho doanh nghiệp lớn và quy trình đặc thù',
    features: ['Không giới hạn kỹ thuật viên', 'Tất cả tính năng Professional', 'Tích hợp ERP / CRM', 'Custom workflow và SLA', 'Triển khai on-premise', 'Dedicated account manager'],
    cta: 'Đặt lịch tư vấn',
  },
]

export const INTEGRATIONS: Integration[] = [
  { icon: <GlobalOutlined />, name: 'REST API', desc: 'Webhook và API mở' },
  { icon: <TeamOutlined />, name: 'CRM', desc: 'Salesforce, HubSpot' },
  { icon: <DatabaseOutlined />, name: 'ERP', desc: 'SAP, Oracle, MISA' },
  { icon: <AuditOutlined />, name: 'Kế toán', desc: 'Xero, QuickBooks' },
]

export const TRUST_LOGOS = [
  'TechCool HVAC',
  'Lan Anh Elevator',
  'Phong Phú Engineering',
  'Nam Tiến Services',
  'Minh Quang Electric',
  'Delta Facilities',
]

export const MOCK_METRICS: MockMetric[] = [
  { label: 'Work Orders', value: '24', bg: '#eff6ff', icon: 'WO' },
  { label: 'Đang xử lý', value: '8', bg: '#ecfdf5', icon: 'IP' },
  { label: 'Hoàn thành', value: '14', bg: '#f0fdf4', icon: 'OK' },
  { label: 'Phụ tùng', value: '312', bg: '#fef9c3', icon: 'SP' },
]

export const MOCK_ROWS: MockRow[] = [
  { id: 'WO-0041', client: 'Minh Quang Electric', status: 'Đang xử lý', statusBg: '#fef3c7' },
  { id: 'WO-0040', client: 'TechCool HVAC', status: 'Hoàn thành', statusBg: '#dcfce7' },
  { id: 'WO-0039', client: 'Lan Anh Elevator', status: 'Chờ phân công', statusBg: '#f1f5f9' },
]

export const MOCK_NAV_ITEMS = ['Tổng quan', 'Work Orders', 'Kỹ thuật viên', 'Kho phụ tùng', 'Báo cáo']

export const FOOTER_LINKS: Record<string, FooterLink[]> = {
  'Sản phẩm': [
    { label: 'Tính năng', href: '#features' },
    { label: 'Bảng giá', href: '#pricing' },
    { label: 'Kết quả', href: '#stats' },
  ],
  'Doanh nghiệp': [
    { label: 'Khách hàng', href: '#testimonials' },
    { label: 'Tích hợp', href: '#integrations' },
    { label: 'Đăng nhập', href: '/login' },
  ],
  'Liên hệ': [
    { label: 'hello@serviceops.vn', href: 'mailto:hello@serviceops.vn' },
    { label: '0909 000 000', href: 'tel:+84909000000' },
    { label: 'TP. Hồ Chí Minh', href: '#' },
  ],
}
