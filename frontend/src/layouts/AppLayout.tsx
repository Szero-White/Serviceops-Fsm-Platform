import {
  AppstoreOutlined,
  AuditOutlined,
  BellOutlined,
  CalendarOutlined,
  CheckOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  ControlOutlined,
  HistoryOutlined,
  ProfileOutlined,
  ReconciliationOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MailOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Avatar, Badge, Button, Drawer, Dropdown, Empty, Layout, List, Menu, Pagination, Segmented, Space, Tooltip, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/http'
import { QueryErrorAlert } from '../components/QueryErrorAlert'
import { notificationsApi } from '../features/notifications/api'
import { notificationDisplayText } from '../features/notifications/presentation'
import { useAuth } from '../features/auth/AuthContext'
import { AiHelpAssistant } from '../features/ai/components/AiHelpAssistant'
import { formatDateTime } from '../utils/format'
import { canAccessRoute } from '../router/routeAccess'

const { Header, Sider, Content } = Layout
const NOTIFICATION_PAGE_SIZE = 30

const roleLabels: Record<string, string> = {
  OWNER: 'Quản trị hệ thống',
  DISPATCHER: 'Điều phối viên',
  CUSTOMER_SERVICE: 'Chăm sóc khách hàng',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [notificationView, setNotificationView] = useState<'all' | 'unread'>('all')
  const [notificationPage, setNotificationPage] = useState(0)
  const [notificationActionId, setNotificationActionId] = useState<string>()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { message } = App.useApp()

  const notificationCountKey = ['notification-count', user?.id] as const
  const notificationsKey = ['notifications', user?.id] as const
  const notificationsListKey = [...notificationsKey, notificationView, notificationPage] as const

  const notificationCountQuery = useQuery({
    queryKey: notificationCountKey,
    queryFn: notificationsApi.unreadCount,
    enabled: Boolean(user?.id),
    staleTime: 0,
    refetchOnMount: 'always',
    refetchInterval: 30_000,
  })
  const unread = notificationCountQuery.data ?? 0

  const notificationsQuery = useQuery({
    queryKey: notificationsListKey,
    queryFn: () => notificationsApi.list(notificationPage, NOTIFICATION_PAGE_SIZE, notificationView === 'unread'),
    enabled: Boolean(user?.id) && notificationsOpen,
    staleTime: 0,
    refetchOnMount: 'always',
    refetchInterval: 30_000,
  })
  const notifications = notificationsQuery.data
  const notificationsFetching = notificationsQuery.isFetching
  useEffect(() => {
    if (notifications && notificationPage > 0 && notificationPage >= notifications.totalPages) {
      setNotificationPage(Math.max(notifications.totalPages - 1, 0))
    }
  }, [notificationPage, notifications])

  const items = useMemo(() => {
    const role = user?.role
    const sections = [
      {
        key: 'operations',
        label: 'Vận hành',
        children: [
          { key: '/', icon: <DashboardOutlined />, label: <Link to="/">Tổng quan</Link> },
          { key: '/service-requests', icon: <CustomerServiceOutlined />, label: <Link to="/service-requests">Yêu cầu dịch vụ</Link> },
          { key: '/work-orders', icon: <CalendarOutlined />, label: <Link to="/work-orders">Phiếu công việc</Link> },
          { key: '/schedule', icon: <CalendarOutlined />, label: <Link to="/schedule">Lịch điều phối</Link> },
          { key: '/my-schedule', icon: <CalendarOutlined />, label: <Link to="/my-schedule">Lịch của tôi</Link> },
          { key: '/work-order-history', icon: <HistoryOutlined />, label: <Link to="/work-order-history">Lịch sử phiếu</Link> },
        ],
      },
      {
        key: 'master-data',
        label: 'Danh mục & nguồn lực',
        children: [
          { key: '/customers', icon: <TeamOutlined />, label: <Link to="/customers">Khách hàng</Link> },
          { key: '/assets', icon: <AppstoreOutlined />, label: <Link to="/assets">Thiết bị</Link> },
          { key: '/service-channels', icon: <ControlOutlined />, label: <Link to="/service-channels">Kênh tiếp nhận</Link> },
          { key: '/technicians', icon: <ToolOutlined />, label: <Link to="/technicians">Kỹ thuật viên</Link> },
          { key: '/inventory', icon: <DatabaseOutlined />, label: <Link to="/inventory">Kho phụ tùng</Link> },
          { key: '/inventory-stocktake', icon: <ReconciliationOutlined />, label: <Link to="/inventory-stocktake">Kiểm kê tồn kho</Link> },
          { key: '/inventory-movements', icon: <ProfileOutlined />, label: <Link to="/inventory-movements">Lịch sử biến động</Link> },
        ],
      },
      {
        key: 'governance',
        label: 'Quản trị',
        children: [
          { key: '/audit', icon: <AuditOutlined />, label: <Link to="/audit">Nhật ký hệ thống</Link> },
          { key: '/users', icon: <UserSwitchOutlined />, label: <Link to="/users">Người dùng</Link> },
        ],
      },
    ]

    return sections
      .map((section) => ({
        type: 'group' as const,
        key: section.key,
        label: section.label,
        children: section.children
          .filter((item) => canAccessRoute(role, item.key)),
      }))
      .filter((section) => section.children.length > 0)
  }, [user?.role])

  const selectedKey = location.pathname === '/' ? '/' : `/${location.pathname.split('/')[1]}`
  const refreshNotifications = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: notificationsKey }),
      queryClient.invalidateQueries({ queryKey: notificationCountKey }),
    ])
  }

  const updateNotificationReadState = async (notificationId: string, markAsRead: boolean) => {
    setNotificationActionId(notificationId)
    try {
      if (markAsRead) {
        await notificationsApi.markRead(notificationId)
      } else {
        await notificationsApi.markUnread(notificationId)
      }
      await refreshNotifications()
    } catch (error) {
      message.error(apiErrorMessage(error))
    } finally {
      setNotificationActionId(undefined)
    }
  }

  const handleLogout = () => {
    queryClient.clear()
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <Layout className="app-shell">
      <Sider trigger={null} collapsible collapsed={collapsed} width={228} className="app-sider">
        <div className="brand">
          <div className="brand-mark"><SettingOutlined /></div>
          {!collapsed && (
            <div>
              <strong>ServiceOps</strong>
              <span>Nền tảng dịch vụ hiện trường</span>
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

          <Button
            type="text"
            className="notification-button"
            onClick={() => {
              setNotificationsOpen(true)
              void queryClient.invalidateQueries({ queryKey: notificationCountKey })
              void queryClient.invalidateQueries({ queryKey: notificationsKey })
            }}
          >
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
        <AiHelpAssistant />
      </Layout>

      <Drawer title="Thông báo" open={notificationsOpen} onClose={() => setNotificationsOpen(false)} size="default">
        <Space orientation="vertical" size={12} className="notification-drawer-content">
          <Segmented
            block
            value={notificationView}
            onChange={(value) => {
              setNotificationView(value as 'all' | 'unread')
              setNotificationPage(0)
            }}
            options={[
              { label: 'Tất cả', value: 'all' },
              { label: `Chưa đọc (${unread})`, value: 'unread' },
            ]}
          />

          {notificationCountQuery.isError ? (
            <QueryErrorAlert
              title="Chưa cập nhật được số thông báo chưa đọc"
              error={notificationCountQuery.error}
              onRetry={() => notificationCountQuery.refetch()}
            />
          ) : null}

          {notificationsQuery.isError ? (
            <QueryErrorAlert
              title="Chưa tải được danh sách thông báo"
              error={notificationsQuery.error}
              onRetry={() => notificationsQuery.refetch()}
            />
          ) : null}

          <List
            loading={notificationsFetching}
            dataSource={notifications?.content ?? []}
            locale={{
              emptyText: (
                <Empty description={notificationView === 'unread' ? 'Không có thông báo chưa đọc' : 'Chưa có thông báo'} />
              ),
            }}
            renderItem={(item) => {
              const isUnread = !item.readAt
              const actionLabel = isUnread ? 'Đánh dấu đã đọc' : 'Đánh dấu chưa đọc'
              const displayItem = notificationDisplayText(item)

              return (
                <List.Item
                  className={`notification-item ${isUnread ? 'notification-item-unread' : 'notification-item-read'}`}
                  onClick={() => {
                    if (isUnread && notificationActionId !== item.id) {
                      void updateNotificationReadState(item.id, true)
                    }
                  }}
                >
                  <List.Item.Meta
                    avatar={<Badge dot={isUnread}><Avatar icon={<BellOutlined />} /></Badge>}
                    title={<Typography.Text className="notification-item-title">{displayItem.title}</Typography.Text>}
                    description={
                      <Space orientation="vertical" size={2}>
                        <Typography.Text>{displayItem.message}</Typography.Text>
                        <Typography.Text type="secondary">{formatDateTime(item.createdAt)}</Typography.Text>
                      </Space>
                    }
                  />

                  <Tooltip title={actionLabel}>
                    <Button
                      type="text"
                      shape="circle"
                      className="notification-read-state-button"
                      aria-label={actionLabel}
                      icon={isUnread ? <CheckOutlined /> : <MailOutlined />}
                      loading={notificationActionId === item.id}
                      disabled={Boolean(notificationActionId && notificationActionId !== item.id)}
                      onClick={(event) => {
                        event.stopPropagation()
                        void updateNotificationReadState(item.id, isUnread)
                      }}
                    />
                  </Tooltip>
                </List.Item>
              )
            }}
          />

          {(notifications?.totalElements ?? 0) > NOTIFICATION_PAGE_SIZE ? (
            <Pagination
              size="small"
              current={notificationPage + 1}
              pageSize={NOTIFICATION_PAGE_SIZE}
              total={notifications?.totalElements ?? 0}
              showSizeChanger={false}
              onChange={(page) => setNotificationPage(page - 1)}
            />
          ) : null}
        </Space>
      </Drawer>
    </Layout>
  )
}
