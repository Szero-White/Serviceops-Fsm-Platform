import { Spin } from 'antd'
import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import type { UserRole } from '../types'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { RoleRoute } from '../features/auth/RoleRoute'
import { AppLayout } from '../layouts/AppLayout'
import { ROUTE_ACCESS } from './routeAccess'

type LazyPage = LazyExoticComponent<ComponentType>
type AppRoute = { path?: string; index?: true; Page: LazyPage; roles: readonly UserRole[] }

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
  { index: true, Page: page(() => import('../features/dashboard/pages/DashboardPage'), 'DashboardPage'), roles: ROUTE_ACCESS['/'] },
  { path: 'users', Page: page(() => import('../features/users/pages/UsersPage'), 'UsersPage'), roles: ROUTE_ACCESS['/users'] },
  { path: 'customers', Page: page(() => import('../features/customers/pages/CustomersPage'), 'CustomersPage'), roles: ROUTE_ACCESS['/customers'] },
  { path: 'assets', Page: page(() => import('../features/assets/pages/AssetsPage'), 'AssetsPage'), roles: ROUTE_ACCESS['/assets'] },
  { path: 'service-requests', Page: page(() => import('../features/service-requests/pages/ServiceRequestsPage'), 'ServiceRequestsPage'), roles: ROUTE_ACCESS['/service-requests'] },
  { path: 'service-channels', Page: page(() => import('../features/service-channels/pages/ServiceChannelsPage'), 'ServiceChannelsPage'), roles: ROUTE_ACCESS['/service-channels'] },
  { path: 'work-orders', Page: page(() => import('../features/work-orders/pages/WorkOrdersPage'), 'WorkOrdersPage'), roles: ROUTE_ACCESS['/work-orders'] },
  { path: 'schedule', Page: page(() => import('../features/scheduling/pages/ScheduleBoardPage'), 'ScheduleBoardPage'), roles: ROUTE_ACCESS['/schedule'] },
  { path: 'my-schedule', Page: page(() => import('../features/scheduling/pages/MySchedulePage'), 'MySchedulePage'), roles: ROUTE_ACCESS['/my-schedule'] },
  { path: 'work-order-history', Page: page(() => import('../features/work-orders/pages/WorkOrderHistoryPage'), 'WorkOrderHistoryPage'), roles: ROUTE_ACCESS['/work-order-history'] },
  { path: 'technicians', Page: page(() => import('../features/technicians/pages/TechniciansPage'), 'TechniciansPage'), roles: ROUTE_ACCESS['/technicians'] },
  { path: 'inventory', Page: page(() => import('../features/inventory/pages/InventoryPage'), 'InventoryPage'), roles: ROUTE_ACCESS['/inventory'] },
  { path: 'part-requests', Page: page(() => import('../features/inventory/pages/WorkOrderPartRequestsPage'), 'WorkOrderPartRequestsPage'), roles: ROUTE_ACCESS['/part-requests'] },
  { path: 'payments', Page: page(() => import('../features/payments/pages/PaymentQueuePage'), 'PaymentQueuePage'), roles: ROUTE_ACCESS['/payments'] },
  { path: 'payment-settings', Page: page(() => import('../features/payments/pages/PaymentSettingsPage'), 'PaymentSettingsPage'), roles: ROUTE_ACCESS['/payment-settings'] },
  { path: 'inventory-stocktake', Page: page(() => import('../features/inventory/pages/InventoryStocktakePage'), 'InventoryStocktakePage'), roles: ROUTE_ACCESS['/inventory-stocktake'] },
  { path: 'inventory-movements', Page: page(() => import('../features/inventory/pages/InventoryMovementsPage'), 'InventoryMovementsPage'), roles: ROUTE_ACCESS['/inventory-movements'] },
  { path: 'audit', Page: page(() => import('../features/audit/pages/AuditPage'), 'AuditPage'), roles: ROUTE_ACCESS['/audit'] },
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
            {protectedRoutes.map(({ path, index, Page, roles }) => (
              <Route
                key={path ?? 'dashboard'}
                path={path}
                index={index}
                element={<RoleRoute allowedRoles={roles}><Page /></RoleRoute>}
              />
            ))}
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
