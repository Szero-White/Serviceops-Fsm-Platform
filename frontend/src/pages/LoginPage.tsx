import { LockOutlined, SafetyCertificateOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/http'
import { useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const { login, authenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  if (authenticated) {
    return <Navigate to="/" replace />
  }

  const submit = async (values: { username: string; password: string }) => {
    setLoading(true)
    setError(undefined)
    try {
      await login(values.username, values.password)
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
      <section className="login-hero">
        <div className="login-brand"><div className="brand-mark"><ToolOutlined /></div><strong>ServiceOps</strong></div>
        <div className="login-copy">
          <span className="eyebrow">FIELD SERVICE MANAGEMENT</span>
          <h1>Điều phối dịch vụ<br />rõ ràng, đúng hẹn.</h1>
          <p>Quản lý khách hàng, thiết bị, work order, lịch kỹ thuật viên và phụ tùng trên một luồng vận hành thống nhất.</p>
          <div className="login-benefits">
            <div><SafetyCertificateOutlined /><span><strong>Phân quyền rõ ràng</strong><small>JWT, role và tenant isolation</small></span></div>
            <div><ToolOutlined /><span><strong>Đúng nghiệp vụ hiện trường</strong><small>Lịch, trạng thái và lịch sử thiết bị</small></span></div>
          </div>
        </div>
        <div className="login-hero-footer">Local-first MVP · Dữ liệu demo được tạo tự động</div>
      </section>

      <section className="login-panel">
        <Card className="login-card" bordered={false}>
          <div className="login-card-heading">
            <Typography.Title level={2}>Chào mừng trở lại</Typography.Title>
            <Typography.Text type="secondary">Đăng nhập để tiếp tục quản lý vận hành dịch vụ.</Typography.Text>
          </div>
          {error && <Alert type="error" showIcon message={error} className="login-alert" />}
          <Form layout="vertical" onFinish={submit} initialValues={{ username: 'owner', password: '123456' }} requiredMark={false}>
            <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
              <Input prefix={<UserOutlined />} placeholder="owner" autoComplete="username" />
            </Form.Item>
            <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="••••••" autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} size="large">Đăng nhập</Button>
          </Form>
          <div className="demo-accounts">
            <strong>Tài khoản demo</strong>
            <Space direction="vertical" size={3}>
              <Typography.Text code>owner / 123456</Typography.Text>
              <Typography.Text type="secondary">dispatcher · technician · warehouse · customer-service</Typography.Text>
            </Space>
          </div>
        </Card>
      </section>
    </div>
  )
}
