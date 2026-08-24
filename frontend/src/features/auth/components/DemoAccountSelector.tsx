import {
  CustomerServiceOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { ReactNode } from 'react'

type DemoAccount = {
  username: string
  role: string
  description: string
  icon: ReactNode
}

const DEMO_ACCOUNTS: DemoAccount[] = [
  { username: 'owner', role: 'Owner', description: 'Toàn quyền quản trị và giám sát', icon: <SafetyCertificateOutlined /> },
  { username: 'dispatcher', role: 'Dispatcher', description: 'Điều phối phiếu công việc và lịch kỹ thuật', icon: <TeamOutlined /> },
  { username: 'customer-service', role: 'Customer Service', description: 'Khách hàng và yêu cầu dịch vụ', icon: <CustomerServiceOutlined /> },
  { username: 'technician', role: 'Technician', description: 'Thực hiện công việc hiện trường', icon: <ToolOutlined /> },
  { username: 'warehouse', role: 'Warehouse', description: 'Tồn kho, kiểm kê và truy vết biến động', icon: <UserOutlined /> },
]

export const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD?.trim() ?? ''

export function DemoAccountSelector({ onSelect }: { onSelect: (username: string, password: string) => void }) {
  return (
    <section className="demo-accounts" aria-label="Tài khoản demo">
      <div className="demo-accounts-heading">
        <div>
          <strong>Trải nghiệm theo vai trò</strong>
          <Typography.Text type="secondary">Chọn tài khoản để điền nhanh thông tin đăng nhập.</Typography.Text>
        </div>
        <MetaBadge>Demo</MetaBadge>
      </div>

      <div className="demo-account-grid">
        {DEMO_ACCOUNTS.map((account) => (
          <Button
            key={account.username}
            className="demo-account-card"
            type="text"
            onClick={() => onSelect(account.username, DEMO_PASSWORD)}
          >
            <span className="demo-account-icon">{account.icon}</span>
            <span className="demo-account-copy">
              <strong>{account.role}</strong>
              <small>{account.description}</small>
              <code>{account.username}</code>
            </span>
          </Button>
        ))}
      </div>

      <Typography.Text className="demo-password-note" type="secondary">
        {DEMO_PASSWORD
          ? 'Mỗi thẻ đại diện cho một vai trò demo. Khi vận hành thực tế, mỗi nhân viên sử dụng tài khoản cá nhân riêng.'
          : 'Public deployment cần cấu hình VITE_DEMO_PASSWORD để bật đăng nhập một chạm.'}
      </Typography.Text>
    </section>
  )
}
