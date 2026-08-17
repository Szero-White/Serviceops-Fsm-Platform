import {
  ArrowRightOutlined,
  BarChartOutlined,
  SafetyCertificateOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

type LoginBenefit = {
  icon: ReactNode
  title: string
  description: string
}

const LOGIN_BENEFITS: LoginBenefit[] = [
  {
    icon: <SafetyCertificateOutlined />,
    title: 'Kiểm soát truy cập',
    description: 'JWT, RBAC và cô lập dữ liệu theo tenant',
  },
  {
    icon: <ToolOutlined />,
    title: 'Điều phối vận hành',
    description: 'Work order, lịch kỹ thuật viên và kho phụ tùng',
  },
  {
    icon: <BarChartOutlined />,
    title: 'Theo dõi minh bạch',
    description: 'Dashboard, notification và audit trail',
  },
]

const LOGIN_METRICS = [
  { value: '5', label: 'Vai trò nghiệp vụ' },
  { value: '1', label: 'Luồng vận hành xuyên suốt' },
  { value: '24/7', label: 'Sẵn sàng demo' },
]

function BrandMark() {
  return (
    <div className="login-brand">
      <div className="brand-mark"><ToolOutlined /></div>
      <strong>ServiceOps</strong>
    </div>
  )
}

export function LoginHero() {
  return (
    <section className="login-hero">
      <div className="login-hero-orb-1" aria-hidden="true" />
      <div className="login-hero-orb-2" aria-hidden="true" />
      <BrandMark />

      <div className="login-copy">
        <span className="eyebrow">FIELD SERVICE OPERATIONS PLATFORM</span>
        <h1>
          Điều phối dịch vụ
          <br />
          rõ ràng, nhất quán.
        </h1>
        <p>
          Quản lý khách hàng, thiết bị, yêu cầu dịch vụ, phiếu công việc, lịch kỹ thuật viên
          và phụ tùng trong một quy trình vận hành thống nhất.
        </p>

        <div className="login-proof-grid">
          {LOGIN_METRICS.map((metric) => (
            <div key={metric.label}>
              <strong>{metric.value}</strong>
              <span>{metric.label}</span>
            </div>
          ))}
        </div>

        <div className="login-benefits">
          {LOGIN_BENEFITS.map((benefit) => (
            <div key={benefit.title}>
              {benefit.icon}
              <span>
                <strong>{benefit.title}</strong>
                <small>{benefit.description}</small>
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="login-hero-footer">
        <Link to="/landing" className="login-hero-landing-link">
          <ArrowRightOutlined /> Xem tổng quan sản phẩm
        </Link>
        <span className="login-hero-footer-sep">·</span>
        Production-oriented demo · Dữ liệu mẫu cô lập
      </div>
    </section>
  )
}
