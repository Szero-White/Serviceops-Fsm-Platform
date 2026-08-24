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

const legacyNotificationStatusLabels: Record<string, string> = {
  ON_THE_WAY: 'Kỹ thuật viên đang di chuyển',
  'Đang di chuyển': 'Kỹ thuật viên đang di chuyển',
  IN_PROGRESS: 'Công việc đang thực hiện',
  'Đang thực hiện': 'Công việc đang thực hiện',
  WAITING_FOR_PARTS: 'Cần xử lý phụ tùng',
  'Chờ phụ tùng': 'Cần xử lý phụ tùng',
  COMPLETED: 'Công việc đã hoàn thành',
  'Đã hoàn thành': 'Công việc đã hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách đã xác nhận',
  'Khách hàng đã nghiệm thu': 'Khách đã xác nhận',
  CLOSED: 'Phiếu đã hoàn tất',
  'Đã đóng': 'Phiếu đã hoàn tất',
  CANCELLED: 'Phiếu đã hủy',
  'Đã hủy': 'Phiếu đã hủy',
  REOPENED: 'Công việc cần xử lý lại',
  'Đã mở lại': 'Công việc cần xử lý lại',
}

const legacyNotificationStatusMessages: Record<string, string> = {
  ON_THE_WAY: 'Kỹ thuật viên đã bắt đầu di chuyển tới địa điểm thực hiện.',
  'Đang di chuyển': 'Kỹ thuật viên đã bắt đầu di chuyển tới địa điểm thực hiện.',
  IN_PROGRESS: 'Kỹ thuật viên đã bắt đầu xử lý công việc.',
  'Đang thực hiện': 'Kỹ thuật viên đã bắt đầu xử lý công việc.',
  WAITING_FOR_PARTS: 'Công việc đang chờ phụ tùng để tiếp tục xử lý.',
  'Chờ phụ tùng': 'Công việc đang chờ phụ tùng để tiếp tục xử lý.',
  COMPLETED: 'Kỹ thuật viên đã hoàn thành công việc. Chăm sóc khách hàng cần kiểm tra kết quả.',
  'Đã hoàn thành': 'Kỹ thuật viên đã hoàn thành công việc. Chăm sóc khách hàng cần kiểm tra kết quả.',
  CUSTOMER_ACCEPTED: 'Khách hàng đã đồng ý với kết quả xử lý.',
  'Khách hàng đã nghiệm thu': 'Khách hàng đã đồng ý với kết quả xử lý.',
  CLOSED: 'Phiếu đã được xác nhận với khách và đóng hoàn tất.',
  'Đã đóng': 'Phiếu đã được xác nhận với khách và đóng hoàn tất.',
  CANCELLED: 'Phiếu đã bị hủy. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  'Đã hủy': 'Phiếu đã bị hủy. Mở Lịch sử phiếu nếu cần kiểm tra chi tiết.',
  REOPENED: 'Phiếu đã được mở lại và cần tiếp tục xử lý.',
  'Đã mở lại': 'Phiếu đã được mở lại và cần tiếp tục xử lý.',
}

