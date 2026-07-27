import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider, theme } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { AuthProvider } from './auth/AuthContext'
import { AppRouter } from './router/AppRouter'
import './styles/app/base.css'
import './styles/app/layout.css'
import './styles/app/components.css'
import './styles/app/dashboard.css'
import './styles/app/login.css'
import './styles/app/responsive.css'

dayjs.locale('vi')

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 20_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={viVN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#3b82f6',
          colorInfo: '#3b82f6',
          colorSuccess: '#10b981',
          colorWarning: '#f59e0b',
          colorError: '#ef4444',
          colorBgLayout: '#f8fafc',
          borderRadius: 12,
          fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        },
        components: {
          Layout: { bodyBg: '#f8fafc', headerBg: 'rgba(255,255,255,0.92)', siderBg: '#0f172a' },
          Card: { borderRadiusLG: 20, borderRadius: 16 },
          Button: {
            borderRadius: 12,
            controlHeight: 44,
            paddingInline: 20,
            fontWeight: 600,
            primaryColor: '#3b82f6',
            defaultBg: '#3b82f6',
            defaultBorderColor: '#3b82f6',
            colorPrimary: '#3b82f6',
            colorPrimaryHover: '#2563eb',
            colorPrimaryActive: '#1d4ed8',
          },
          Input: { controlHeight: 44, borderRadius: 10, paddingInline: 14, fontSize: 14 },
          Select: { controlHeight: 44, borderRadius: 10 },
          Modal: { borderRadiusLG: 20 },
          Form: { itemMarginBottom: 20, labelFontSize: 14, labelColor: '#475569' },
        },
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <AuthProvider>
              <AppRouter />
            </AuthProvider>
          </BrowserRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  </React.StrictMode>,
)
