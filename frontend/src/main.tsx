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
import './styles/global.css'

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
          colorPrimary: '#2563eb',
          colorInfo: '#2563eb',
          colorSuccess: '#059669',
          colorWarning: '#d97706',
          colorError: '#dc2626',
          borderRadius: 10,
          fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        },
        components: {
          Layout: { bodyBg: '#f4f7fb', headerBg: '#ffffff', siderBg: '#0f172a' },
          Card: { borderRadiusLG: 16 },
          Button: { borderRadius: 10, controlHeight: 40 },
          Input: { controlHeight: 40 },
          Select: { controlHeight: 40 },
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
