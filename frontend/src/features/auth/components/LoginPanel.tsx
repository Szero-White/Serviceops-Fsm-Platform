import {
  ClockCircleOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import type { ReactNode } from 'react'
import { DEMO_PASSWORD, DemoAccountSelector } from './DemoAccountSelector'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

export type LoginFormValues = {
  username: string
  password: string
}

const DEFAULT_LOGIN_VALUES: LoginFormValues = import.meta.env.DEV
  ? { username: 'owner', password: DEMO_PASSWORD }
  : { username: 'owner', password: '' }

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

export function LoginPanel({
  error,
  loading,
  onSubmit,
}: {
  error?: string
  loading: boolean
  onSubmit: (values: LoginFormValues) => Promise<void>
}) {
  const [form] = Form.useForm<LoginFormValues>()
  const handleFormValidationFailed = useFormValidationFeedback()

  const selectDemoAccount = (username: string, password: string) => {
    form.setFieldsValue({ username, password })
  }

  return (
    <section className="login-panel">
      <div className="login-panel-shell">
        <div className="login-panel-badges">
          <TagLine icon={<ClockCircleOutlined />} label="Demo nhanh" value="Chọn vai trò và đăng nhập" />
          <TagLine icon={<SafetyCertificateOutlined />} label="Bảo mật" value="JWT · RBAC · Tenant isolation" />
        </div>

        <Card className="login-card" variant="borderless">
          <div className="login-card-heading">
            <Typography.Title level={2}>Đăng nhập ServiceOps</Typography.Title>
            <Typography.Text type="secondary">
              Khám phá hệ thống theo đúng quyền của từng vai trò nghiệp vụ.
            </Typography.Text>
          </div>

          {error && <Alert type="error" showIcon message={error} className="login-alert" />}

          <Form
            form={form}
            layout="vertical"
            onFinish={onSubmit}
            initialValues={DEFAULT_LOGIN_VALUES}
            onFinishFailed={handleFormValidationFailed}
            scrollToFirstError
            requiredMark
          >
            <Form.Item
              label="Tên đăng nhập"
              name="username"
              rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="owner" autoComplete="username" />
            </Form.Item>

            <Form.Item
              label="Mật khẩu"
              name="password"
              rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder={DEMO_PASSWORD ? 'Mật khẩu demo' : 'Nhập mật khẩu demo'}
                autoComplete="current-password"
              />
            </Form.Item>

            <Button type="primary" htmlType="submit" block loading={loading} size="large">
              Đăng nhập
            </Button>
          </Form>

          <DemoAccountSelector onSelect={selectDemoAccount} />
        </Card>
      </div>
    </section>
  )
}
