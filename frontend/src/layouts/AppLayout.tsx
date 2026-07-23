import {
  AppstoreOutlined,
  AuditOutlined,
  BellOutlined,
  CalendarOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  ControlOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Avatar, Badge, Button, Drawer, Dropdown, Empty, Layout, List, Menu, Space, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { notificationsApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime } from '../utils/format'

const { Header, Sider, Content } = Layout

const roleLabels: Record<string, string> = {
  OWNER: 'Chủ doanh nghiệp',
  DISPATCHER: 'Điều phối viên',
  CUSTOMER_SERVICE: 'Chăm sóc khách hàng',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()

  const { data: unread = 0 } = useQuery({
    queryKey: ['notification-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 30_000,
  })

  const { data: notifications } = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsApi.list,
    enabled: notificationsOpen,
  })

  const items = useMemo(() => {
    const role = user?.role ?? ''

    return [
      { key: '/', icon: <DashboardOutlined />, label: <Link to="/">Tổng quan</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN', 'WAREHOUSE_STAFF'] },
      { key: '/customers', icon: <TeamOutlined />, label: <Link to="/customers">Khách hàng</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'] },
      { key: '/assets', icon: <AppstoreOutlined />, label: <Link to="/assets">Thiết bị</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'] },
      { key: '/service-requests', icon: <CustomerServiceOutlined />, label: <Link to="/service-requests">Yêu cầu dịch vụ</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'] },
      { key: '/service-channels', icon: <ControlOutlined />, label: <Link to="/service-channels">Kênh tiếp nhận</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE'] },
      { key: '/work-orders', icon: <CalendarOutlined />, label: <Link to="/work-orders">Work order</Link>, roles: ['OWNER', 'DISPATCHER', 'CUSTOMER_SERVICE', 'TECHNICIAN', 'WAREHOUSE_STAFF'] },
      { key: '/technicians', icon: <ToolOutlined />, label: <Link to="/technicians">Kỹ thuật viên</Link>, roles: ['OWNER', 'DISPATCHER'] },
      { key: '/inventory', icon: <DatabaseOutlined />, label: <Link to="/inventory">Kho phụ tùng</Link>, roles: ['OWNER', 'WAREHOUSE_STAFF', 'TECHNICIAN'] },
      { key: '/audit', icon: <AuditOutlined />, label: <Link to="/audit">Nhật ký hệ thống</Link>, roles: ['OWNER', 'DISPATCHER'] },
    ].filter((item) => item.roles.includes(role)).map(({ roles: _roles, ...item }) => item)
  }, [user?.role])

  const selectedKey = location.pathname === '/' ? '/' : `/${location.pathname.split('/')[1]}`
  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Layout className="app-shell">
      <Sider trigger={null} collapsible collapsed={collapsed} width={268} className="app-sider">
        <div className="brand">
          <div className="brand-mark"><SettingOutlined /></div>
          {!collapsed && (
            <div>
              <strong>ServiceOps</strong>
              <span>Field Service Platform</span>
            </div>
          )}
        </div>

        <Menu theme="dark" mode="inline" selectedKeys={[selectedKey]} items={items} className="app-menu" />

        {!collapsed && (
          <div className="sider-footer">
            <button type="button" className="sider-logout" onClick={handleLogout}>
              <LogoutOutlined />
              <span>Đăng xuất</span>
            </button>
          </div>
        )}
      </Sider>

      <Layout className="app-main">
        <Header className="app-header">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed((value) => !value)}
            className="header-toggle"
          />

          <div className="header-spacer" />

          <Button type="text" className="notification-button" onClick={() => setNotificationsOpen(true)}>
            <Badge count={unread} size="small"><BellOutlined className="header-icon" /></Badge>
          </Button>

          <Dropdown
            menu={{
              items: [
                { key: 'profile', icon: <SettingOutlined />, label: 'Thông tin tài khoản', disabled: true },
                { type: 'divider' },
                { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout },
              ],
            }}
            trigger={['click']}
          >
            <button className="user-menu">
              <Avatar className="user-avatar">{user?.displayName?.charAt(0) ?? 'U'}</Avatar>
              <span className="user-menu-copy">
                <strong>{user?.displayName}</strong>
                <small>{roleLabels[user?.role ?? ''] ?? user?.role}</small>
              </span>
            </button>
          </Dropdown>
        </Header>

        <Content className="app-content"><Outlet /></Content>
      </Layout>

      <Drawer title="Thông báo" open={notificationsOpen} onClose={() => setNotificationsOpen(false)} size="default">
        {notifications?.content.length ? (
          <List
            dataSource={notifications.content}
            renderItem={(item) => (
              <List.Item
                className={item.readAt ? '' : 'notification-unread'}
                onClick={async () => {
                  if (!item.readAt) {
                    await notificationsApi.markRead(item.id)
                    queryClient.invalidateQueries({ queryKey: ['notifications'] })
                    queryClient.invalidateQueries({ queryKey: ['notification-count'] })
                  }
                }}
              >
                <List.Item.Meta
                  avatar={<Badge dot={!item.readAt}><Avatar icon={<BellOutlined />} /></Badge>}
                  title={item.title}
                  description={
                    <Space orientation="vertical" size={2}>
                      <Typography.Text>{item.message}</Typography.Text>
                      <Typography.Text type="secondary">{formatDateTime(item.createdAt)}</Typography.Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        ) : <Empty description="Chưa có thông báo" />}
      </Drawer>
    </Layout>
  )
}
