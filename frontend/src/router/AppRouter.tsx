import { Spin } from 'antd'
import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../auth/ProtectedRoute'
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

const LoginPage = page(() => import('../pages/LoginPage'), 'LoginPage')
const LandingPage = page(() => import('../pages/LandingPage'), 'LandingPage')

const protectedRoutes: AppRoute[] = [
  { index: true, Page: page(() => import('../pages/DashboardPage'), 'DashboardPage') },
  { path: 'customers', Page: page(() => import('../pages/CustomersPage'), 'CustomersPage') },
  { path: 'assets', Page: page(() => import('../pages/AssetsPage'), 'AssetsPage') },
  { path: 'service-requests', Page: page(() => import('../pages/ServiceRequestsPage'), 'ServiceRequestsPage') },
  { path: 'work-orders', Page: page(() => import('../pages/WorkOrdersPage'), 'WorkOrdersPage') },
  { path: 'technicians', Page: page(() => import('../pages/TechniciansPage'), 'TechniciansPage') },
  { path: 'inventory', Page: page(() => import('../pages/InventoryPage'), 'InventoryPage') },
  { path: 'audit', Page: page(() => import('../pages/AuditPage'), 'AuditPage') },
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