function notificationDisplayText(item: { title: string; message: string }) {
  const legacyStatusUpdate = /^Cập nhật (WO-[^:]+): .+ → (.+)$/.exec(item.title)
  if (legacyStatusUpdate) {
    const [, code, targetStatus] = legacyStatusUpdate
    const normalizedTarget = targetStatus.trim()
    const friendlyTitle = legacyNotificationStatusLabels[normalizedTarget]
    const friendlyMessage = legacyNotificationStatusMessages[normalizedTarget]
    return friendlyTitle
      ? {
          ...item,
          title: `${friendlyTitle}: ${code}`,
          message: friendlyMessage ?? item.message,
        }
      : item
  }

  const assignedWorkOrder = /^Công việc mới: (WO-.+)$/.exec(item.title)
  if (assignedWorkOrder) {
    return {
      ...item,
      title: `Bạn được giao công việc mới: ${assignedWorkOrder[1]}`,
      message: 'Mở phiếu để xem nội dung, khách hàng và thời gian thực hiện.',
    }
  }

  const rescheduledWorkOrder = /^Lịch công việc được cập nhật: (WO-.+)$/.exec(item.title)
  if (rescheduledWorkOrder) {
    return {
      ...item,
      title: `Lịch làm việc đã thay đổi: ${rescheduledWorkOrder[1]}`,
      message: 'Thời gian thực hiện đã được điều chỉnh. Mở Lịch của tôi để xem lịch mới.',
    }
  }

  const removedAssignment = /^Công việc đã được điều phối lại: (WO-.+)$/.exec(item.title)
  if (removedAssignment) {
    return {
      ...item,
      title: `Lịch làm việc đã thay đổi: ${removedAssignment[1]}`,
      message: 'Bạn không còn được phân công phiếu này. Kiểm tra Lịch của tôi để cập nhật kế hoạch.',
    }
  }

  if (item.title === 'Hồ sơ kỹ thuật viên được cập nhật') {
    return {
      ...item,
      title: `Thông tin kỹ thuật viên đã thay đổi${item.message ? `: ${item.message}` : ''}`,
      message: 'Kiểm tra kỹ năng, liên hệ và trạng thái hoạt động trước khi phân công công việc.',
    }
  }

  if (item.title === 'Tệp đính kèm mới') {
    return {
      ...item,
      title: 'Có tệp đính kèm mới',
      message: item.message ? `Tệp "${item.message}" vừa được thêm. Mở đối tượng liên quan để xem.` : 'Có tệp mới được thêm vào hệ thống.',
    }
  }

  const exactTitleCopies: Record<string, { title: string; messagePrefix?: string; messageSuffix?: string }> = {
    'Yêu cầu dịch vụ mới': {
      title: 'Yêu cầu mới cần tiếp nhận',
      messagePrefix: 'Nội dung: ',
      messageSuffix: '. Mở Yêu cầu dịch vụ để kiểm tra và xử lý.',
    },
    'Yêu cầu dịch vụ được cập nhật': {
      title: 'Yêu cầu dịch vụ vừa được cập nhật',
      messagePrefix: 'Nội dung: ',
      messageSuffix: '. Mở Yêu cầu dịch vụ để xem thông tin mới.',
    },
    'Yêu cầu dịch vụ đã huỷ': {
      title: 'Yêu cầu dịch vụ đã hủy',
      messagePrefix: 'Nội dung: ',
      messageSuffix: '. Yêu cầu này không còn tiếp tục xử lý.',
    },
    'Yêu cầu dịch vụ đã xoá': {
      title: 'Yêu cầu dịch vụ đã được xóa',
      messagePrefix: 'Nội dung: ',
      messageSuffix: '.',
    },
    'Đã import danh sách khách hàng': {
      title: 'Đã thêm khách hàng từ tệp',
    },
    'Đã import danh sách thiết bị': {
      title: 'Đã thêm thiết bị từ tệp',
    },
    'Đã import danh mục phụ tùng': {
      title: 'Đã thêm phụ tùng từ tệp',
    },
  }

  const exactCopy = exactTitleCopies[item.title]
  if (exactCopy) {
    return {
      ...item,
      title: exactCopy.title,
      message: `${exactCopy.messagePrefix ?? ''}${item.message}${exactCopy.messageSuffix ?? ''}`,
    }
  }

  const prefixReplacements: Array<[RegExp, (value: string) => string]> = [
    [/^Khách hàng mới: (.+)$/, (value) => `Đã thêm khách hàng: ${value}`],
    [/^Khách hàng được cập nhật: (.+)$/, (value) => `Thông tin khách hàng đã thay đổi: ${value}`],
    [/^Khách hàng đã xoá: (.+)$/, (value) => `Đã xóa khách hàng: ${value}`],
    [/^Thiết bị mới: (.+)$/, (value) => `Đã thêm thiết bị: ${value}`],
    [/^Thiết bị được cập nhật: (.+)$/, (value) => `Thông tin thiết bị đã thay đổi: ${value}`],
    [/^Thiết bị đã xoá: (.+)$/, (value) => `Đã xóa thiết bị: ${value}`],
    [/^Kênh tiếp nhận mới: (.+)$/, (value) => `Đã thêm kênh tiếp nhận: ${value}`],
    [/^Kênh tiếp nhận được cập nhật: (.+)$/, (value) => `Thông tin kênh tiếp nhận đã thay đổi: ${value}`],
    [/^Kênh tiếp nhận đã xoá: (.+)$/, (value) => `Đã xóa kênh tiếp nhận: ${value}`],
    [/^Đã nhập kho (.+)$/, (value) => `Kho vừa được bổ sung: ${value}`],
    [/^Phụ tùng sắp hết: (.+)$/, (value) => `Cần bổ sung tồn kho: ${value}`],
    [/^Tồn thấp sau kiểm kê: (.+)$/, (value) => `Cần bổ sung tồn kho sau kiểm kê: ${value}`],
    [/^Tồn thấp theo ngưỡng mới: (.+)$/, (value) => `Cần kiểm tra tồn kho: ${value}`],
    [/^Chênh lệch kiểm kê: (.+)$/, (value) => `Kiểm kê có chênh lệch: ${value}`],
  ]

  for (const [pattern, replaceTitle] of prefixReplacements) {
    const match = pattern.exec(item.title)
    if (match) {
      return {
        ...item,
        title: replaceTitle(match[1]),
      }
    }
  }

  return item
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
                    title={<Typography.Text strong={isUnread}>{displayItem.title}</Typography.Text>}
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
