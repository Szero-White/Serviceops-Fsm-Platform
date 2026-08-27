import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  CalendarOutlined,
  ControlOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DollarOutlined,
  HistoryOutlined,
  InboxOutlined,
  ProfileOutlined,
  ReconciliationOutlined,
  TeamOutlined,
  ToolOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import type { UserRole } from '../types'

type NavigationItemKey =
  | 'dashboard'
  | 'serviceRequests'
  | 'workOrders'
  | 'schedule'
  | 'mySchedule'
  | 'workOrderHistory'
  | 'partRequests'
  | 'payments'
  | 'customers'
  | 'assets'
  | 'serviceChannels'
  | 'technicians'
  | 'inventory'
  | 'stocktake'
  | 'inventoryMovements'
  | 'users'
  | 'audit'
  | 'paymentSettings'

export type NavigationItem = {
  key: NavigationItemKey
  path: string
  label: string
  icon: ReactNode
}

export type NavigationSection = {
  key: string
  label: string
  items: readonly NavigationItem[]
}

// Presentation policy only. Route/backend authorization remains authoritative.
const NAVIGATION_ITEMS: Record<NavigationItemKey, NavigationItem> = {
  dashboard: { key: 'dashboard', path: '/', label: 'Tổng quan', icon: <DashboardOutlined /> },
  serviceRequests: { key: 'serviceRequests', path: '/service-requests', label: 'Yêu cầu dịch vụ', icon: <CustomerServiceOutlined /> },
  workOrders: { key: 'workOrders', path: '/work-orders', label: 'Phiếu công việc', icon: <CalendarOutlined /> },
  schedule: { key: 'schedule', path: '/schedule', label: 'Lịch điều phối', icon: <CalendarOutlined /> },
  mySchedule: { key: 'mySchedule', path: '/my-schedule', label: 'Lịch của tôi', icon: <CalendarOutlined /> },
  workOrderHistory: { key: 'workOrderHistory', path: '/work-order-history', label: 'Lịch sử phiếu', icon: <HistoryOutlined /> },
  partRequests: { key: 'partRequests', path: '/part-requests', label: 'Yêu cầu phụ tùng', icon: <InboxOutlined /> },
  payments: { key: 'payments', path: '/payments', label: 'Xử lý thanh toán', icon: <DollarOutlined /> },
  customers: { key: 'customers', path: '/customers', label: 'Khách hàng', icon: <TeamOutlined /> },
  assets: { key: 'assets', path: '/assets', label: 'Thiết bị', icon: <AppstoreOutlined /> },
  serviceChannels: { key: 'serviceChannels', path: '/service-channels', label: 'Kênh tiếp nhận', icon: <ControlOutlined /> },
  technicians: { key: 'technicians', path: '/technicians', label: 'Kỹ thuật viên', icon: <ToolOutlined /> },
  inventory: { key: 'inventory', path: '/inventory', label: 'Kho phụ tùng', icon: <DatabaseOutlined /> },
  stocktake: { key: 'stocktake', path: '/inventory-stocktake', label: 'Kiểm kê tồn kho', icon: <ReconciliationOutlined /> },
  inventoryMovements: { key: 'inventoryMovements', path: '/inventory-movements', label: 'Lịch sử biến động', icon: <ProfileOutlined /> },
  users: { key: 'users', path: '/users', label: 'Người dùng', icon: <UserSwitchOutlined /> },
  audit: { key: 'audit', path: '/audit', label: 'Nhật ký hệ thống', icon: <AuditOutlined /> },
  paymentSettings: { key: 'paymentSettings', path: '/payment-settings', label: 'Thiết lập thanh toán', icon: <BankOutlined /> },
}

const items = (...keys: NavigationItemKey[]) => keys.map((key) => NAVIGATION_ITEMS[key])

export const ROLE_NAVIGATION_SECTIONS: Record<UserRole, readonly NavigationSection[]> = {
  OWNER: [
    { key: 'operations', label: 'Vận hành', items: items('dashboard', 'serviceRequests', 'workOrders', 'schedule', 'workOrderHistory', 'payments') },
    { key: 'customers-resources', label: 'Khách hàng & nguồn lực', items: items('customers', 'assets', 'serviceChannels', 'technicians') },
    { key: 'inventory', label: 'Kho & vật tư', items: items('partRequests', 'inventory', 'stocktake', 'inventoryMovements') },
    { key: 'governance', label: 'Quản trị', items: items('users', 'audit', 'paymentSettings') },
  ],
  CUSTOMER_SERVICE: [
    { key: 'work', label: 'Công việc', items: items('dashboard', 'serviceRequests', 'workOrders', 'payments', 'workOrderHistory') },
    { key: 'customers', label: 'Khách hàng', items: items('customers', 'assets', 'serviceChannels') },
  ],
  DISPATCHER: [
    { key: 'dispatch', label: 'Điều phối', items: items('dashboard', 'workOrders', 'schedule', 'workOrderHistory') },
    { key: 'resources', label: 'Nguồn lực', items: items('technicians') },
  ],
  TECHNICIAN: [
    { key: 'my-work', label: 'Công việc của tôi', items: items('dashboard', 'mySchedule', 'workOrders', 'workOrderHistory') },
  ],
  WAREHOUSE_STAFF: [
    { key: 'inventory', label: 'Kho & vật tư', items: items('partRequests', 'inventory', 'stocktake', 'inventoryMovements') },
  ],
}

export function navigationSectionsForRole(role: UserRole | undefined): readonly NavigationSection[] {
  return role ? ROLE_NAVIGATION_SECTIONS[role] : []
}
