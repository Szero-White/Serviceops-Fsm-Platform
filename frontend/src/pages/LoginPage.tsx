import {
  ArrowRightOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/http'
import { useAuth } from '../auth/AuthContext'

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
    title: 'Phân quyền rõ ràng',
    description: 'JWT, vai trò và tenant isolation',
  },
  {
    icon: <ToolOutlined />,
    title: 'Đúng nghiệp vụ hiện trường',
    description: 'Lịch, trạng thái và lịch sử thiết bị',
  },
]

function BrandMark() {
  return (
    <div className="login-brand">
      <div className="brand-mark">
        <ToolOutlined />
      </div>
      <strong>ServiceOps</strong>
    </div>
  )
}

function LoginHero() {
  return (
    <section className="login-hero">
      <BrandMark />

      <div className="login-copy">
        <span className="eyebrow">FIELD SERVICE MANAGEMENT</span>
        <h1>
          Điều phối dịch vụ
          <br />
          rõ ràng, đúng hẹn.
        </h1>
        <p>
          Quản lý khách hàng, thiết bị, work order, lịch kỹ thuật viên và phụ tùng trên một luồng
          vận hành thống nhất.
        </p>

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
          <ArrowRightOutlined /> Khám phá tính năng &amp; bảng giá
        </Link>
        <span className="login-hero-footer-sep">·</span>
        Local-first MVP · Dữ liệu demo được tạo tự động
      </div>
    </section>
  )
}

function DemoAccounts() {
  return (
    <div className="demo-accounts">
      <strong>Tài khoản demo</strong>
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
      <Card className="login-card" variant="borderless">
        <div className="login-card-heading">
          <Typography.Title level={2}>Chào mừng trở lại</Typography.Title>
          <Typography.Text type="secondary">
            Đăng nhập để tiếp tục quản lý vận hành dịch vụ.
          </Typography.Text>
        </div>

        {error && <Alert type="error" showIcon title={error} className="login-alert" />}

        <Form layout="vertical" onFinish={onSubmit} initialValues={DEMO_CREDENTIALS} requiredMark={false}>
          <Form.Item
            label="Tên đăng nhập"
            name="username"
            rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}
          >
            <Input prefix={<UserOutlined />} placeholder={DEMO_CREDENTIALS.username} autoComplete="username" />
          </Form.Item>

          <Form.Item
            label="Mật khẩu"
            name="password"
            rules={[{ required: true, message: 'Nhập mật khẩu' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="123456" autoComplete="current-password" />
          </Form.Item>

          <Button type="primary" htmlType="submit" block loading={loading} size="large">
            Đăng nhập
          </Button>
        </Form>

        <DemoAccounts />
      </Card>
    </section>
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
