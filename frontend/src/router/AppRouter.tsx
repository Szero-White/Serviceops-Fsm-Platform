import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { Spin } from 'antd'
import { ProtectedRoute } from '../auth/ProtectedRoute'
import { AppLayout } from '../layouts/AppLayout'

const LoginPage = lazy(() => import('../pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const DashboardPage = lazy(() => import('../pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const CustomersPage = lazy(() => import('../pages/CustomersPage').then((m) => ({ default: m.CustomersPage })))
const AssetsPage = lazy(() => import('../pages/AssetsPage').then((m) => ({ default: m.AssetsPage })))
const ServiceRequestsPage = lazy(() => import('../pages/ServiceRequestsPage').then((m) => ({ default: m.ServiceRequestsPage })))
const WorkOrdersPage = lazy(() => import('../pages/WorkOrdersPage').then((m) => ({ default: m.WorkOrdersPage })))
const TechniciansPage = lazy(() => import('../pages/TechniciansPage').then((m) => ({ default: m.TechniciansPage })))
const InventoryPage = lazy(() => import('../pages/InventoryPage').then((m) => ({ default: m.InventoryPage })))
const AuditPage = lazy(() => import('../pages/AuditPage').then((m) => ({ default: m.AuditPage })))

function RouteFallback() {
  return <div className="route-fallback"><Spin size="large" tip="Đang tải dữ liệu..." /></div>
}

export function AppRouter() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="assets" element={<AssetsPage />} />
            <Route path="service-requests" element={<ServiceRequestsPage />} />
            <Route path="work-orders" element={<WorkOrdersPage />} />
            <Route path="technicians" element={<TechniciansPage />} />
            <Route path="inventory" element={<InventoryPage />} />
            <Route path="audit" element={<AuditPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
