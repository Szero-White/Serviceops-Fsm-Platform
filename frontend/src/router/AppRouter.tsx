import { Spin } from 'antd'
import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { AppLayout } from '../layouts/AppLayout'

type LazyPage = LazyExoticComponent<ComponentType>
type AppRoute = { path?: string; index?: true; Page: LazyPage }

const page = <TModule, TExport extends keyof TModule>(
  importer: () => Promise<TModule>,
  exportName: TExport,
) =>
  lazy(async () => ({
    default: (await importer())[exportName] as ComponentType,
  }))

const LoginPage = page(() => import('../features/auth/pages/LoginPage'), 'LoginPage')
const LandingPage = page(() => import('../pages/LandingPage'), 'LandingPage')

const protectedRoutes: AppRoute[] = [
  { index: true, Page: page(() => import('../features/dashboard/pages/DashboardPage'), 'DashboardPage') },
  { path: 'users', Page: page(() => import('../features/users/pages/UsersPage'), 'UsersPage') },
  { path: 'customers', Page: page(() => import('../features/customers/pages/CustomersPage'), 'CustomersPage') },
  { path: 'assets', Page: page(() => import('../features/assets/pages/AssetsPage'), 'AssetsPage') },
  { path: 'service-requests', Page: page(() => import('../features/service-requests/pages/ServiceRequestsPage'), 'ServiceRequestsPage') },
  { path: 'service-channels', Page: page(() => import('../features/service-channels/pages/ServiceChannelsPage'), 'ServiceChannelsPage') },
  { path: 'work-orders', Page: page(() => import('../features/work-orders/pages/WorkOrdersPage'), 'WorkOrdersPage') },
  { path: 'work-order-history', Page: page(() => import('../features/work-orders/pages/WorkOrderHistoryPage'), 'WorkOrderHistoryPage') },
  { path: 'technicians', Page: page(() => import('../features/technicians/pages/TechniciansPage'), 'TechniciansPage') },
  { path: 'inventory', Page: page(() => import('../features/inventory/pages/InventoryPage'), 'InventoryPage') },
  { path: 'audit', Page: page(() => import('../features/audit/pages/AuditPage'), 'AuditPage') },
]

function RouteFallback() {
  return (
    <div className="route-fallback">
      <Spin size="large" description="Đang tải dữ liệu..." />
    </div>
  )
}

export function AppRouter() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route path="/landing" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            {protectedRoutes.map(({ path, index, Page }) => (
              <Route key={path ?? 'dashboard'} path={path} index={index} element={<Page />} />
            ))}
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
