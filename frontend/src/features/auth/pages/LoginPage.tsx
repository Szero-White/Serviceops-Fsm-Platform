import {
  ArrowRightOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { useAuth } from '../AuthContext'

type LoginFormValues = {
  username: string
  password: string
}

type LoginBenefit = {
  icon: ReactNode
  title: string
  description: string
}

const DEMO_CREDENTIALS: LoginFormValues = {
  username: 'owner',
  password: '123456',
}

const DEMO_ROLES = ['dispatcher', 'technician', 'warehouse', 'customer-service']

const LOGIN_BENEFITS: LoginBenefit[] = [
  {
    icon: <SafetyCertificateOutlined />,
    title: 'Bảo mật tuyệt đối',
    description: 'Xác thực JWT, phân quyền theo vai trò',
  },
  {
    icon: <ToolOutlined />,
    title: 'Quản lý thông minh',
    description: 'Lịch kỹ thuật, theo dõi thiết bị, nhật ký hoạt động',
  },
  {
    icon: <BarChartOutlined />,
    title: 'Báo cáo thời gian thực',
    description: 'Cập nhật tức thì khi đơn hàng thay đổi',
  },
]

const LOGIN_METRICS = [
  { value: '5+', label: 'Vai trò vận hành' },
  { value: '100%', label: 'Dữ liệu nội bộ' },
  { value: '24/7', label: 'Hỗ trợ demo' },
]

function BrandMark() {
  return (
    <div className="login-brand">
      <div className="brand-mark"><ToolOutlined /></div>
      <strong>ServiceOps</strong>
    </div>
  )
}

function LoginHero() {
  return (
    <section className="login-hero">
      <div className="login-hero-orb-1" aria-hidden="true" />
      <div className="login-hero-orb-2" aria-hidden="true" />
      <BrandMark />

      <div className="login-copy">
        <span className="eyebrow">QUẢN LÝ DỊCH VỤ HIỆN TRƯỜNG</span>
        <h1>
          Điều phối dịch vụ
          <br />
          chuyên nghiệp, hiệu quả.
        </h1>
        <p>
          Giải pháp toàn diện quản lý khách hàng, thiết bị, đơn hàng, lịch kỹ thuật và kho phụ tùng - mọi thứ trong một nền tảng thống nhất.
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
          <ArrowRightOutlined /> Xem tính năng và bảng giá
        </Link>
        <span className="login-hero-footer-sep">·</span>
        Phiên bản demo · Dữ liệu mẫu sẵn sàng
      </div>
    </section>
  )
}

function DemoAccounts() {
  return (
    <div className="demo-accounts">
      <strong>Tài khoản dùng thử</strong>
      <Space orientation="vertical" size={3}>
        <Typography.Text code>
          {DEMO_CREDENTIALS.username} / {DEMO_CREDENTIALS.password}
        </Typography.Text>
        <Typography.Text type="secondary">{DEMO_ROLES.join(' · ')}</Typography.Text>
      </Space>
    </div>
  )
}

function LoginPanel({
  error,
  loading,
  onSubmit,
}: {
  error?: string
  loading: boolean
  onSubmit: (values: LoginFormValues) => Promise<void>
}) {
  return (
    <section className="login-panel">
      <div className="login-panel-shell">
        <div className="login-panel-badges">
          <TagLine icon={<ClockCircleOutlined />} label="Demo nhanh" value="Sẵn sàng trong vài phút" />
          <TagLine icon={<SafetyCertificateOutlined />} label="Bảo mật" value="JWT + Phân quyền" />
        </div>

        <Card className="login-card" variant="borderless">
          <div className="login-card-heading">
            <Typography.Title level={2}>Chào mừng trở lại</Typography.Title>
            <Typography.Text type="secondary">
              Đăng nhập để bắt đầu quản lý dịch vụ của bạn.
            </Typography.Text>
          </div>

          {error && <Alert type="error" showIcon message={error} className="login-alert" />}

          <div className="login-card-stats">
            <div><strong>Vận hành trực tiếp</strong><span>Bảng điều khiển, thông báo, nhật ký</span></div>
            <div><strong>Sẵn sàng hiện trường</strong><span>Điều phối, kỹ thuật, kho hàng</span></div>
            <div><strong>Dữ liệu nội bộ</strong><span>Postgres, Flyway, dữ liệu mẫu</span></div>
          </div>

          <Form layout="vertical" onFinish={onSubmit} initialValues={DEMO_CREDENTIALS} requiredMark={false}>
            <Form.Item
              label="Tên đăng nhập"
              name="username"
              rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
            >
              <Input prefix={<UserOutlined />} placeholder={DEMO_CREDENTIALS.username} autoComplete="username" />
            </Form.Item>

            <Form.Item
              label="Mật khẩu"
              name="password"
              rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="123456" autoComplete="current-password" />
            </Form.Item>

            <Button type="primary" htmlType="submit" block loading={loading} size="large">
              Đăng nhập
            </Button>
          </Form>

          <DemoAccounts />
        </Card>
      </div>
    </section>
  )
}

function TagLine({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="login-badge">
      <span className="login-badge-icon">{icon}</span>
      <span>
        <strong>{label}</strong>
        <small>{value}</small>
      </span>
    </div>
  )
}

export function LoginPage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const { login, authenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  if (authenticated) {
    return <Navigate to="/" replace />
  }

  const submit = async ({ username, password }: LoginFormValues) => {
    setLoading(true)
    setError(undefined)

    try {
      await login(username, password)
      const target = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(target, { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <LoginHero />
      <LoginPanel error={error} loading={loading} onSubmit={submit} />
    </div>
  )
}
