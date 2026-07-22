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
          colorPrimary: '#1d4ed8',
          colorInfo: '#1d4ed8',
          colorSuccess: '#059669',
          colorWarning: '#d97706',
          colorError: '#dc2626',
          colorBgLayout: '#eef3f9',
          borderRadius: 12,
          fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        },
        components: {
          Layout: { bodyBg: '#eef3f9', headerBg: 'rgba(255,255,255,0.74)', siderBg: '#0f172a' },
          Card: { borderRadiusLG: 20 },
          Button: { borderRadius: 12, controlHeight: 42 },
          Input: { controlHeight: 42 },
          Select: { controlHeight: 42 },
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
